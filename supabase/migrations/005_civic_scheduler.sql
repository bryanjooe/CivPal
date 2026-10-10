-- Backend job runs every minute without a phone or screen visit.
begin;
create or replace function civic_private.scheduler_ready()
returns boolean language plpgsql set search_path='' as $$
declare ready boolean;
begin
  if to_regclass('cron.job') is null then return false; end if;
  execute $query$select exists(select 1 from cron.job where jobname='civic-escalate-overdue'
    and schedule='* * * * *' and command='select civic_private.escalate_overdue();'
    and username='postgres' and active)$query$ into ready;
  return ready;
end $$;
revoke all on function civic_private.scheduler_ready() from public,anon,authenticated;
do $$
declare existing_job bigint;
begin
  if not exists(select 1 from pg_available_extensions where name='pg_cron') then
    raise warning 'pg_cron unavailable. Enable Cron in Integrations/Database Extensions, then rerun this scheduler file. Scheduler remains incomplete.';
    return;
  end if;
  create extension if not exists pg_cron with schema pg_catalog;
  -- Named cron.schedule updates the same job rather than adding duplicates.
  select jobid into existing_job from cron.job where jobname='civic-escalate-overdue';
  if existing_job is not null and exists(select 1 from cron.job where jobid=existing_job and username<>'postgres') then
    raise exception 'Civic scheduler job belongs to another database role; inspect ownership';
  end if;
  perform cron.schedule('civic-escalate-overdue','* * * * *','select civic_private.escalate_overdue();');
  insert into civic_private.schema_migrations(version) values('005_civic_scheduler') on conflict do nothing;
end $$;
-- Initial evaluation is real server work and idempotent.
select civic_private.escalate_overdue() as complaints_escalated_now;
commit;
