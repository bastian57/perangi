-- FASE 3 — skema Supabase untuk PERANGI (server komunitas).
-- Dipakai di project: dieng-local (nebeng, tabel di-prefix perangi_).
-- Cara pakai: Supabase dashboard → pilih project dieng-local → SQL Editor
-- → New query → paste semua → Run. Aman dijalankan ulang.

-- 3a: laporan komunitas + voting
create table if not exists perangi_reports (
  id uuid primary key default gen_random_uuid(),
  domain text not null,
  category text not null,
  device_hash text not null,              -- SHA-256(ANDROID_ID + salt), BUKAN id mentah
  status text not null default 'pending', -- pending | approved | rejected
  created_at timestamptz not null default now(),
  unique (device_hash, domain)            -- 1 HP 1 suara per domain, dijamin database
);
create index if not exists perangi_reports_domain_idx on perangi_reports (domain);
create index if not exists perangi_reports_status_idx on perangi_reports (status);

-- 3b: heartbeat HP anak (monitoring keluarga)
create table if not exists perangi_heartbeats (
  device_hash text primary key,
  protection_on boolean not null,
  blocked_today int not null default 0,
  app_version text,
  updated_at timestamptz not null default now()
);

-- 3b: pairing ortu-anak via kode 6 digit
create table if not exists perangi_pairs (
  code text primary key,
  child_hash text not null,
  parent_hash text,
  parent_fcm_token text,                  -- diisi HP ortu saat pairing (3c)
  created_at timestamptz not null default now()
);

alter table perangi_reports enable row level security;
alter table perangi_heartbeats enable row level security;
alter table perangi_pairs enable row level security;

-- WAJIB: RLS policy saja tidak cukup — role anon butuh GRANT eksplisit.
grant insert on perangi_reports to anon;
grant insert, update on perangi_heartbeats to anon;
grant select on perangi_heartbeats to anon;
grant all on perangi_pairs to anon;

-- v1: anon boleh INSERT laporan & UPSERT heartbeat, dan kelola pairs.
-- Abuse dibatasi: unique(device_hash, domain), threshold 5 device untuk promote,
-- rate limit 5/hari di aplikasi.
drop policy if exists "anon ins perangi_reports" on perangi_reports;
create policy "anon ins perangi_reports"
  on perangi_reports for insert to anon
  with check (true);

drop policy if exists "anon ins perangi_heartbeats" on perangi_heartbeats;
create policy "anon ins perangi_heartbeats"
  on perangi_heartbeats for insert to anon
  with check (true);
drop policy if exists "anon upd perangi_heartbeats" on perangi_heartbeats;
create policy "anon upd perangi_heartbeats"
  on perangi_heartbeats for update to anon
  using (true);

drop policy if exists "anon perangi_pairs" on perangi_pairs;
create policy "anon perangi_pairs"
  on perangi_pairs for all to anon
  using (true) with check (true);

-- 3b: dashboard ortu perlu BACA heartbeat anak pasangannya
drop policy if exists "anon sel perangi_heartbeats" on perangi_heartbeats;
create policy "anon sel perangi_heartbeats"
  on perangi_heartbeats for select to anon
  using (true);
