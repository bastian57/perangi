# FASE 3 — Server Komunitas (Rancangan Teknis)

**Keputusan Putra (2026-10-06):** kurasi FULL-OTOMATIS (threshold 5 device),
notifikasi orang tua LANGSUNG PUSH. Backend: Supabase (akun sudah ada).
**Tanpa VPS** — robot jalan di GitHub Actions, push via Firebase (gratis).

## 1. Komponen & di mana jalan

| # | Komponen | Jalan di | Fungsi |
|---|----------|----------|--------|
| 1 | Supabase Postgres | cloud Supabase | tabel `perangi_reports`, `perangi_heartbeats`, `perangi_pairs` |
| 2 | Aplikasi PERANGI | HP user | upload laporan, heartbeat 15 mnt, pairing, terima FCM |
| 3 | Action `kurasi.yml` | GitHub (tiap 1 jam) | hitung vote ≥ 5 → tulis `blocklist.json` → commit |
| 4 | Action `monitor.yml` | GitHub (tiap 15 mnt) | cek heartbeat → kirim FCM ke ortu |
| 5 | Firebase FCM | cloud Google | push notification (gratis) |

Aplikasi tetap narik `blocklist.json` dari GitHub raw tiap 6 jam — **tidak berubah**.

## 2. Skema database

```sql
create table perangi_reports (
  id uuid primary key default gen_random_uuid(),
  domain text not null,
  category text not null,
  device_hash text not null,              -- SHA-256(ANDROID_ID + salt), bukan ID mentah
  status text not null default 'pending', -- pending | approved | rejected
  created_at timestamptz not null default now(),
  unique (device_hash, domain)             -- 1 HP 1 suara, dijamin database
);
create index perangi_reports_domain_idx on perangi_reports (domain);
create index perangi_reports_status_idx on perangi_reports (status);

create table perangi_heartbeats (
  device_hash text primary key,
  protection_on boolean not null,
  blocked_today int not null default 0,
  app_version text,
  updated_at timestamptz not null default now()
);

create table perangi_pairs (
  code text primary key,                  -- 6 digit, dibuat HP anak
  child_hash text not null,
  parent_hash text,
  parent_fcm_token text,                  -- diisi HP ortu saat pairing
  created_at timestamptz not null default now()
);

alter table reports enable row level security;
alter table heartbeats enable row level security;
alter table pairs enable row level security;
-- v1: anon boleh insert/upsert (friction abuse: 1 device = 1 suara, butuh 5 device fisik)
create policy "anon ins reports" on perangi_reports for insert to anon with check (true);
create policy "anon ins hb" on perangi_heartbeats for insert to anon with check (true);
create policy "anon upd hb" on perangi_heartbeats for update to anon using (true);
create policy "anon pairs" on perangi_pairs for all to anon using (true) with check (true);
```

## 3. Alur kurasi otomatis (laporan → blocklist global)

1. User tap "Kirim Laporan" → tersimpan di antrean lokal (seperti sekarang).
2. `ReportUploadWorker` (baru) → POST ke `https://<project>.supabase.co/rest/v1/reports`
   dengan header `apikey` + `Authorization: Bearer <ANON_KEY>`.
3. Tiap 1 jam, `kurasi.yml` jalan dengan `SUPABASE_SERVICE_KEY`:
   - `SELECT domain, count(distinct device_hash) FROM reports WHERE status='pending' GROUP BY domain`
   - domain dengan count ≥ 5 → `status='approved'`
   - baca `blocklist/blocklist.json` dari repo → tambah domain approved ke kategori sesuai
     `category` laporan → `version++` → commit + push.
4. Action yang sama bikin/update **GitHub Issue "Digest Kurasi"** berisi domain yang baru
   dipromote — Putra bisa review & revert manual kalau ada salah (human oversight tanpa kerja manual).
5. Aplikasi HP narik `blocklist.json` baru dalam ≤ 6 jam via mekanisme yang sudah ada.
6. Daftar `rejected`: kalau Putra revert via issue, domain masuk daftar tolak — robot tidak akan promote lagi.

## 4. Alur monitoring keluarga

1. **Pairing:** HP anak → layar "Keluarga" → "Tampilkan Kode" → POST `/pairs` `{code, child_hash}`.
   HP ortu → "Masukkan Kode Anak" → GET `/pairs?code=eq.XXXXXX` → PATCH `{parent_hash, parent_fcm_token}`.
2. **Heartbeat:** `HeartbeatWorker` (15 mnt) → upsert `/heartbeats` `{device_hash, protection_on, blocked_today, app_version}`.
3. **Monitor:** `monitor.yml` tiap 15 mnt (service key):
   - untuk tiap pair dengan `parent_fcm_token`: ambil heartbeat anak.
   - kondisi bahaya: `updated_at < now() - 30 mnt` (uninstall/mati) ATAU `protection_on = false`.
   - kirim FCM HTTP v1: `POST https://fcm.googleapis.com/v1/projects/<id>/messages:send`
     (OAuth2 dari `FIREBASE_SA_JSON` di GitHub Secrets).
   - isi: "⚠️ Proteksi di HP anak mati! Segera cek."
4. **Terima push:** `PerangiFirebaseService : FirebaseMessagingService` → tampilkan notifikasi.
   Token FCM direfresh otomatis → diupdate ke `perangi_pairs` saat pairing/dashboard dibuka.
5. **Dashboard ortu:** layar "Keluarga" di HP ortu → daftar anak + status (🟢 aman / 🔴 bahaya) via GET heartbeat.

## 5. Perubahan aplikasi (daftar file)

Baru:
- `data/Supabase.kt` — URL + ANON_KEY + helper HTTP (tanpa dependency baru, pakai HttpURLConnection)
- `data/DeviceId.kt` — `device_hash = SHA-256(ANDROID_ID + "perangi-v1")`
- `blocklist/ReportUploadWorker.kt` — upload antrean lokal → Supabase
- `family/HeartbeatWorker.kt` — upsert heartbeat tiap 15 mnt
- `family/FamilyScreen.kt` — pairing (anak & ortu) + dashboard status anak
- `family/PerangiFirebaseService.kt` — terima FCM → notifikasi
- `google-services.json` — dari Firebase console (Putra download, DANCUK bundel)

Ubah:
- `app/build.gradle` — tambah `com.google.firebase:firebase-messaging`, plugin google-services
- `ui/PerangiApp.kt` + `DashboardScreen.kt` — entry "Keluarga"
- `AndroidManifest.xml` — service FCM

Baru (repo):
- `.github/workflows/kurasi.yml`, `.github/workflows/monitor.yml`
- `tools/kurasi.py`, `tools/monitor.py`

## 6. Yang harus Putra siapkan (checklist)

- [ ] Supabase: New Project (gratis) → jalankan SQL bagian 2 → salin **Project URL** + **anon key**
- [ ] Supabase: salin **service_role key** → repo Settings → Secrets → `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`
- [ ] Firebase: New Project → tambah app Android `id.perangi` → download `google-services.json` → kirim ke DANCUK
- [ ] Firebase: Project Settings → Service Accounts → generate key JSON → repo Secrets → `FIREBASE_SA_JSON` (+ `FIREBASE_PROJECT_ID`)

## 7. Anti-abuse (jujur)

- 1 device fisik = 1 suara (butuh 5 HP/emulator berbeda untuk meloloskan 1 domain — friction cukup untuk v1).
- `unique(device_hash, domain)` di database — double vote mustahil.
- App-side rate limit 5/hari tetap jalan.
- Digest Issue = audit trail publik; Putra bisa revert kapan pun.
- v1 RLS permisif (anon insert) — diterima untuk v1 karena dampak abuse terbatas (tidak ada data sensitif; yang dipertaruhkan cuma 1 domain butuh 5 device).

## 8. Tahapan eksekusi (3 push)

- **3a — Kurasi:** SQL + Supabase.kt + DeviceId + ReportUploadWorker + kurasi.yml + digest issue. Hasil: laporan jalan, blocklist tumbuh otomatis.
- **3b — Heartbeat & pairing:** HeartbeatWorker + FamilyScreen (pairing + dashboard).
- **3c — Push:** Firebase (dep + service + google-services.json) + monitor.yml + Secrets.
