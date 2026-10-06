#!/usr/bin/env python3
"""Robot kurasi Fase 3a: promote laporan dengan >=5 suara device unik ke blocklist.json.

Dijalankan GitHub Actions tiap jam. Tanpa dependency (urllib saja).
Keluar graceful (exit 0) kalau secrets belum diisi / tidak ada yang lolos.
"""
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone

THRESHOLD = 5
CAT_MAP = {
    "Judol": "judol",
    "Pinjol Ilegal": "pinjol_ilegal",
    "Iklan Judi": "iklan_judi",
    "Lainnya": "lainnya",
}
DIGEST_TITLE = "\U0001F6E1\uFE0F Digest Kurasi Blocklist"


def env(k):
    return os.environ.get(k, "").strip()


def sbase(method, path, body=None):
    base = env("SUPABASE_URL").rstrip("/")
    key = env("SUPABASE_SERVICE_KEY")
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(
        base + path, method=method, data=data,
        headers={
            "apikey": key,
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
            "Prefer": "return=minimal",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        return e.code, None


def gh(method, path, body=None):
    repo = env("GITHUB_REPOSITORY")
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(
        f"https://api.github.com/repos/{repo}{path}", method=method, data=data,
        headers={
            "Authorization": f"Bearer {env('GITHUB_TOKEN')}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        return e.code, None


def main():
    if not env("SUPABASE_URL") or not env("SUPABASE_SERVICE_KEY"):
        print("SKIP: SUPABASE_URL / SUPABASE_SERVICE_KEY belum diisi di Secrets.")
        return 0

    st, rows = sbase(
        "GET",
        "/rest/v1/reports?select=domain,category,device_hash&status=eq.pending",
    )
    if st != 200 or rows is None:
        print(f"GAGAL baca reports: HTTP {st}")
        return 0

    groups = {}
    for r in rows:
        g = groups.setdefault(r["domain"], {"category": r["category"], "voters": set()})
        g["voters"].add(r["device_hash"])
    qualified = {d: g for d, g in groups.items() if len(g["voters"]) >= THRESHOLD}
    if not qualified:
        print("Tidak ada domain yang lolos threshold.")
        return 0

    bp = "blocklist/blocklist.json"
    data = json.load(open(bp))
    existing = {d for cats in data["categories"].values() for d in cats}
    promoted = []
    for domain in sorted(qualified):
        g = qualified[domain]
        if domain in existing:
            continue
        key = CAT_MAP.get(g["category"], "lainnya")
        data["categories"].setdefault(key, [])
        if domain not in data["categories"][key]:
            data["categories"][key].append(domain)
        promoted.append((domain, g["category"], len(g["voters"])))
        q = urllib.parse.quote(domain, safe="")
        sbase("PATCH", f"/rest/v1/reports?domain=eq.{q}&status=eq.pending",
              {"status": "approved"})

    if not promoted:
        print("Semua yang lolos sudah ada di blocklist.")
        return 0

    data["version"] = data.get("version", 0) + 1
    data["updated"] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    json.dump(data, open(bp, "w"), indent=2, ensure_ascii=False)

    stamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    lines = "\n".join(f"- `{d}` ({c}, {n} suara unik)" for d, c, n in promoted)
    entry = f"\n\n## {stamp}\n{lines}"
    st, issues = gh("GET", "/issues?state=open&per_page=100")
    found = None
    if st == 200 and issues:
        for i in issues:
            if i.get("title") == DIGEST_TITLE:
                found = i
                break
    if found:
        gh("PATCH", f"/issues/{found['number']}",
           {"body": (found.get("body") or "") + entry})
        print(f"Digest issue #{found['number']} diupdate: {len(promoted)} domain.")
    else:
        gh("POST", "/issues",
           {"title": DIGEST_TITLE,
            "body": f"Domain yang dipromote otomatis (>= {THRESHOLD} suara device unik). "
                    f"Revert manual kalau ada yang salah:{entry}"})
        print(f"Digest issue dibuat: {len(promoted)} domain.")
    print("PROMOTED:", ", ".join(d for d, _, _ in promoted))
    return 0


sys.exit(main())
