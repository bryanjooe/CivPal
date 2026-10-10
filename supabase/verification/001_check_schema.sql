-- Run in SQL Editor after 001_civic_schema.sql.
-- Reads only. This checks installation, not final RLS authorization behavior.
with expected(name) as (values
  ('departments'),('jurisdictions'),('contractors'),('profiles'),('profile_scopes'),
  ('projects'),('project_assignments'),('project_milestones'),('progress_reports'),
  ('progress_report_reviews'),('delay_records'),('complaints'),('complaint_responses'),
  ('complaint_events'),('project_conflicts'),('officer_handovers'),('handover_projects'),
  ('handover_pending_tasks'),('project_follows'),('deadline_revision_requests'),('audit_logs')
)
select e.name as table_name, c.oid is not null as table_exists,
  coalesce(c.relrowsecurity,false) as rls_enabled,
  case when c.oid is null then false else
    not has_table_privilege('anon',c.oid,'SELECT,INSERT,UPDATE,DELETE')
    and not has_table_privilege('authenticated',c.oid,'SELECT,INSERT,UPDATE,DELETE')
  end as api_access_closed
from expected e left join pg_namespace n on n.nspname='public'
left join pg_class c on c.relnamespace=n.oid and c.relname=e.name and c.relkind='r'
order by e.name;

select version, applied_at from civic_private.schema_migrations;

select conname, pg_get_constraintdef(oid) as definition
from pg_constraint where conrelid in
  ('public.project_milestones'::regclass,'public.progress_reports'::regclass,
   'public.handover_pending_tasks'::regclass,'public.profiles'::regclass)
  and contype in ('p','f') order by conrelid::regclass::text,conname;

select tgname, tgenabled from pg_trigger
where tgname in ('civic_project_id_immutable','civic_complaint_deadline','civic_auth_profile_created','civic_auth_profile_email_changed');
