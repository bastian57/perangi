# PERANGI — Perang Melawan Judol & Pinjol Ilegal

Aplikasi Android gratis pelindung keluarga: **DNS filter on-device** (via `VpnService`,
tanpa root) yang memblokir situs judi online, pinjol ilegal, dan iklan judi —
dilengkapi Kalkulator Rugi Judol, Cek Pinjol (direktori OJK), Mode Orang Tua,
Tombol Darurat, dan Scan Aplikasi.

**⬇️ Download APK terbaru:** https://github.com/bastian57/perangi/releases/latest
**🌐 Landing page:** https://bastian57.github.io/perangi/ *(aktifkan GitHub Pages: Settings → Pages → Deploy from branch → `main` → `/docs`)*

## Fitur

- **Filter DNS lokal** — 19+ domain + 15 pola kata (`togel`, `gacor`, `maxwin`, …) diblokir via VPN lokal; whitelist untuk false positive
- **Tes Kesehatan Proteksi** (5 cek: VPN, blocklist, koneksi DNS, update otomatis, DNS Pribadi)
- **Mode Orang Tua** — PIN untuk mematikan proteksi/ubah whitelist; alarm kalau dimatikan paksa
- **Tombol Darurat (SOS)** — interupsi psikologis saat "pengen main", pakai angka rugi pribadi
- **Kalkulator Hitung Rugi Judol** & **Kalkulator Hitung Bunga Pinjol** (format titik ribuan)
- **Cek Pinjol** — direktori OJK September 2026 (94 legal + status izin dicabut)
- **Scan Aplikasi** — deteksi APK judi/pinjol ilegal terinstal (tidak lewat DNS)
- **Laporkan Situs** — antrean laporan komunitas (1 HP 1 suara, 5/hari)
- **Streak** "hari terlindungi", Quick Settings tile, auto-start setelah reboot
- **Cek Pembaruan** di aplikasi via GitHub Releases (rolling tag `latest`)
- Update blocklist otomatis tiap 6 jam (WorkManager) dari `blocklist/blocklist.json`

## Arsitektur (lihat FASE0_RESEARCH.md untuk fondasi riset)

```
app/src/main/java/id/perangi/
├── vpn/
│   ├── PerangiVpnService.kt   # VpnService, DNS-only routing (ala DNS66)
│   ├── PerangiVpnThread.kt    # event loop TUN → filter → forward
│   ├── DnsPacket.kt           # parser & pembangun paket DNS (clean-room)
│   ├── Blocklist.kt           # HashSet + atomic swap + suffix matching
│   ├── KeywordBlock.kt        # blokir pola kata (substring hostname)
│   └── BootReceiver.kt        # auto-start setelah reboot
├── blocklist/
│   ├── BlocklistUpdater.kt    # tarik blocklist.json + keywords dari GitHub raw
│   └── BlocklistUpdateWorker.kt # jadwal WorkManager 6 jam
├── data/                      # StatsRepository, ParentMode (PIN), ReportStore,
│                              # StreakStore, WhitelistStore, OjkData, UpdateChecker
├── qs/PerangiTileService.kt   # Quick Settings tile
└── ui/                        # Compose: onboarding, dashboard, kalkulator,
                               # cek pinjol, SOS, kesehatan, ortu, lapor, whitelist,
                               # scan aplikasi, tentang & pembaruan
```

## Build

Setiap push ke `main`/`master` → GitHub Actions build debug APK →
otomatis publish ke **rolling release `latest`** (`perangi.apk`).

```bash
./gradlew :app:assembleDebug
```

## Format blocklist (`blocklist/blocklist.json`)

```json
{
  "version": 3,
  "updated": "2026-10-06T17:15:00+07:00",
  "categories": { "doh_bypass": [...], "judol": [...], "pinjol_ilegal": [], "iklan_judi": [] },
  "keywords": ["togel", "gacor", "maxwin", ...]
}
```

Naikkan `version` setiap mengubah daftar — aplikasi menarik otomatis tiap 6 jam.

## Roadmap

- **Fase 3 (server komunitas):** laporan → voting → blocklist global; monitoring keluarga jarak jauh
- **Distribusi:** ✅ rolling release + cek pembaruan di aplikasi + landing page

## Batasan jujur

Di HP yang tidak di-manage, user yang nekat selalu bisa bypass (uninstall/mematikan VPN) —
PERANGI membangun friksi berlapis + alarm + kesadaran, bukan DRM (itu wilayah malware).
Untuk anak kecil, kombinasikan dengan Google Family Link.

Butuh bantuan pinjol? Hubungi **OJK di 157** (Senin–Jumat).
