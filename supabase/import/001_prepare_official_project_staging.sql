-- OPTIONAL official-data import; NOT included in the demo setup bundle.
-- Run as postgres, then import a reviewed CSV with source_id,record_data columns.
-- record_data is a JSON object containing the existing project fields in SQL snake_case.
begin;
create table if not exists civic_private.official_project_staging(
  source_id text primary key,
  record_data jsonb not null check(jsonb_typeof(record_data)='object'),
  status text not null default 'PENDING' check(status in ('PENDING','IMPORTED','REJECTED')),
  error_detail text,
  imported_at timestamptz
);
revoke all on civic_private.official_project_staging from public,anon,authenticated;
commit;
