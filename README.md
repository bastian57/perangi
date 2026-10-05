# PERANGI — Perang Melawan Judol & Pinjol Ilegal

Aplikasi Android pelindung keluarga: DNS filter on-device (via `VpnService`,
tanpa root) yang memblokir situs judi online, pinjol ilegal, dan iklan judi —
dilengkapi Kalkulator Rugi Judol, Cek Pinjol (data OJK), dan Mode Orang Tua.

## Status
**Fase 0 — kerangka/skeleton.** VPN filter inti + update blocklist sudah berbentuk
kode dan siap di-build. Fitur UI lengkap (Compose), backend laporan komunitas
(Opsi B), dan alarm wali menyusul di fase berikutnya.

## Arsitektur (lihat FASE0_RESEARCH.md untuk fondasi riset)
```
app/src/main/java/id/perangi/
├── vpn/
│   ├── PerangiVpnService.kt   # VpnService, DNS-only routing (ala DNS66)
│   ├── PerangiVpnThread.kt    # event loop TUN → filter → forward
│   ├── DnsPacket.kt           # parser & pembangun paket DNS (clean-room)
│   ├── Blocklist.kt           # HashSet + atomic swap + suffix matching
│   └── BootReceiver.kt        # auto-start setelah reboot
├── blocklist/
│   ├── BlocklistUpdater.kt    # tarik blocklist.json tiap aplikasi dibuka
│   └── ReportManager.kt       # antrean laporan komunitas (stub Fase 3)
├── ui/
│   └── MainActivity.kt        # layar utama skeleton (Fase 1: Compose)
└── data/
    └── StatsRepository.kt     # statistik blokir
blocklist/blocklist.json       # daftar blokir remote (diupdate berkala)
```

## Build otomatis (GitHub Actions)
Setiap push ke `main` → APK debug otomatis jadi → download di
**Actions → workflow run → Artifacts → perangi-debug-apk**.
Install di HP (izinkan "install dari sumber tak dikenal" sekali saja).

## Kebenaran lisensi
Implementasi VPN ditulis clean-room (dari nol) terinspirasi arsitektur DNS66
(GPL-3.0) — **tidak ada kode GPL yang disalin**. Library yang dipakai langsung
hanya yang berlisensi permisif. Lisensi final PERANGI ditentukan sebelum rilis
publik (kandidat: GPL-3.0).

## Distribusi
APK dari GitHub Releases / web resmi (utama). Play Store tidak mengizinkan
DNS-blocker on-device — hanya sebagai kanal bonus dengan fitur terbatas.
