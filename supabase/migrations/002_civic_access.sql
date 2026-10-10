-- Scoped SELECT access. Business writes are available only through authorized RPCs.
begin;
do $$ begin
  if not exists(select 1 from civic_private.schema_migrations where version='001_civic_schema') then
    raise exception 'Install 001_civic_schema.sql first';
  end if;
end $$;

create or replace function civic_private.active_role()
returns public.civic_role language sql stable security definer set search_path='' as $$
  select p.role from public.profiles p join auth.users u on u.id=p.id
  where p.id=auth.uid() and p.is_active
    and (p.role='CITIZEN' or (not coalesce(u.is_anonymous,false) and u.email_confirmed_at is not null));
$$;
create or replace function civic_private.is_admin()
returns boolean language sql stable security definer set search_path='' as $$
  select coalesce(civic_private.active_role()='ADMINISTRATOR',false);
$$;
create or replace function civic_private.has_scope(dept text, jurisdiction text)
returns boolean language sql stable security definer set search_path='' as $$
  with recursive ancestors(id,parent_id) as (
    select j.id,j.parent_id from public.jurisdictions j where j.id=jurisdiction and j.is_active
    union
    select j.id,j.parent_id from public.jurisdictions j join ancestors a on a.parent_id=j.id where j.is_active
  ) select civic_private.is_admin() or (
    civic_private.active_role() in ('OFFICER','COORDINATOR') and exists(
      select 1 from public.profile_scopes s join ancestors a on s.jurisdiction_id=a.id
      join public.departments d on d.id=s.department_id and d.is_active
      where s.profile_id=auth.uid() and s.department_id=dept));
$$;
create or replace function civic_private.can_manage_project(project text)
returns boolean language sql stable security definer set search_path='' as $$
  select exists(select 1 from public.projects p where p.id=project
    and civic_private.active_role() in ('OFFICER','ADMINISTRATOR')
    and civic_private.has_scope(p.department_id,p.responsible_office_id));
$$;
create or replace function civic_private.contractor_project(project text)
returns boolean language sql stable security definer set search_path='' as $$
  select exists(select 1 from public.projects p join public.profiles u on u.contractor_id=p.contractor_id
    join public.contractors c on c.id=p.contractor_id and c.is_active
    where p.id=project and u.id=auth.uid() and civic_private.active_role()='CONTRACTOR');
$$;
create or replace function civic_private.internal_project(project text)
returns boolean language sql stable security definer set search_path='' as $$
  select exists(select 1 from public.projects p where p.id=project
    and (civic_private.has_scope(p.department_id,p.responsible_office_id)
      or civic_private.contractor_project(p.id)));
$$;
create or replace function civic_private.public_project(project text)
returns boolean language sql stable security definer set search_path='' as $$
  select exists(select 1 from public.projects where id=project and is_published);
$$;
create or replace function civic_private.can_read_complaint(complaint text)
returns boolean language sql stable security definer set search_path='' as $$
  select exists(select 1 from public.complaints c where c.id=complaint and
    ((c.citizen_id=auth.uid() and civic_private.active_role()='CITIZEN')
      or civic_private.has_scope(c.assigned_department_id,c.jurisdiction_id)));
$$;
create or replace function civic_private.can_manage_complaint(complaint text)
returns boolean language sql stable security definer set search_path='' as $$
  select exists(select 1 from public.complaints c where c.id=complaint
    and civic_private.active_role() in ('OFFICER','ADMINISTRATOR')
    and civic_private.has_scope(c.assigned_department_id,c.jurisdiction_id));
$$;
create or replace function civic_private.can_read_handover(handover text)
returns boolean language sql stable security definer set search_path='' as $$
  select exists(select 1 from public.officer_handovers h where h.id=handover
    and (civic_private.is_admin() or
      (civic_private.active_role()='OFFICER'
        and auth.uid() in (h.outgoing_officer_id,h.incoming_officer_id)
        and civic_private.has_scope(h.department_id,h.office_jurisdiction_id))));
$$;
create or replace function civic_private.can_read_review(report text)
returns boolean language sql stable security definer set search_path='' as $$
  select civic_private.is_admin() or exists(select 1 from public.progress_reports r where r.id=report
    and civic_private.can_manage_project(r.project_id));
$$;

-- Rebuild only CivicTech-owned policies and reset only CivicTech table grants.
do $$ declare t text; cols text; pred text; public_table boolean;
begin
  for t,pred,public_table in select * from (values
    ('departments','is_active or civic_private.is_admin()',true),
    ('jurisdictions','is_active or civic_private.is_admin()',true),
    ('contractors','is_active or civic_private.is_admin()',true),
    ('profiles','id=(select auth.uid()) or civic_private.is_admin()',false),
    ('profile_scopes','(profile_id=(select auth.uid()) and civic_private.active_role() is not null) or civic_private.is_admin()',false),
    ('projects','is_published or civic_private.internal_project(id)',true),
    ('project_assignments','civic_private.can_manage_project(project_id) or (civic_private.active_role()=''COORDINATOR'' and civic_private.internal_project(project_id))',false),
    ('project_milestones','civic_private.public_project(project_id) or civic_private.internal_project(project_id)',true),
    ('progress_reports','(is_public and status=''Approved'' and civic_private.public_project(project_id)) or civic_private.internal_project(project_id)',true),
    ('progress_report_reviews','civic_private.can_read_review(report_id)',false),
    ('delay_records','(is_published and civic_private.public_project(project_id)) or civic_private.internal_project(project_id)',true),
    ('complaints','civic_private.can_read_complaint(id)',false),
    ('complaint_responses','civic_private.can_read_complaint(complaint_id)',false),
    ('complaint_events','civic_private.can_read_complaint(complaint_id)',false),
    ('project_conflicts','civic_private.is_admin() or (civic_private.active_role()=''COORDINATOR'' and civic_private.internal_project(project_a_id) and civic_private.internal_project(project_b_id))',false),
    ('officer_handovers','civic_private.can_read_handover(id)',false),
    ('handover_projects','civic_private.can_read_handover(handover_id)',false),
    ('handover_pending_tasks','civic_private.can_read_handover(handover_id)',false),
    ('project_follows','user_id=(select auth.uid()) and civic_private.active_role()=''CITIZEN''',false),
    ('deadline_revision_requests','civic_private.can_manage_project(project_id) or (submitted_by=(select auth.uid()) and civic_private.contractor_project(project_id))',false),
    ('audit_logs','civic_private.is_admin()',false)
  ) policies(table_name,expression,is_public) loop
    execute format('revoke all on public.%I from public,anon,authenticated',t);
    -- Remove any prior column grants too. RLS does not hide individual columns.
    select string_agg(quote_ident(column_name),', ' order by ordinal_position) into cols
      from information_schema.columns where table_schema='public' and table_name=t;
    execute format('revoke select (%s),insert (%s),update (%s),references (%s) on public.%I from public,anon,authenticated',cols,cols,cols,cols,t);
    select string_agg(quote_ident(column_name),', ' order by ordinal_position) into cols
      from information_schema.columns where table_schema='public' and table_name=t
      and column_name <> 'source_snapshot'
      and (not public_table or column_name not in ('submitted_by','recorded_by','resolved_by'));
    execute format('grant select (%s) on public.%I to authenticated',cols,t);
    if public_table then execute format('grant select (%s) on public.%I to anon',cols,t); end if;
    execute format('drop policy if exists civic_read on public.%I',t);
    execute format('create policy civic_read on public.%I for select to %s using (%s)',t,
      case when public_table then 'anon,authenticated' else 'authenticated' end,pred);
    execute format('alter table public.%I enable row level security',t);
    -- Convenient explicit safe projection: SELECT * works on these invoker views.
    execute format('create or replace view public.civic_%I with (security_invoker=true) as select %s from public.%I',t,cols,t);
    execute format('revoke all on public.civic_%I from public,anon,authenticated',t);
    execute format('grant select on public.civic_%I to authenticated',t);
    if public_table then execute format('grant select on public.civic_%I to anon',t); end if;
  end loop;
end $$;
-- Helpers are not in an exposed API schema; no private tables are granted.
grant usage on schema civic_private to anon,authenticated;
revoke all on all functions in schema civic_private from public,anon,authenticated;
grant execute on function civic_private.active_role(),civic_private.is_admin(),
  civic_private.has_scope(text,text),civic_private.can_manage_project(text),
  civic_private.contractor_project(text),civic_private.internal_project(text),
  civic_private.public_project(text),civic_private.can_read_complaint(text),
  civic_private.can_manage_complaint(text),civic_private.can_read_handover(text),civic_private.can_read_review(text)
  to anon,authenticated;
insert into civic_private.schema_migrations(version) values('002_civic_access') on conflict do nothing;
commit;
