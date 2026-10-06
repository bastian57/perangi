-- FASE 3a — skema Supabase untuk kurasi laporan komunitas.
-- Cara pakai: Supabase dashboard → SQL Editor → New query → paste semua → Run.
-- Aman dijalankan ulang (IF NOT EXISTS).

create table if not exists reports (
  id uuid primary key default gen_random_uuid(),
  domain text not null,
  category text not null,
  device_hash text not null,              -- SHA-256(ANDROID_ID + salt), BUKAN id mentah
  status text not null default 'pending', -- pending | approved | rejected
  created_at timestamptz not null default now(),
  unique (device_hash, domain)            -- 1 HP 1 suara per domain, dijamin database
);
create index if not exists reports_domain_idx on reports (domain);
create index if not exists reports_status_idx on reports (status);

alter table reports enable row level security;

-- v1: anon boleh INSERT laporan. Abuse dibatasi oleh:
--   * unique(device_hash, domain) → 1 device fisik cuma 1 suara
--   * threshold 5 device berbeda untuk promote
--   * rate limit 5/hari di aplikasi
drop policy if exists "anon ins reports" on reports;
create policy "anon ins reports"
  on reports for insert to anon
  with check (true);
