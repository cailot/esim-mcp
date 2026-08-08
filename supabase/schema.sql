-- Supabase schema for esim-mcp daily reports
-- Run this in the Supabase SQL editor once.

create table if not exists daily_esim_reports (
    id bigserial primary key,
    report_date date not null,
    generated_at timestamptz not null default now(),
    best_plan jsonb,
    matching_plans jsonb not null default '[]'::jsonb,
    candidates jsonb not null default '[]'::jsonb,
    evaluation_notes text,
    dry_run boolean not null default false,
    created_at timestamptz not null default now()
);

create unique index if not exists daily_esim_reports_report_date_uidx
    on daily_esim_reports (report_date);

comment on table daily_esim_reports is 'Daily eSIM plan discovery reports until Korea arrival (2026-09-15)';
