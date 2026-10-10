-- CIVICTECH: ALL REMAINING SQL TOGETHER
-- Run the entire file as postgres in SQL Editor AFTER 001_civic_schema.sql.
-- Includes clearly labeled Android DEMO data and the five confirmed test emails.
-- No passwords, API keys, Android edits or public self-promotion are included.
-- This includes the admin bootstrap whether or not you already ran it.
-- Everything below is one transaction: any error rolls the entire bundle back.
begin;
do $$ begin
 if current_user<>'postgres' then raise exception 'Use SQL Editor execution role postgres'; end if;
 if not exists(select 1 from civic_private.schema_migrations where version='001_civic_schema') then
   raise exception 'Initial schema 001 must already be installed'; end if;
 perform pg_advisory_xact_lock(hashtextextended('civic:remaining-setup',0));
end $$;

-- ===== admin/001_bootstrap_admin.sql =====
-- One-time trusted bootstrap, not an application RPC or an Auth metadata edit.
-- Run the ENTIRE file in Supabase SQL Editor with execution role postgres.
-- admin@test.com is the intended test Administrator shown in the user's dashboard.
-- For another installation, change this email only to a verified account you control.
do $$
declare
  target_email constant text := 'admin@test.com';
  target_id uuid;
  target_profile public.profiles%rowtype;
begin
  if current_user <> 'postgres' then
    raise exception 'Run bootstrap only in SQL Editor as postgres';
  end if;
  perform pg_advisory_xact_lock(hashtextextended('civic:first-administrator', 0));
  if (select count(*) from auth.users where email = target_email) <> 1 then
    raise exception 'Expected exactly one Auth account for %', target_email;
  end if;
  select id into target_id from auth.users
  where email = target_email and email_confirmed_at is not null
    and not coalesce(is_anonymous, false)
  for update;
  if target_id is null then
    raise exception 'Administrator must be a confirmed, non-anonymous Auth account';
  end if;
  select * into target_profile from public.profiles where id = target_id for update;
  if not found or not target_profile.is_active then
    raise exception 'An active profile is required; verify profile creation first';
  end if;
  if target_profile.email is distinct from target_email then
    raise exception 'Profile email does not match Auth email';
  end if;
  -- Repeating a successful bootstrap performs no further writes.
  if target_profile.role = 'ADMINISTRATOR' then
    return;
  end if;
  if exists (select 1 from public.profiles where role = 'ADMINISTRATOR') then
    raise exception 'An Administrator already exists; use the authorized administrator workflow for additional accounts';
  end if;
  if target_profile.role <> 'CITIZEN' then
    raise exception 'Bootstrap expects the initial CITIZEN profile';
  end if;
  update public.profiles set role = 'ADMINISTRATOR', updated_at = now()
  where id = target_id;
  insert into public.audit_logs(event_at, action, performed_by_name,
    target_type, target_id, details)
  values (now(), 'FIRST_ADMINISTRATOR_BOOTSTRAP', 'Supabase SQL Editor (postgres)',
    'PROFILE', target_id::text,
    'Trusted dashboard owner assigned the first Administrator role to a confirmed test identity.');
end;
$$;

-- Expected: admin@test.com / ADMINISTRATOR / true.
select email, role, is_active from public.profiles where email = 'admin@test.com';

-- ===== migrations/002_civic_access.sql =====
-- Scoped SELECT access. Business writes are available only through authorized RPCs.
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

-- ===== migrations/003_civic_workflows.sql =====
-- All client writes go through these authorized, atomic functions.
create or replace function civic_private.require_actor(allowed public.civic_role[])
returns public.profiles language plpgsql security definer set search_path='' as $$
declare actor public.profiles;
begin
  select * into actor from public.profiles where id=auth.uid();
  if actor.id is null or not coalesce(civic_private.active_role()=any(allowed),false) then
    raise exception 'Active authorized account required' using errcode='42501';
  end if;
  return actor;
end $$;
create or replace function civic_private.log_action(action text,kind text,target text,details text)
returns void language sql security definer set search_path='' as $$
  insert into public.audit_logs(event_at,action,performed_by,performed_by_name,target_type,target_id,details)
  select now(),action,p.id,p.name,kind,target,details from public.profiles p where p.id=auth.uid();
$$;
create or replace function civic_private.require_text(value text,label text,max_length integer default 10000)
returns text language plpgsql immutable set search_path='' as $$
begin
  if value is null or length(btrim(value)) not between 1 and max_length then
    raise exception '% must contain 1 to % characters',label,max_length using errcode='22023';
  end if;
  return btrim(value);
end $$;
create or replace function civic_private.next_project_id(dept_code text)
returns text language plpgsql security definer set search_path='' as $$
declare y integer := extract(year from current_date); n bigint; candidate text; initial bigint;
begin
  -- Imported IDs are considered before the first counter is initialized.
  select coalesce(max(substring(id from '[0-9]+$')::bigint),0) into initial from public.projects
    where id ~ ('^TN-'||dept_code||'-'||y||'-[0-9]+$');
  loop
    insert into civic_private.id_counters(prefix,department_code,id_year,last_issued)
      values('TN',dept_code,y,initial+1)
    on conflict(prefix,department_code,id_year) do update
      set last_issued=greatest(civic_private.id_counters.last_issued,initial)+1
    returning last_issued into n;
    candidate := 'TN-'||dept_code||'-'||y||'-'||lpad(n::text,greatest(2,length(n::text)),'0');
    if not exists(select 1 from public.projects where id=candidate) then return candidate; end if;
  end loop;
end $$;

create or replace function public.civic_update_my_profile(p_name text,p_language text,p_notifications boolean)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles;
begin
  a := civic_private.require_actor(array['CITIZEN','OFFICER','COORDINATOR','CONTRACTOR','ADMINISTRATOR']::public.civic_role[]);
  update public.profiles set name=civic_private.require_text(p_name,'Name',200),
    preferred_language=p_language,notifications_enabled=p_notifications,updated_at=now() where id=a.id;
end $$;
create or replace function public.civic_set_follow(p_project text,p_follow boolean)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles;
begin
  a := civic_private.require_actor(array['CITIZEN']::public.civic_role[]);
  if not civic_private.public_project(p_project) then raise exception 'Published project required' using errcode='42501'; end if;
  if p_follow then
    insert into public.project_follows(user_id,project_id) values(a.id,p_project) on conflict do nothing;
  else delete from public.project_follows where user_id=a.id and project_id=p_project; end if;
end $$;
create or replace function public.civic_register_project(p_data jsonb,p_milestones jsonb default '[]')
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; d public.departments; c public.contractors; m public.jurisdictions;
  o public.jurisdictions; result text; item jsonb;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into d from public.departments where id=p_data->>'department_id' and is_active;
  select * into c from public.contractors where id=p_data->>'contractor_id' and is_active;
  select * into m from public.jurisdictions where id=p_data->>'municipal_body_id' and kind='MUNICIPALITY' and is_active;
  select * into o from public.jurisdictions where id=p_data->>'responsible_office_id' and kind='OFFICE' and is_active;
  if d.id is null or c.id is null or m.id is null or o.id is null then raise exception 'Valid active department, contractor, municipality and office required'; end if;
  if not coalesce(civic_private.has_scope(d.id,o.id),false) then raise exception 'Project outside authorized scope' using errcode='42501'; end if;
  if jsonb_typeof(p_milestones)<>'array' or jsonb_array_length(p_milestones)>100 then raise exception 'Milestones must be an array of at most 100 items'; end if;
  result := civic_private.next_project_id(d.code);
  insert into public.projects(id,title,description,department_id,department_name,district,municipal_body_id,
    municipal_body,location,latitude,longitude,contractor_id,contractor_name,responsible_office_id,
    responsible_office,current_officer_in_charge,estimated_budget_in_lakhs,funding_source,
    start_date,original_completion_date,revised_completion_date)
  values(result,civic_private.require_text(p_data->>'title','Title',500),
    civic_private.require_text(p_data->>'description','Description'),d.id,d.name,
    civic_private.require_text(p_data->>'district','District',200),m.id,m.name,
    civic_private.require_text(p_data->>'location','Location',1000),
    (p_data->>'latitude')::double precision,(p_data->>'longitude')::double precision,c.id,c.name,o.id,o.name,a.name,
    (p_data->>'estimated_budget_in_lakhs')::numeric,civic_private.require_text(p_data->>'funding_source','Funding source',500),
    (p_data->>'start_date')::date,(p_data->>'original_completion_date')::date,(p_data->>'original_completion_date')::date);
  if a.role='OFFICER' then insert into public.project_assignments(project_id,officer_id) values(result,a.id); end if;
  for item in select value from jsonb_array_elements(p_milestones) loop
    insert into public.project_milestones(project_id,milestone_id,title,target_date)
    values(result,civic_private.require_text(item->>'milestone_id','Milestone ID',100),
      civic_private.require_text(item->>'title','Milestone title',500),(item->>'target_date')::date);
  end loop;
  perform civic_private.log_action('PROJECT_REGISTERED','PROJECT',result,'Project registered with immutable database-generated ID; publication pending.');
  return result;
end $$;
create or replace function public.civic_publish_project(p_project text,p_publish boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  perform 1 from public.projects where id=p_project for update;
  if not civic_private.can_manage_project(p_project) then raise exception 'Project outside authorized scope' using errcode='42501'; end if;
  update public.projects set is_published=p_publish,updated_at=now() where id=p_project;
  perform civic_private.log_action('PROJECT_PUBLICATION_CHANGED','PROJECT',p_project,'Published: '||p_publish);
end $$;
create or replace function public.civic_submit_complaint(p_category public.civic_complaint_category,
  p_location text,p_description text,p_project text default null,p_jurisdiction text default null)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; p public.projects; j public.jurisdictions; d public.departments; result text;
begin
  a := civic_private.require_actor(array['CITIZEN']::public.civic_role[]);
  if p_project is not null then
    select * into p from public.projects where id=p_project and is_published;
    if p.id is null then raise exception 'Published project required' using errcode='42501'; end if;
    select * into d from public.departments where id=p.department_id and is_active;
    select * into j from public.jurisdictions where id=p.responsible_office_id and is_active;
  else
    select * into j from public.jurisdictions where id=p_jurisdiction and kind='MUNICIPALITY' and is_active;
    select * into d from public.departments where is_active and code=case p_category
      when 'POTHOLE' then 'HWY' when 'WATER_LEAK' then 'TWAD' when 'DRAINAGE_OVERFLOW' then 'PWD' else 'MAWS' end;
  end if;
  if j.id is null or d.id is null then raise exception 'Active routing department and jurisdiction required'; end if;
  insert into public.complaints(category,project_id,project_title_snapshot,citizen_id,submitted_by_citizen_name,
    location,district,description,assigned_department_id,assigned_department_name,jurisdiction_id)
  values(p_category,p.id,p.title,a.id,a.name,civic_private.require_text(p_location,'Location',1000),
    coalesce(j.district,p.district,j.name),civic_private.require_text(p_description,'Description'),d.id,d.name,j.id)
  returning id into result;
  insert into public.complaint_events(complaint_id,event_type,description,performed_by,performed_by_name)
  values(result,'Submitted','Complaint registered. Official response deadline: 168 hours.',a.id,a.name);
  perform civic_private.log_action('COMPLAINT_SUBMITTED','COMPLAINT',result,'Category: '||p_category||'; department: '||d.code);
  return result;
end $$;
create or replace function public.civic_respond_complaint(p_complaint text,p_status public.civic_complaint_status,p_message text)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; c public.complaints;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into c from public.complaints where id=p_complaint for update;
  if not civic_private.can_manage_complaint(p_complaint) then raise exception 'Complaint outside authorized scope' using errcode='42501'; end if;
  if c.status in ('RESOLVED','REJECTED') then raise exception 'Complaint is already closed'; end if;
  if p_status not in ('UNDER_REVIEW','ACTION_PROPOSED','RESOLVED','REJECTED','AWAITING_CITIZEN_INFO') then raise exception 'Invalid officer decision'; end if;
  insert into public.complaint_responses(complaint_id,responded_by,responded_by_name,role_snapshot,message,responded_at)
  values(c.id,a.id,a.name,a.role::text,civic_private.require_text(p_message,'Response'),now());
  update public.complaints set status=p_status,updated_at=now() where id=c.id;
  insert into public.complaint_events(complaint_id,event_type,description,performed_by,performed_by_name)
  values(c.id,case when now()>c.deadline_at then 'Late Response Recorded' else 'Response Recorded' end,
    'Status: '||p_status||'. '||btrim(p_message),a.id,a.name);
  perform civic_private.log_action('COMPLAINT_RESPONSE','COMPLAINT',c.id,'Status: '||p_status);
end $$;
create or replace function public.civic_reply_complaint(p_complaint text,p_message text)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; c public.complaints;
begin
  a := civic_private.require_actor(array['CITIZEN']::public.civic_role[]);
  select * into c from public.complaints where id=p_complaint for update;
  if c.citizen_id is distinct from a.id then raise exception 'Own complaint required' using errcode='42501'; end if;
  if c.status<>'AWAITING_CITIZEN_INFO' then raise exception 'Complaint is not awaiting citizen information'; end if;
  insert into public.complaint_responses(complaint_id,responded_by,responded_by_name,role_snapshot,message,responded_at)
  values(c.id,a.id,a.name,'CITIZEN',civic_private.require_text(p_message,'Additional information'),now());
  update public.complaints set status=case when now()>deadline_at then 'ESCALATED_OVERDUE'::public.civic_complaint_status
    else 'UNDER_REVIEW'::public.civic_complaint_status end,priority=case when now()>deadline_at then 'HIGH'::public.civic_complaint_priority else priority end,updated_at=now() where id=c.id;
  if now()>c.deadline_at then
    insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name)
    values(c.id,'EVT-ESC-'||c.id,c.deadline_at,'Escalated — Overdue',
      '168-hour official response deadline exceeded; citizen information does not reset it.','System Escalation Engine') on conflict do nothing;
    if found then
      insert into public.audit_logs(event_at,action,performed_by_name,target_type,target_id,details)
      values(now(),'COMPLAINT_AUTO_ESCALATED','System Escalation Engine','COMPLAINT',c.id,'Original deadline exceeded while awaiting citizen information.');
    end if;
  end if;
  insert into public.complaint_events(complaint_id,event_type,description,performed_by,performed_by_name)
  values(c.id,'Citizen Information Provided',btrim(p_message),a.id,a.name);
  perform civic_private.log_action('CITIZEN_INFORMATION','COMPLAINT',c.id,'Citizen supplied requested information; original deadline retained.');
end $$;
create or replace function public.civic_assign_complaint(p_complaint text,p_officer uuid)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; c public.complaints; o public.profiles;
begin
  a := civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  select * into c from public.complaints where id=p_complaint for update;
  if c.id is null then raise exception 'Complaint not found'; end if;
  if p_officer is not null then
    select * into o from public.profiles where id=p_officer and is_active and role='OFFICER';
    if o.id is null or not exists(select 1 from public.profile_scopes s where s.profile_id=o.id
      and s.department_id=c.assigned_department_id and s.jurisdiction_id=c.jurisdiction_id) then
      raise exception 'Officer requires an explicit matching complaint scope';
    end if;
  end if;
  update public.complaints set assigned_officer_id=o.id,assigned_officer_name=o.name,assigned_officer_email=o.email,updated_at=now() where id=c.id;
  insert into public.complaint_events(complaint_id,event_type,description,performed_by,performed_by_name)
  values(c.id,'Officer Assigned',coalesce(o.name,'Unassigned'),a.id,a.name);
  perform civic_private.log_action('COMPLAINT_ASSIGNMENT','COMPLAINT',c.id,coalesce(o.id::text,'Unassigned'));
end $$;
create or replace function public.civic_submit_progress(p_project text,p_milestone text,p_percentage smallint,
  p_completed text,p_remaining text,p_obstacles text default null)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; result text;
begin
  a := civic_private.require_actor(array['CONTRACTOR']::public.civic_role[]);
  perform 1 from public.projects where id=p_project for share;
  if not civic_private.contractor_project(p_project) then raise exception 'Assigned contractor project required' using errcode='42501'; end if;
  insert into public.progress_reports(project_id,milestone_id,submitted_by,submitted_by_name,submitted_at,
    completion_percentage,work_completed,remaining_work,obstacles)
  values(p_project,p_milestone,a.id,a.name,now(),p_percentage,civic_private.require_text(p_completed,'Completed work'),
    civic_private.require_text(p_remaining,'Remaining work'),left(p_obstacles,10000)) returning id into result;
  perform civic_private.log_action('PROGRESS_SUBMITTED','PROGRESS_REPORT',result,'Project: '||p_project||'; milestone: '||p_milestone);
  return result;
end $$;
create or replace function public.civic_review_progress(p_report text,p_decision public.civic_progress_status,p_notes text,p_publish boolean default false)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; r public.progress_reports;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into r from public.progress_reports where id=p_report for update;
  if not civic_private.can_manage_project(r.project_id) or r.submitted_by=a.id then raise exception 'Authorized independent reviewer required' using errcode='42501'; end if;
  if r.status<>'Pending Verification' then raise exception 'Report already reviewed'; end if;
  if p_decision='Pending Verification' or (p_publish and p_decision<>'Approved') then raise exception 'Invalid review decision'; end if;
  insert into public.progress_report_reviews(report_id,reviewed_by,reviewer_name,officer_notes,decision,reviewed_at)
  values(r.id,a.id,a.name,civic_private.require_text(p_notes,'Review notes'),p_decision,now());
  update public.progress_reports set status=p_decision,is_public=p_publish where id=r.id;
  if p_decision='Approved' then
    update public.projects set progress_percentage=greatest(progress_percentage,r.completion_percentage),
      status=case when greatest(progress_percentage,r.completion_percentage)=100 then 'COMPLETED'::public.civic_project_status
        when status='PLANNED' then 'ONGOING'::public.civic_project_status else status end,updated_at=now() where id=r.project_id;
    if r.completion_percentage=100 then update public.project_milestones set is_completed=true,completion_date=current_date
      where project_id=r.project_id and milestone_id=r.milestone_id and not is_completed; end if;
  end if;
  perform civic_private.log_action('PROGRESS_REVIEWED','PROGRESS_REPORT',r.id,'Decision: '||p_decision);
end $$;
create or replace function public.civic_record_delay(p_project text,p_date date,p_reason text,p_explanation text)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; p public.projects; result text;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into p from public.projects where id=p_project for update;
  if not civic_private.can_manage_project(p_project) then raise exception 'Project outside authorized scope' using errcode='42501'; end if;
  if p.status='COMPLETED' or p_date<=p.revised_completion_date then raise exception 'A later deadline for an unfinished project is required'; end if;
  insert into public.delay_records(project_id,recorded_by,recorded_by_name,recorded_at,reason,
    original_completion_date,revised_completion_date,published_explanation,is_published)
  values(p.id,a.id,a.name,now(),civic_private.require_text(p_reason,'Reason'),p.revised_completion_date,p_date,
    civic_private.require_text(p_explanation,'Public explanation'),true) returning id into result;
  update public.projects set revised_completion_date=p_date,status='DELAYED',updated_at=now() where id=p.id;
  perform civic_private.log_action('DELAY_RECORDED','PROJECT',p.id,'New deadline: '||p_date);
  return result;
end $$;
create or replace function public.civic_request_deadline(p_project text,p_date date,p_reason text)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; p public.projects; result text;
begin
  a := civic_private.require_actor(array['CONTRACTOR']::public.civic_role[]);
  select * into p from public.projects where id=p_project for share;
  if not civic_private.contractor_project(p_project) then raise exception 'Assigned project required' using errcode='42501'; end if;
  if p.status='COMPLETED' then raise exception 'Project already completed'; end if;
  insert into public.deadline_revision_requests(project_id,submitted_by,current_deadline_snapshot,proposed_completion_date,reason)
  values(p.id,a.id,p.revised_completion_date,p_date,civic_private.require_text(p_reason,'Reason')) returning id into result;
  perform civic_private.log_action('DEADLINE_REQUESTED','DEADLINE_REQUEST',result,'Project: '||p.id);
  return result;
end $$;
create or replace function public.civic_review_deadline(p_request text,p_approve boolean,p_notes text)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; r public.deadline_revision_requests; p public.projects;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into r from public.deadline_revision_requests where id=p_request for update;
  select * into p from public.projects where id=r.project_id for update;
  if not civic_private.can_manage_project(r.project_id) or r.submitted_by=a.id then raise exception 'Independent authorized reviewer required' using errcode='42501'; end if;
  if r.status<>'PENDING' then raise exception 'Request already reviewed'; end if;
  if p_approve and (p.revised_completion_date<>r.current_deadline_snapshot or p.status='COMPLETED') then raise exception 'Project deadline/status changed; request must be resubmitted'; end if;
  update public.deadline_revision_requests set status=case when p_approve then 'APPROVED' else 'REJECTED' end,
    reviewed_by=a.id,reviewer_notes=civic_private.require_text(p_notes,'Review notes'),reviewed_at=now() where id=r.id;
  if p_approve then perform public.civic_record_delay(p.id,r.proposed_completion_date,r.reason,p_notes); end if;
  perform civic_private.log_action('DEADLINE_REVIEWED','DEADLINE_REQUEST',r.id,'Approved: '||p_approve);
end $$;
create or replace function public.civic_record_conflict(p_a text,p_b text,p_type public.civic_conflict_type,p_location text,p_dates text,p_resolution text default null)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; pa public.projects; pb public.projects; result text;
begin
  a := civic_private.require_actor(array['COORDINATOR','ADMINISTRATOR']::public.civic_role[]);
  select * into pa from public.projects where id=p_a;
  select * into pb from public.projects where id=p_b;
  if not coalesce(civic_private.has_scope(pa.department_id,pa.responsible_office_id) and
    civic_private.has_scope(pb.department_id,pb.responsible_office_id),false) then raise exception 'Both projects must be in coordination scope' using errcode='42501'; end if;
  insert into public.project_conflicts(project_a_id,project_b_id,project_a_title_snapshot,project_b_title_snapshot,
    department_a_snapshot,department_b_snapshot,location,conflicting_dates,conflict_type,proposed_resolution)
  values(pa.id,pb.id,pa.title,pb.title,pa.department_name,pb.department_name,civic_private.require_text(p_location,'Location',1000),
    civic_private.require_text(p_dates,'Conflicting dates',500),p_type,left(p_resolution,10000)) returning id into result;
  perform civic_private.log_action('CONFLICT_RECORDED','CONFLICT',result,'Projects: '||pa.id||', '||pb.id);
  return result;
end $$;
create or replace function public.civic_resolve_conflict(p_conflict text,p_resolution text)
returns void language plpgsql security definer set search_path='' as $$
declare c public.project_conflicts;
begin
  perform civic_private.require_actor(array['COORDINATOR','ADMINISTRATOR']::public.civic_role[]);
  select * into c from public.project_conflicts where id=p_conflict for update;
  if not coalesce(civic_private.internal_project(c.project_a_id) and civic_private.internal_project(c.project_b_id),false) then raise exception 'Both projects must be in coordination scope' using errcode='42501'; end if;
  if c.status='RESOLVED' then raise exception 'Conflict already resolved'; end if;
  update public.project_conflicts set proposed_resolution=civic_private.require_text(p_resolution,'Resolution'),
    status='RESOLVED',resolved_at=now(),resolved_by=auth.uid() where id=c.id;
  perform civic_private.log_action('CONFLICT_RESOLVED','CONFLICT',c.id,'Resolution recorded.');
end $$;

create or replace function public.civic_initiate_handover(p_incoming uuid,p_projects text[],p_effective date,p_reason text,p_notes text,p_tasks jsonb default '[]')
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; incoming public.profiles; first_project public.projects; p public.projects; result text; item jsonb;
begin
  a := civic_private.require_actor(array['OFFICER']::public.civic_role[]);
  select * into incoming from public.profiles where id=p_incoming and is_active and role='OFFICER';
  if incoming.id is null or incoming.id=a.id then raise exception 'A different active incoming Officer is required'; end if;
  if coalesce(cardinality(p_projects),0) not between 1 and 100 or cardinality(p_projects)<>(select count(distinct x) from unnest(p_projects) x) then raise exception 'Select 1 to 100 distinct projects'; end if;
  if p_effective is null or p_effective<current_date then raise exception 'Transfer date must be today or later'; end if;
  if jsonb_typeof(p_tasks)<>'array' or jsonb_array_length(p_tasks)>100 then raise exception 'Tasks must be an array of at most 100 items'; end if;
  -- Serialize overlapping initiations and approvals for the same outgoing officer.
  perform pg_advisory_xact_lock(hashtextextended('civic:handover:'||a.id,0));
  select * into first_project from public.projects where id=p_projects[1];
  for p in select * from public.projects where id=any(p_projects) order by id for update loop
    if p.department_id<>first_project.department_id or p.responsible_office_id<>first_project.responsible_office_id
      or not civic_private.can_manage_project(p.id) or not exists(select 1 from public.project_assignments where project_id=p.id and officer_id=a.id)
      or not exists(select 1 from public.profile_scopes where profile_id=incoming.id and department_id=p.department_id and jurisdiction_id=p.responsible_office_id) then
      raise exception 'Every project must be assigned to you in one office, with approved incoming scope' using errcode='42501';
    end if;
    if exists(select 1 from public.handover_projects hp join public.officer_handovers h on h.id=hp.handover_id
      where hp.project_id=p.id and h.status in ('INITIATED','CLARIFICATION_REQUESTED')) then raise exception 'Project already has an open handover'; end if;
  end loop;
  if (select count(*) from public.projects where id=any(p_projects))<>cardinality(p_projects) then raise exception 'Unknown project'; end if;
  insert into public.officer_handovers(outgoing_officer_id,outgoing_officer_name,outgoing_officer_email,
    incoming_officer_id,incoming_officer_name,incoming_officer_email,department_id,department_name,
    office_jurisdiction_id,office_jurisdiction,effective_transfer_date,reason_for_reassignment,handover_notes)
  values(a.id,a.name,a.email,incoming.id,incoming.name,incoming.email,first_project.department_id,first_project.department_name,
    first_project.responsible_office_id,first_project.responsible_office,p_effective,
    civic_private.require_text(p_reason,'Transfer reason'),civic_private.require_text(p_notes,'Handover notes')) returning id into result;
  insert into public.handover_projects(handover_id,project_id) select result,unnest(p_projects);
  for item in select value from jsonb_array_elements(p_tasks) loop
    insert into public.handover_pending_tasks(handover_id,id,related_project_id,title,priority,due_date)
    values(result,'TASK-'||gen_random_uuid(),item->>'project_id',civic_private.require_text(item->>'title','Task title',500),item->>'priority',(item->>'due_date')::date);
  end loop;
  perform civic_private.log_action('HANDOVER_INITIATED','HANDOVER',result,'Projects: '||cardinality(p_projects));
  return result;
end $$;
create or replace function public.civic_answer_handover(p_handover text,p_acknowledge boolean,p_clarification text default null)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; h public.officer_handovers; p public.projects;
begin
  a := civic_private.require_actor(array['OFFICER']::public.civic_role[]);
  select * into h from public.officer_handovers where id=p_handover for update;
  if h.incoming_officer_id is distinct from a.id or not civic_private.can_read_handover(h.id) then raise exception 'Authorized incoming Officer required' using errcode='42501'; end if;
  if h.status='ACKNOWLEDGED' then raise exception 'Handover already acknowledged'; end if;
  if p_acknowledge then
    if current_date<h.effective_transfer_date then raise exception 'Transfer date has not arrived'; end if;
    perform pg_advisory_xact_lock(hashtextextended('civic:handover:'||h.outgoing_officer_id,0));
    for p in select pr.* from public.projects pr join public.handover_projects hp on hp.project_id=pr.id
      where hp.handover_id=h.id order by pr.id for update of pr loop
      if not civic_private.can_manage_project(p.id) or not exists(select 1 from public.project_assignments where project_id=p.id and officer_id=h.outgoing_officer_id) then
        raise exception 'Scope or outgoing assignment has changed' using errcode='42501';
      end if;
      update public.project_assignments set officer_id=a.id,updated_at=now() where project_id=p.id;
      update public.projects set current_officer_in_charge=a.name,updated_at=now() where id=p.id;
    end loop;
    if not exists(select 1 from public.handover_projects where handover_id=h.id) then raise exception 'Handover has no transferable projects'; end if;
    update public.officer_handovers set status='ACKNOWLEDGED',acknowledged_at=now() where id=h.id;
  else
    update public.officer_handovers set status='CLARIFICATION_REQUESTED',
      clarification_notes=civic_private.require_text(p_clarification,'Clarification') where id=h.id;
  end if;
  perform civic_private.log_action('HANDOVER_ANSWERED','HANDOVER',h.id,'Acknowledged: '||p_acknowledge);
end $$;

create or replace function public.civic_admin_set_user(p_user uuid,p_role public.civic_role,p_active boolean,
  p_department text default null,p_office text default null,p_contractor text default null)
returns void language plpgsql security definer set search_path='' as $$
declare target public.profiles;
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  perform pg_advisory_xact_lock(hashtextextended('civic:admin-user-management',0));
  select * into target from public.profiles where id=p_user for update;
  if target.id is null then raise exception 'Existing Auth profile required'; end if;
  if p_user=auth.uid() then raise exception 'Administrator cannot change own role or account status'; end if;
  if p_role<>'CITIZEN' and not exists(select 1 from auth.users where id=p_user and not coalesce(is_anonymous,false) and email_confirmed_at is not null) then raise exception 'Elevated roles require a confirmed permanent Auth identity'; end if;
  if p_role='CONTRACTOR' and (p_contractor is null or not exists(select 1 from public.contractors where id=p_contractor and is_active)) then raise exception 'Active contractor association required'; end if;
  if p_role<>'CONTRACTOR' and p_contractor is not null then raise exception 'Contractor association applies only to contractors'; end if;
  update public.profiles set role=p_role,is_active=p_active,department_id=p_department,
    office_jurisdiction_id=p_office,contractor_id=p_contractor,updated_at=now() where id=p_user;
  -- Reauthorization is explicit after role/scope identity changes; stale scopes are removed.
  delete from public.profile_scopes where profile_id=p_user;
  perform civic_private.log_action('USER_AUTHORIZATION_CHANGED','PROFILE',p_user::text,'Role: '||p_role||'; active: '||p_active);
end $$;
create or replace function public.civic_admin_set_scope(p_user uuid,p_department text,p_jurisdiction text,p_grant boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  if not exists(select 1 from public.profiles where id=p_user and is_active and role in ('OFFICER','COORDINATOR')) then raise exception 'Active Officer or Coordinator required'; end if;
  if p_grant then
    if not exists(select 1 from public.departments where id=p_department and is_active)
      or not exists(select 1 from public.jurisdictions where id=p_jurisdiction and is_active) then raise exception 'Active department and jurisdiction required'; end if;
    insert into public.profile_scopes(profile_id,department_id,jurisdiction_id,granted_by)
      values(p_user,p_department,p_jurisdiction,auth.uid()) on conflict do nothing;
  else delete from public.profile_scopes where profile_id=p_user and department_id=p_department and jurisdiction_id=p_jurisdiction; end if;
  perform civic_private.log_action('USER_SCOPE_CHANGED','PROFILE',p_user::text,p_department||' / '||p_jurisdiction||'; grant: '||p_grant);
end $$;
create or replace function public.civic_admin_department(p_id text,p_code text,p_name text,p_description text,p_active boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  insert into public.departments(id,code,name,scope_description,is_active) values(p_id,p_code,p_name,p_description,p_active)
  on conflict(id) do update set code=excluded.code,name=excluded.name,scope_description=excluded.scope_description,is_active=excluded.is_active;
  perform civic_private.log_action('DEPARTMENT_CHANGED','DEPARTMENT',p_id,'Catalog details/status updated.');
end $$;
create or replace function public.civic_admin_jurisdiction(p_id text,p_name text,p_kind text,p_parent text,p_district text,p_details text,p_active boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  perform pg_advisory_xact_lock(hashtextextended('civic:jurisdiction-tree',0));
  if p_parent=p_id or exists(with recursive ancestors(id,parent_id) as (
    select id,parent_id from public.jurisdictions where id=p_parent
    union select j.id,j.parent_id from public.jurisdictions j join ancestors a on a.parent_id=j.id)
    select 1 from ancestors where id=p_id) then raise exception 'Jurisdiction hierarchy cannot contain cycles'; end if;
  insert into public.jurisdictions(id,name,kind,parent_id,district,details,is_active)
    values(p_id,p_name,p_kind,p_parent,p_district,p_details,p_active)
  on conflict(id) do update set name=excluded.name,kind=excluded.kind,parent_id=excluded.parent_id,
    district=excluded.district,details=excluded.details,is_active=excluded.is_active;
  perform civic_private.log_action('JURISDICTION_CHANGED','JURISDICTION',p_id,'Catalog details/status updated.');
end $$;
create or replace function public.civic_admin_contractor(p_id text,p_name text,p_active boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  insert into public.contractors(id,name,is_active) values(p_id,p_name,p_active)
  on conflict(id) do update set name=excluded.name,is_active=excluded.is_active;
  perform civic_private.log_action('CONTRACTOR_CHANGED','CONTRACTOR',p_id,'Catalog details/status updated.');
end $$;
create or replace function civic_private.escalate_overdue()
returns integer language plpgsql security definer set search_path='' as $$
declare c public.complaints; total integer := 0;
begin
  for c in select * from public.complaints where deadline_at<now()
    and status in ('PENDING','UNDER_REVIEW','AWAITING_CITIZEN_INFO') order by id for update skip locked loop
    update public.complaints set status='ESCALATED_OVERDUE',priority='HIGH',updated_at=now() where id=c.id;
    insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name)
    values(c.id,'EVT-ESC-'||c.id,c.deadline_at,'Escalated — Overdue',
      '168-hour response deadline passed while complaint remained pending, under review or awaiting citizen information.', 'System Escalation Engine') on conflict do nothing;
    insert into public.audit_logs(event_at,action,performed_by_name,target_type,target_id,details)
    values(now(),'COMPLAINT_AUTO_ESCALATED','System Escalation Engine','COMPLAINT',c.id,'168-hour deadline exceeded.');
    total := total+1;
  end loop;
  return total;
end $$;

-- Explicitly grant only named civic_* RPCs, never every function in public.
do $$ declare f record;
begin
  for f in select p.oid::regprocedure as signature from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname='public' and p.proname in (
      'civic_update_my_profile','civic_set_follow','civic_register_project','civic_publish_project',
      'civic_submit_complaint','civic_respond_complaint','civic_reply_complaint','civic_assign_complaint',
      'civic_submit_progress','civic_review_progress','civic_record_delay','civic_request_deadline',
      'civic_review_deadline','civic_record_conflict','civic_resolve_conflict','civic_initiate_handover',
      'civic_answer_handover','civic_admin_set_user','civic_admin_set_scope','civic_admin_department',
      'civic_admin_jurisdiction','civic_admin_contractor') loop
    execute format('revoke all on function %s from public,anon,authenticated',f.signature);
    execute format('grant execute on function %s to authenticated',f.signature);
  end loop;
end $$;
revoke all on function civic_private.require_actor(public.civic_role[]),
  civic_private.log_action(text,text,text,text),civic_private.require_text(text,text,integer),
  civic_private.next_project_id(text),civic_private.escalate_overdue() from public,anon,authenticated;
insert into civic_private.schema_migrations(version) values('003_civic_workflows') on conflict do nothing;

-- ===== migrations/004_civic_storage.sql =====
-- Private buckets; object paths are user UUID / business record ID / random UUID.ext.
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types) values
 ('complaint-photos','complaint-photos',false,10485760,array['image/jpeg','image/png','image/webp']),
 ('progress-evidence','progress-evidence',false,10485760,array['image/jpeg','image/png','image/webp']),
 ('deadline-documents','deadline-documents',false,10485760,array['application/pdf'])
on conflict(id) do nothing;
do $$ begin
  if exists(select 1 from storage.buckets where id in ('complaint-photos','progress-evidence','deadline-documents')
    and (public or file_size_limit is distinct from 10485760::bigint or
      allowed_mime_types is distinct from case when id='deadline-documents' then array['application/pdf']
        else array['image/jpeg','image/png','image/webp'] end)) then
    raise exception 'Existing CivicTech bucket configuration conflicts; inspect it instead of overwriting';
  end if;
end $$;
create or replace function civic_private.storage_upload_allowed(bucket text,path text)
returns boolean language sql stable security definer set search_path='' as $$
  select civic_private.active_role() is not null
    and split_part(path,'/',1)=auth.uid()::text
    and array_length(string_to_array(path,'/'),1)=3
    and split_part(path,'/',3) ~ case when bucket='deadline-documents'
      then '^[0-9a-f-]{36}\.pdf$' else '^[0-9a-f-]{36}\.(jpg|jpeg|png|webp)$' end
    and case bucket
      when 'complaint-photos' then exists(select 1 from public.complaints c
        where c.id=split_part(path,'/',2) and c.citizen_id=auth.uid()
          and civic_private.active_role()='CITIZEN' and c.status not in ('RESOLVED','REJECTED'))
      when 'progress-evidence' then exists(select 1 from public.progress_reports r
        where r.id=split_part(path,'/',2) and r.submitted_by=auth.uid()
          and r.status='Pending Verification' and civic_private.contractor_project(r.project_id))
      when 'deadline-documents' then exists(select 1 from public.deadline_revision_requests r
        where r.id=split_part(path,'/',2) and r.submitted_by=auth.uid()
          and r.status='PENDING' and civic_private.contractor_project(r.project_id))
      else false end;
$$;
create or replace function civic_private.storage_is_linked(bucket text,path text)
returns boolean language sql stable security definer set search_path='' as $$
  select case bucket
    when 'complaint-photos' then exists(select 1 from public.complaints where photo_path=path)
    when 'progress-evidence' then exists(select 1 from public.progress_reports where photo_path=path)
    when 'deadline-documents' then exists(select 1 from public.deadline_revision_requests where document_path=path)
    else false end;
$$;
create or replace function civic_private.storage_read_allowed(bucket text,path text)
returns boolean language sql stable security definer set search_path='' as $$
  select civic_private.storage_upload_allowed(bucket,path) or case bucket
    when 'complaint-photos' then exists(select 1 from public.complaints c where c.photo_path=path and civic_private.can_read_complaint(c.id))
    when 'progress-evidence' then exists(select 1 from public.progress_reports r where r.photo_path=path
      and ((r.is_public and r.status='Approved' and civic_private.public_project(r.project_id)) or civic_private.internal_project(r.project_id)))
    when 'deadline-documents' then exists(select 1 from public.deadline_revision_requests r where r.document_path=path
      and (civic_private.can_manage_project(r.project_id) or (r.submitted_by=auth.uid() and civic_private.contractor_project(r.project_id))))
    else false end;
$$;
create or replace function public.civic_attach_file(p_bucket text,p_record text,p_path text)
returns void language plpgsql security definer set search_path='' as $$
declare existing text;
begin
  perform civic_private.require_actor(array['CITIZEN','CONTRACTOR']::public.civic_role[]);
  -- Lock business row first, then check pending status to serialize with review.
  case p_bucket
    when 'complaint-photos' then select photo_path into existing from public.complaints where id=p_record for update;
    when 'progress-evidence' then select photo_path into existing from public.progress_reports where id=p_record for update;
    when 'deadline-documents' then select document_path into existing from public.deadline_revision_requests where id=p_record for update;
    else raise exception 'Unsupported bucket';
  end case;
  if split_part(p_path,'/',2)<>p_record or not coalesce(civic_private.storage_upload_allowed(p_bucket,p_path),false) then
    raise exception 'Authorized record path required' using errcode='42501';
  end if;
  if existing is not null and existing<>p_path then raise exception 'Linked evidence is immutable; upload evidence for a new submission'; end if;
  if not exists(select 1 from storage.objects where bucket_id=p_bucket and name=p_path and owner_id=auth.uid()::text) then
    raise exception 'Upload the owned object through Supabase Storage first';
  end if;
  case p_bucket
    when 'complaint-photos' then update public.complaints set photo_path=p_path,updated_at=now() where id=p_record;
    when 'progress-evidence' then update public.progress_reports set photo_path=p_path where id=p_record;
    when 'deadline-documents' then update public.deadline_revision_requests set document_path=p_path where id=p_record;
  end case;
  perform civic_private.log_action('ATTACHMENT_LINKED','ATTACHMENT',p_record,'Bucket: '||p_bucket);
end $$;
-- Restrictive guards prevent broader pre-existing permissive policies from
-- accidentally opening these buckets. Other buckets retain existing behavior.
drop policy if exists civic_storage_read_guard on storage.objects;
create policy civic_storage_read_guard on storage.objects as restrictive for select to anon,authenticated
using(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents') or civic_private.storage_read_allowed(bucket_id,name));
drop policy if exists civic_storage_insert_guard on storage.objects;
create policy civic_storage_insert_guard on storage.objects as restrictive for insert to anon,authenticated
with check(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents') or
  (owner_id=(select auth.uid())::text and civic_private.storage_upload_allowed(bucket_id,name)));
drop policy if exists civic_storage_update_guard on storage.objects;
create policy civic_storage_update_guard on storage.objects as restrictive for update to anon,authenticated
using(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents'))
with check(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents'));
drop policy if exists civic_storage_delete_guard on storage.objects;
create policy civic_storage_delete_guard on storage.objects as restrictive for delete to anon,authenticated
using(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents'));
drop policy if exists civic_storage_read on storage.objects;
create policy civic_storage_read on storage.objects for select to anon,authenticated
using(bucket_id in ('complaint-photos','progress-evidence','deadline-documents') and civic_private.storage_read_allowed(bucket_id,name));
drop policy if exists civic_storage_insert on storage.objects;
create policy civic_storage_insert on storage.objects for insert to authenticated
with check(bucket_id in ('complaint-photos','progress-evidence','deadline-documents')
  and owner_id=(select auth.uid())::text and civic_private.storage_upload_allowed(bucket_id,name));
drop policy if exists civic_storage_delete on storage.objects;
-- No client DELETE or UPDATE policy. Trusted maintenance may clean orphaned
-- uploads through Storage API after checking that no business record links them.
revoke all on function civic_private.storage_upload_allowed(text,text),
  civic_private.storage_is_linked(text,text),civic_private.storage_read_allowed(text,text) from public,anon,authenticated;
grant execute on function civic_private.storage_upload_allowed(text,text),
  civic_private.storage_is_linked(text,text),civic_private.storage_read_allowed(text,text) to anon,authenticated;
revoke all on function public.civic_attach_file(text,text,text) from public,anon,authenticated;
grant execute on function public.civic_attach_file(text,text,text) to authenticated;
insert into civic_private.schema_migrations(version) values('004_civic_storage') on conflict do nothing;

-- ===== seeds/001_android_demo.sql =====
-- DEMONSTRATION DATA ONLY: extracted from CivicDataRepository.kt, not verified government records.
-- Applied once; reruns skip the seed entirely, so live records are never reset.
-- Historical actors remain name snapshots, not fabricated Auth identities.
do $seed$
declare anchor timestamptz := clock_timestamp();
begin
  if exists(select 1 from civic_private.schema_migrations where version='demo_seed_v1') then return; end if;
  -- Reject collisions before any inserts. Never overwrite real records.
  if exists(select 1 from public.projects where id in ('TN-PWD-2025-01','TN-TWAD-2025-02'))
    or exists(select 1 from public.complaints where id in ('CMP-2026-101','CMP-2026-102','CMP-2026-103','CMP-2026-104','CMP-2026-105','CMP-2026-106'))
    or exists(select 1 from public.officer_handovers where id='HO-2026-88')
    or exists(select 1 from public.audit_logs where id in ('AL-100','AL-101')) then
    raise exception 'Demo IDs already exist without seed marker. Inspect collisions; never overwrite records.';
  end if;
insert into public.departments(id,code,name,scope_description,source_labels,is_demo) values('PWD','PWD','Public Works Department','Highways, Buildings, Water Bodies',array['Public Works Department','Public Works Department (PWD)'],true) on conflict do nothing;
insert into public.departments(id,code,name,scope_description,source_labels,is_demo) values('TWAD','TWAD','TWAD Board','Tamil Nadu Water Supply & Drainage Board',array['TWAD Board'],true) on conflict do nothing;
insert into public.departments(id,code,name,scope_description,source_labels,is_demo) values('HWY','HWY','Highways Department','State Highways, Major District Roads',array['Highways Department'],true) on conflict do nothing;
insert into public.departments(id,code,name,scope_description,source_labels,is_demo) values('MAWS','MAWS','Municipal Administration & Water Supply','Corporations, Municipalities, Panchayats',array['Municipal Administration','Municipal Administration & Water Supply'],true) on conflict do nothing;
insert into public.departments(id,code,name,scope_description,source_labels,is_demo) values('TANGEDCO','TANGEDCO','TANGEDCO','Electricity Board Power Line Relocation',array['TANGEDCO'],true) on conflict do nothing;
insert into public.departments(id,code,name,scope_description,source_labels,is_demo) values('COORD','COORD','Inter-Departmental Coordination Cell',null,array['Inter-Departmental Coordination Cell'],true) on conflict do nothing;
insert into public.departments(id,code,name,scope_description,source_labels,is_demo) values('CONTRACT','CONTRACT','Contractor Wing',null,array['Contractor Wing'],true) on conflict do nothing;
insert into public.departments(id,code,name,scope_description,source_labels,is_demo) values('GOVTECH','GOVTECH','Governance Tech Wing',null,array['Governance Tech Wing'],true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('GCC','Greater Chennai Corporation','MUNICIPALITY','Chennai','38 Wards, Metro Circle',true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('CMC','Coimbatore Municipal Corporation','MUNICIPALITY','Coimbatore','East, West, South, North Zones',true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('MADURAI','Madurai Corporation','MUNICIPALITY','Madurai','Vaigai Circle, Central Zone',true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('TRICHY','Tiruchirappalli Corporation','MUNICIPALITY','Tiruchirappalli','Srirangam & Golden Rock Zones',true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('SALEM','Salem City Municipal Corporation','MUNICIPALITY','Salem','Zone 1, 2, 3, 4 Divisions',true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('PWD-CHENNAI-OFFICE','Highways & PWD Chennai Metro Division','OFFICE','Chennai',null,true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('TWAD-COIMBATORE-OFFICE','TWAD Water Supply Circle Coimbatore','OFFICE','Coimbatore',null,true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('COORD-CELL','Tamil Nadu State Infrastructure Cell','OFFICE',null,null,true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('LT-OFFICE','L&T Infrastructure Ltd','OFFICE',null,null,true) on conflict do nothing;
insert into public.jurisdictions(id,name,kind,district,details,is_demo) values('EGOV-OFFICE','Tamil Nadu E-Governance Agency','OFFICE',null,null,true) on conflict do nothing;
if exists(select 1 from public.departments where id in ('PWD','TWAD','HWY','MAWS','TANGEDCO','COORD','CONTRACT','GOVTECH') and not is_demo)
 or exists(select 1 from public.jurisdictions where id in ('GCC','CMC','MADURAI','TRICHY','SALEM','PWD-CHENNAI-OFFICE','TWAD-COIMBATORE-OFFICE','COORD-CELL','LT-OFFICE','EGOV-OFFICE') and not is_demo)
 or exists(select 1 from public.contractors where id in ('CON-LT-9081','CON-KSR-4421') and not is_demo) then
 raise exception 'Demo catalog IDs conflict with non-demo records'; end if;
insert into public.contractors(id,name,is_demo) values('CON-LT-9081','L&T Public Infrastructure Ltd',true) on conflict do nothing;
insert into public.projects(id,title,description,department_id,department_name,district,municipal_body_id,municipal_body,location,latitude,longitude,contractor_id,contractor_name,responsible_office_id,responsible_office,current_officer_in_charge,estimated_budget_in_lakhs,funding_source,start_date,original_completion_date,revised_completion_date,status,progress_percentage,is_published,is_demo,source_snapshot) values('TN-PWD-2025-01','Anna Salai Stormwater Drain & Road Resurfacing','Comprehensive stormwater drain network construction and high-grade asphalt resurfacing along Anna Salai corridor to prevent monsoon flooding.','PWD','Public Works Department','Chennai','GCC','Greater Chennai Corporation','Anna Salai (Teynampet to Nandanam), Chennai',13.0418,80.2341,'CON-LT-9081','L&T Public Infrastructure Ltd','PWD-CHENNAI-OFFICE','Highways & PWD Chennai Metro Division','Er. R. Kanimozhi (Formerly Er. S. Sundaram)',450,'Tamil Nadu Urban Infrastructure Fund','2025-01-15','2025-05-30','2025-06-15','ONGOING',68,true,true,'{"is_demonstration":true,"kotlin_constructor":"Project(\r\n                    id = \"TN-PWD-2025-01\",\r\n                    title = \"Anna Salai Stormwater Drain & Road Resurfacing\",\r\n                    description = \"Comprehensive stormwater drain network construction and high-grade asphalt resurfacing along Anna Salai corridor to prevent monsoon flooding.\",\r\n                    department = \"Public Works Department\",\r\n                    district = \"Chennai\",\r\n                    municipalBody = \"Greater Chennai Corporation\",\r\n                    location = \"Anna Salai (Teynampet to Nandanam), Chennai\",\r\n                    latitude = 13.0418,\r\n                    longitude = 80.2341,\r\n                    contractorName = \"L&T Public Infrastructure Ltd\",\r\n                    contractorId = \"CON-LT-9081\",\r\n                    responsibleOffice = \"Highways & PWD Chennai Metro Division\",\r\n                    currentOfficerInCharge = \"Er. R. Kanimozhi (Formerly Er. S. Sundaram)\",\r\n                    estimatedBudgetInLakhs = 450.0,\r\n                    fundingSource = \"Tamil Nadu Urban Infrastructure Fund\",\r\n                    startDate = \"15 Jan 2025\",\r\n                    originalCompletionDate = \"30 May 2025\",\r\n                    revisedCompletionDate = \"15 Jun 2025\",\r\n                    status = ProjectStatus.ONGOING,\r\n                    progressPercentage = 68,\r\n                    milestones = listOf(\r\n                        Milestone(\"M1\", \"Site Excavation & Utility Mapping\", \"10 Feb 2025\", true, \"08 Feb 2025\"),\r\n                        Milestone(\"M2\", \"Reinforced Concrete Box Culvert Laying\", \"25 Mar 2025\", true, \"22 Mar 2025\"),\r\n                        Milestone(\"M3\", \"Stormwater Outlet Linkage to Cooum River\", \"30 Apr 2025\", false),\r\n                        Milestone(\"M4\", \"Asphalt Road Surface Relaying & Pavement\", \"30 May 2025\", false)\r\n                    ),\r\n                    isFollowed = true\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.project_milestones(project_id,milestone_id,title,target_date,is_completed,completion_date,source_snapshot) values('TN-PWD-2025-01','M1','Site Excavation & Utility Mapping','2025-02-10',true,'2025-02-08','{"is_demonstration":true,"kotlin_constructor":"Milestone(\"M1\", \"Site Excavation & Utility Mapping\", \"10 Feb 2025\", true, \"08 Feb 2025\")"}'::jsonb) on conflict do nothing;
insert into public.project_milestones(project_id,milestone_id,title,target_date,is_completed,completion_date,source_snapshot) values('TN-PWD-2025-01','M2','Reinforced Concrete Box Culvert Laying','2025-03-25',true,'2025-03-22','{"is_demonstration":true,"kotlin_constructor":"Milestone(\"M2\", \"Reinforced Concrete Box Culvert Laying\", \"25 Mar 2025\", true, \"22 Mar 2025\")"}'::jsonb) on conflict do nothing;
insert into public.project_milestones(project_id,milestone_id,title,target_date,is_completed,completion_date,source_snapshot) values('TN-PWD-2025-01','M3','Stormwater Outlet Linkage to Cooum River','2025-04-30',false,null,'{"is_demonstration":true,"kotlin_constructor":"Milestone(\"M3\", \"Stormwater Outlet Linkage to Cooum River\", \"30 Apr 2025\", false)"}'::jsonb) on conflict do nothing;
insert into public.project_milestones(project_id,milestone_id,title,target_date,is_completed,completion_date,source_snapshot) values('TN-PWD-2025-01','M4','Asphalt Road Surface Relaying & Pavement','2025-05-30',false,null,'{"is_demonstration":true,"kotlin_constructor":"Milestone(\"M4\", \"Asphalt Road Surface Relaying & Pavement\", \"30 May 2025\", false)"}'::jsonb) on conflict do nothing;
insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
    values('android_demo_v1','unowned_follow','TN-PWD-2025-01','Global isFollowed has no source owner; no per-user follow fabricated.','{"is_demonstration":true,"kotlin_constructor":"Project(\r\n                    id = \"TN-PWD-2025-01\",\r\n                    title = \"Anna Salai Stormwater Drain & Road Resurfacing\",\r\n                    description = \"Comprehensive stormwater drain network construction and high-grade asphalt resurfacing along Anna Salai corridor to prevent monsoon flooding.\",\r\n                    department = \"Public Works Department\",\r\n                    district = \"Chennai\",\r\n                    municipalBody = \"Greater Chennai Corporation\",\r\n                    location = \"Anna Salai (Teynampet to Nandanam), Chennai\",\r\n                    latitude = 13.0418,\r\n                    longitude = 80.2341,\r\n                    contractorName = \"L&T Public Infrastructure Ltd\",\r\n                    contractorId = \"CON-LT-9081\",\r\n                    responsibleOffice = \"Highways & PWD Chennai Metro Division\",\r\n                    currentOfficerInCharge = \"Er. R. Kanimozhi (Formerly Er. S. Sundaram)\",\r\n                    estimatedBudgetInLakhs = 450.0,\r\n                    fundingSource = \"Tamil Nadu Urban Infrastructure Fund\",\r\n                    startDate = \"15 Jan 2025\",\r\n                    originalCompletionDate = \"30 May 2025\",\r\n                    revisedCompletionDate = \"15 Jun 2025\",\r\n                    status = ProjectStatus.ONGOING,\r\n                    progressPercentage = 68,\r\n                    milestones = listOf(\r\n                        Milestone(\"M1\", \"Site Excavation & Utility Mapping\", \"10 Feb 2025\", true, \"08 Feb 2025\"),\r\n                        Milestone(\"M2\", \"Reinforced Concrete Box Culvert Laying\", \"25 Mar 2025\", true, \"22 Mar 2025\"),\r\n                        Milestone(\"M3\", \"Stormwater Outlet Linkage to Cooum River\", \"30 Apr 2025\", false),\r\n                        Milestone(\"M4\", \"Asphalt Road Surface Relaying & Pavement\", \"30 May 2025\", false)\r\n                    ),\r\n                    isFollowed = true\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.contractors(id,name,is_demo) values('CON-KSR-4421','KSR Infrastructure Projects',true) on conflict do nothing;
insert into public.projects(id,title,description,department_id,department_name,district,municipal_body_id,municipal_body,location,latitude,longitude,contractor_id,contractor_name,responsible_office_id,responsible_office,current_officer_in_charge,estimated_budget_in_lakhs,funding_source,start_date,original_completion_date,revised_completion_date,status,progress_percentage,is_published,is_demo,source_snapshot) values('TN-TWAD-2025-02','Avinashi Road Water Pipeline Distribution Upgrade','Replacement of aging iron water mains with high-density polyethylene pipelines to ensure 24/7 drinking water supply across East Coimbatore.','TWAD','TWAD Board','Coimbatore','CMC','Coimbatore Municipal Corporation','Avinashi Road (Peelamedu to Hope College), Coimbatore',11.0283,77.0012,'CON-KSR-4421','KSR Infrastructure Projects','TWAD-COIMBATORE-OFFICE','TWAD Water Supply Circle Coimbatore','Er. M. Saravanan',280,'AMRUT 2.0 Scheme','2025-02-01','2025-04-28','2025-07-20','DELAYED',42,true,true,'{"is_demonstration":true,"kotlin_constructor":"Project(\r\n                    id = \"TN-TWAD-2025-02\",\r\n                    title = \"Avinashi Road Water Pipeline Distribution Upgrade\",\r\n                    description = \"Replacement of aging iron water mains with high-density polyethylene pipelines to ensure 24/7 drinking water supply across East Coimbatore.\",\r\n                    department = \"TWAD Board\",\r\n                    district = \"Coimbatore\",\r\n                    municipalBody = \"Coimbatore Municipal Corporation\",\r\n                    location = \"Avinashi Road (Peelamedu to Hope College), Coimbatore\",\r\n                    latitude = 11.0283,\r\n                    longitude = 77.0012,\r\n                    contractorName = \"KSR Infrastructure Projects\",\r\n                    contractorId = \"CON-KSR-4421\",\r\n                    responsibleOffice = \"TWAD Water Supply Circle Coimbatore\",\r\n                    currentOfficerInCharge = \"Er. M. Saravanan\",\r\n                    estimatedBudgetInLakhs = 280.0,\r\n                    fundingSource = \"AMRUT 2.0 Scheme\",\r\n                    startDate = \"01 Feb 2025\",\r\n                    originalCompletionDate = \"28 Apr 2025\",\r\n                    revisedCompletionDate = \"20 Jul 2025\",\r\n                    status = ProjectStatus.DELAYED,\r\n                    progressPercentage = 42\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.complaints(id,category,project_id,project_title_snapshot,submitted_by_citizen_name,location,district,description,submitted_at,assigned_department_id,assigned_department_name,jurisdiction_id,assigned_officer_name,assigned_officer_email,status,priority,is_demo,source_snapshot) values('CMP-2026-101','POTHOLE','TN-TWAD-2025-02','Avinashi Road Water Pipeline Upgrade','S. Karthik','Avinashi Road Near Hope College, Coimbatore','Coimbatore','Unpaved trench left open after water pipeline work causing traffic bottlenecks.',(anchor-interval '24 hours'),'TWAD','TWAD Board','TWAD-COIMBATORE-OFFICE','Er. M. Saravanan','officer.saravanan@twad.tn.gov.in','UNDER_REVIEW','NORMAL',true,'{"is_demonstration":true,"kotlin_constructor":"Complaint(\r\n                    id = \"CMP-2026-101\",\r\n                    category = ComplaintCategory.POTHOLE,\r\n                    projectId = \"TN-TWAD-2025-02\",\r\n                    projectTitle = \"Avinashi Road Water Pipeline Upgrade\",\r\n                    location = \"Avinashi Road Near Hope College, Coimbatore\",\r\n                    district = \"Coimbatore\",\r\n                    description = \"Unpaved trench left open after water pipeline work causing traffic bottlenecks.\",\r\n                    submittedByCitizenName = \"S. Karthik\",\r\n                    submittedDate = dateFormat.format(Date(subTime1)),\r\n                    submissionTimestampMillis = subTime1,\r\n                    deadlineTimestampMillis = deadTime1,\r\n                    assignedDepartment = \"TWAD Board\",\r\n                    assignedOfficerName = \"Er. M. Saravanan\",\r\n                    assignedOfficerEmail = \"officer.saravanan@twad.tn.gov.in\",\r\n                    status = ComplaintStatus.UNDER_REVIEW,\r\n                    priority = ComplaintPriority.NORMAL,\r\n                    lastUpdatedDate = \"1 day ago\",\r\n                    eventHistory = listOf(\r\n                        ComplaintEvent(\"E1\", subTime1, dateFormat.format(Date(subTime1)), \"Submitted\", \"Complaint registered by citizen S. Karthik.\", \"S. Karthik\"),\r\n                        ComplaintEvent(\"E2\", subTime1 + (2 * hourMillis), dateFormat.format(Date(subTime1 + 2 * hourMillis)), \"Officer Assigned\", \"Assigned to Er. M. Saravanan (TWAD Board).\", \"System\"),\r\n                        ComplaintEvent(\"E3\", subTime1 + (5 * hourMillis), dateFormat.format(Date(subTime1 + 5 * hourMillis)), \"Review Started\", \"Official review initiated.\", \"Er. M. Saravanan\")\r\n                    )\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-101','E1',(anchor-interval '24 hours'),'Submitted','Complaint registered by citizen S. Karthik.','S. Karthik','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E1\", subTime1, dateFormat.format(Date(subTime1)), \"Submitted\", \"Complaint registered by citizen S. Karthik.\", \"S. Karthik\")"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-101','E2',(anchor-interval '24 hours')+interval '2 hours','Officer Assigned','Assigned to Er. M. Saravanan (TWAD Board).','System','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E2\", subTime1 + (2 * hourMillis), dateFormat.format(Date(subTime1 + 2 * hourMillis)), \"Officer Assigned\", \"Assigned to Er. M. Saravanan (TWAD Board).\", \"System\")"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-101','E3',(anchor-interval '24 hours')+interval '5 hours','Review Started','Official review initiated.','Er. M. Saravanan','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E3\", subTime1 + (5 * hourMillis), dateFormat.format(Date(subTime1 + 5 * hourMillis)), \"Review Started\", \"Official review initiated.\", \"Er. M. Saravanan\")"}'::jsonb) on conflict do nothing;
insert into public.complaints(id,category,project_id,project_title_snapshot,submitted_by_citizen_name,location,district,description,submitted_at,assigned_department_id,assigned_department_name,jurisdiction_id,assigned_officer_name,assigned_officer_email,status,priority,is_demo,source_snapshot) values('CMP-2026-102','DRAINAGE_OVERFLOW','TN-PWD-2025-01','Anna Salai Stormwater Drain & Road Resurfacing','M. Meenakshi','Anna Salai Service Lane, Teynampet, Chennai','Chennai','Construction debris blocking existing roadside storm drain outlet.',(anchor-interval '164 hours'),'PWD','Public Works Department','PWD-CHENNAI-OFFICE','Er. R. Kanimozhi','officer.kanimozhi@pwd.tn.gov.in','PENDING','NORMAL',true,'{"is_demonstration":true,"kotlin_constructor":"Complaint(\r\n                    id = \"CMP-2026-102\",\r\n                    category = ComplaintCategory.DRAINAGE_OVERFLOW,\r\n                    projectId = \"TN-PWD-2025-01\",\r\n                    projectTitle = \"Anna Salai Stormwater Drain & Road Resurfacing\",\r\n                    location = \"Anna Salai Service Lane, Teynampet, Chennai\",\r\n                    district = \"Chennai\",\r\n                    description = \"Construction debris blocking existing roadside storm drain outlet.\",\r\n                    submittedByCitizenName = \"M. Meenakshi\",\r\n                    submittedDate = dateFormat.format(Date(subTime2)),\r\n                    submissionTimestampMillis = subTime2,\r\n                    deadlineTimestampMillis = deadTime2,\r\n                    assignedDepartment = \"Public Works Department\",\r\n                    assignedOfficerName = \"Er. R. Kanimozhi\",\r\n                    assignedOfficerEmail = \"officer.kanimozhi@pwd.tn.gov.in\",\r\n                    status = ComplaintStatus.PENDING,\r\n                    priority = ComplaintPriority.NORMAL,\r\n                    lastUpdatedDate = \"Today\",\r\n                    eventHistory = listOf(\r\n                        ComplaintEvent(\"E10\", subTime2, dateFormat.format(Date(subTime2)), \"Submitted\", \"Complaint registered by citizen M. Meenakshi.\", \"M. Meenakshi\")\r\n                    )\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-102','E10',(anchor-interval '164 hours'),'Submitted','Complaint registered by citizen M. Meenakshi.','M. Meenakshi','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E10\", subTime2, dateFormat.format(Date(subTime2)), \"Submitted\", \"Complaint registered by citizen M. Meenakshi.\", \"M. Meenakshi\")"}'::jsonb) on conflict do nothing;
insert into public.complaints(id,category,project_id,project_title_snapshot,submitted_by_citizen_name,location,district,description,submitted_at,assigned_department_id,assigned_department_name,jurisdiction_id,assigned_officer_name,assigned_officer_email,status,priority,is_demo,source_snapshot) values('CMP-2026-103','CONSTRUCTION_QUALITY','TN-PWD-2025-01','Anna Salai Stormwater Drain','P. Selvam','Anna Salai Teynampet Signal, Chennai','Chennai','Deep unpaved trench causing severe traffic congestion.',(anchor-interval '216 hours'),'PWD','Public Works Department','PWD-CHENNAI-OFFICE','Er. R. Kanimozhi','officer.kanimozhi@pwd.tn.gov.in','ESCALATED_OVERDUE','HIGH',true,'{"is_demonstration":true,"kotlin_constructor":"Complaint(\r\n                    id = \"CMP-2026-103\",\r\n                    category = ComplaintCategory.CONSTRUCTION_QUALITY,\r\n                    projectId = \"TN-PWD-2025-01\",\r\n                    projectTitle = \"Anna Salai Stormwater Drain\",\r\n                    location = \"Anna Salai Teynampet Signal, Chennai\",\r\n                    district = \"Chennai\",\r\n                    description = \"Deep unpaved trench causing severe traffic congestion.\",\r\n                    submittedByCitizenName = \"P. Selvam\",\r\n                    submittedDate = dateFormat.format(Date(subTime3)),\r\n                    submissionTimestampMillis = subTime3,\r\n                    deadlineTimestampMillis = deadTime3,\r\n                    assignedDepartment = \"Public Works Department\",\r\n                    assignedOfficerName = \"Er. R. Kanimozhi\",\r\n                    assignedOfficerEmail = \"officer.kanimozhi@pwd.tn.gov.in\",\r\n                    status = ComplaintStatus.ESCALATED_OVERDUE,\r\n                    priority = ComplaintPriority.HIGH,\r\n                    lastUpdatedDate = \"Overdue\",\r\n                    eventHistory = listOf(\r\n                        ComplaintEvent(\"E20\", subTime3, dateFormat.format(Date(subTime3)), \"Submitted\", \"Complaint registered.\", \"P. Selvam\"),\r\n                        ComplaintEvent(\"E21\", deadTime3, dateFormat.format(Date(deadTime3)), \"Escalated — Overdue\", \"7-day official response deadline passed without response. Automatically escalated.\", \"System Escalation Engine\")\r\n                    )\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-103','E20',(anchor-interval '216 hours'),'Submitted','Complaint registered.','P. Selvam','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E20\", subTime3, dateFormat.format(Date(subTime3)), \"Submitted\", \"Complaint registered.\", \"P. Selvam\")"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-103','E21',((anchor-interval '216 hours')+interval '168 hours'),'Escalated — Overdue','7-day official response deadline passed without response. Automatically escalated.','System Escalation Engine','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E21\", deadTime3, dateFormat.format(Date(deadTime3)), \"Escalated — Overdue\", \"7-day official response deadline passed without response. Automatically escalated.\", \"System Escalation Engine\")"}'::jsonb) on conflict do nothing;
insert into public.complaints(id,category,project_id,project_title_snapshot,submitted_by_citizen_name,location,district,description,submitted_at,assigned_department_id,assigned_department_name,jurisdiction_id,assigned_officer_name,assigned_officer_email,status,priority,is_demo,source_snapshot) values('CMP-2026-104','WATER_LEAK',null,null,'K. Raman','Main Street Junction, Salem','Salem','Water pipeline leakage near bus stand.',(anchor-interval '240 hours'),'TWAD','TWAD Board','SALEM','Er. V. Murugan',null,'RESOLVED','NORMAL',true,'{"is_demonstration":true,"kotlin_constructor":"Complaint(\r\n                    id = \"CMP-2026-104\",\r\n                    category = ComplaintCategory.WATER_LEAK,\r\n                    projectId = null,\r\n                    projectTitle = null,\r\n                    location = \"Main Street Junction, Salem\",\r\n                    district = \"Salem\",\r\n                    description = \"Water pipeline leakage near bus stand.\",\r\n                    submittedByCitizenName = \"K. Raman\",\r\n                    submittedDate = dateFormat.format(Date(subTime4)),\r\n                    submissionTimestampMillis = subTime4,\r\n                    deadlineTimestampMillis = deadTime4,\r\n                    assignedDepartment = \"TWAD Board\",\r\n                    assignedOfficerName = \"Er. V. Murugan\",\r\n                    status = ComplaintStatus.RESOLVED,\r\n                    priority = ComplaintPriority.NORMAL,\r\n                    lastUpdatedDate = \"Resolved\",\r\n                    responses = listOf(\r\n                        ComplaintResponse(\"R104\", dateFormat.format(Date(subTime4 + 2 * dayMillis)), \"Er. V. Murugan\", \"Government Officer\", \"Pipe leak repaired and pressure tested.\")\r\n                    ),\r\n                    eventHistory = listOf(\r\n                        ComplaintEvent(\"E30\", subTime4, dateFormat.format(Date(subTime4)), \"Submitted\", \"Complaint registered.\", \"K. Raman\"),\r\n                        ComplaintEvent(\"E31\", subTime4 + (2 * dayMillis), dateFormat.format(Date(subTime4 + 2 * dayMillis)), \"Resolved\", \"Leak repaired before deadline.\", \"Er. V. Murugan\")\r\n                    )\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-104','E30',(anchor-interval '240 hours'),'Submitted','Complaint registered.','K. Raman','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E30\", subTime4, dateFormat.format(Date(subTime4)), \"Submitted\", \"Complaint registered.\", \"K. Raman\")"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-104','E31',(anchor-interval '240 hours')+interval '48 hours','Resolved','Leak repaired before deadline.','Er. V. Murugan','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E31\", subTime4 + (2 * dayMillis), dateFormat.format(Date(subTime4 + 2 * dayMillis)), \"Resolved\", \"Leak repaired before deadline.\", \"Er. V. Murugan\")"}'::jsonb) on conflict do nothing;
insert into public.complaint_responses(complaint_id,id,responded_by_name,role_snapshot,message,source_snapshot) values('CMP-2026-104','R104','Er. V. Murugan','Government Officer','Pipe leak repaired and pressure tested.','{"is_demonstration":true,"kotlin_constructor":"ComplaintResponse(\"R104\", dateFormat.format(Date(subTime4 + 2 * dayMillis)), \"Er. V. Murugan\", \"Government Officer\", \"Pipe leak repaired and pressure tested.\")"}'::jsonb) on conflict do nothing;
insert into public.complaints(id,category,project_id,project_title_snapshot,submitted_by_citizen_name,location,district,description,submitted_at,assigned_department_id,assigned_department_name,jurisdiction_id,assigned_officer_name,assigned_officer_email,status,priority,is_demo,source_snapshot) values('CMP-2026-105','POTHOLE',null,null,'A. Vijay','Vaigai River South Bank Road, Madurai','Madurai','Road potholes near river bridge.',(anchor-interval '288 hours'),'HWY','Highways Department','MADURAI','Er. A. Vijayalakshmi',null,'RESOLVED','HIGH',true,'{"is_demonstration":true,"kotlin_constructor":"Complaint(\r\n                    id = \"CMP-2026-105\",\r\n                    category = ComplaintCategory.POTHOLE,\r\n                    location = \"Vaigai River South Bank Road, Madurai\",\r\n                    district = \"Madurai\",\r\n                    description = \"Road potholes near river bridge.\",\r\n                    submittedByCitizenName = \"A. Vijay\",\r\n                    submittedDate = dateFormat.format(Date(subTime5)),\r\n                    submissionTimestampMillis = subTime5,\r\n                    deadlineTimestampMillis = deadTime5,\r\n                    assignedDepartment = \"Highways Department\",\r\n                    assignedOfficerName = \"Er. A. Vijayalakshmi\",\r\n                    status = ComplaintStatus.RESOLVED,\r\n                    priority = ComplaintPriority.HIGH,\r\n                    lastUpdatedDate = \"Resolved (Late)\",\r\n                    responses = listOf(\r\n                        ComplaintResponse(\"R105\", dateFormat.format(Date(subTime5 + 9 * dayMillis)), \"Er. A. Vijayalakshmi\", \"Government Officer\", \"Road potholes filled with asphalt mix on 9th day.\")\r\n                    ),\r\n                    eventHistory = listOf(\r\n                        ComplaintEvent(\"E40\", subTime5, dateFormat.format(Date(subTime5)), \"Submitted\", \"Complaint registered.\", \"A. Vijay\"),\r\n                        ComplaintEvent(\"E41\", deadTime5, dateFormat.format(Date(deadTime5)), \"Escalated — Overdue\", \"7-day deadline passed without response. Escalated.\", \"System Escalation Engine\"),\r\n                        ComplaintEvent(\"E42\", subTime5 + (9 * dayMillis), dateFormat.format(Date(subTime5 + 9 * dayMillis)), \"Late Response Recorded\", \"Officer responded late. Complaint resolved.\", \"Er. A. Vijayalakshmi\")\r\n                    )\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-105','E40',(anchor-interval '288 hours'),'Submitted','Complaint registered.','A. Vijay','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E40\", subTime5, dateFormat.format(Date(subTime5)), \"Submitted\", \"Complaint registered.\", \"A. Vijay\")"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-105','E41',((anchor-interval '288 hours')+interval '168 hours'),'Escalated — Overdue','7-day deadline passed without response. Escalated.','System Escalation Engine','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E41\", deadTime5, dateFormat.format(Date(deadTime5)), \"Escalated — Overdue\", \"7-day deadline passed without response. Escalated.\", \"System Escalation Engine\")"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-105','E42',(anchor-interval '288 hours')+interval '216 hours','Late Response Recorded','Officer responded late. Complaint resolved.','Er. A. Vijayalakshmi','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E42\", subTime5 + (9 * dayMillis), dateFormat.format(Date(subTime5 + 9 * dayMillis)), \"Late Response Recorded\", \"Officer responded late. Complaint resolved.\", \"Er. A. Vijayalakshmi\")"}'::jsonb) on conflict do nothing;
insert into public.complaint_responses(complaint_id,id,responded_by_name,role_snapshot,message,source_snapshot) values('CMP-2026-105','R105','Er. A. Vijayalakshmi','Government Officer','Road potholes filled with asphalt mix on 9th day.','{"is_demonstration":true,"kotlin_constructor":"ComplaintResponse(\"R105\", dateFormat.format(Date(subTime5 + 9 * dayMillis)), \"Er. A. Vijayalakshmi\", \"Government Officer\", \"Road potholes filled with asphalt mix on 9th day.\")"}'::jsonb) on conflict do nothing;
insert into public.complaints(id,category,project_id,project_title_snapshot,submitted_by_citizen_name,location,district,description,submitted_at,assigned_department_id,assigned_department_name,jurisdiction_id,assigned_officer_name,assigned_officer_email,status,priority,is_demo,source_snapshot) values('CMP-2026-106','OTHER',null,null,'R. Suresh','Chinthamani Circle, Tiruchirappalli','Tiruchirappalli','Unattended construction debris on bridge approach.',(anchor-interval '192 hours'),'HWY','Highways Department','TRICHY',null,null,'ESCALATED_OVERDUE','HIGH',true,'{"is_demonstration":true,"kotlin_constructor":"Complaint(\r\n                    id = \"CMP-2026-106\",\r\n                    category = ComplaintCategory.OTHER,\r\n                    location = \"Chinthamani Circle, Tiruchirappalli\",\r\n                    district = \"Tiruchirappalli\",\r\n                    description = \"Unattended construction debris on bridge approach.\",\r\n                    submittedByCitizenName = \"R. Suresh\",\r\n                    submittedDate = dateFormat.format(Date(subTime6)),\r\n                    submissionTimestampMillis = subTime6,\r\n                    deadlineTimestampMillis = deadTime6,\r\n                    assignedDepartment = \"Highways Department\",\r\n                    assignedOfficerName = null, // Unassigned!\r\n                    status = ComplaintStatus.ESCALATED_OVERDUE,\r\n                    priority = ComplaintPriority.HIGH,\r\n                    lastUpdatedDate = \"Unassigned — Escalation Required\",\r\n                    eventHistory = listOf(\r\n                        ComplaintEvent(\"E50\", subTime6, dateFormat.format(Date(subTime6)), \"Submitted\", \"Complaint registered.\", \"R. Suresh\"),\r\n                        ComplaintEvent(\"E51\", deadTime6, dateFormat.format(Date(deadTime6)), \"Escalated — Overdue\", \"7-day deadline passed without assigned officer response. Flagged for Manager Action.\", \"System Escalation Engine\")\r\n                    )\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-106','E50',(anchor-interval '192 hours'),'Submitted','Complaint registered.','R. Suresh','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E50\", subTime6, dateFormat.format(Date(subTime6)), \"Submitted\", \"Complaint registered.\", \"R. Suresh\")"}'::jsonb) on conflict do nothing;
insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name,source_snapshot) values('CMP-2026-106','E51',((anchor-interval '192 hours')+interval '168 hours'),'Escalated — Overdue','7-day deadline passed without assigned officer response. Flagged for Manager Action.','System Escalation Engine','{"is_demonstration":true,"kotlin_constructor":"ComplaintEvent(\"E51\", deadTime6, dateFormat.format(Date(deadTime6)), \"Escalated — Overdue\", \"7-day deadline passed without assigned officer response. Flagged for Manager Action.\", \"System Escalation Engine\")"}'::jsonb) on conflict do nothing;
insert into public.officer_handovers(id,outgoing_officer_name,outgoing_officer_email,incoming_officer_name,incoming_officer_email,department_id,department_name,office_jurisdiction_id,office_jurisdiction,effective_transfer_date,reason_for_reassignment,legacy_assigned_projects_count,legacy_pending_tasks_count,handover_notes,status,is_demo,source_snapshot) values('HO-2026-88','Er. S. Sundaram','officer.sundaram@pwd.tn.gov.in','Er. R. Kanimozhi','officer.kanimozhi@pwd.tn.gov.in','PWD','Public Works Department','PWD-CHENNAI-OFFICE','Highways & PWD Chennai Metro Division','2025-03-15','Routine Departmental Transfer',2,3,'Project TN-PWD-2025-01 is 68% complete.','ACKNOWLEDGED',true,'{"is_demonstration":true,"kotlin_constructor":"OfficerHandover(\r\n                    id = \"HO-2026-88\",\r\n                    outgoingOfficerName = \"Er. S. Sundaram\",\r\n                    outgoingOfficerEmail = \"officer.sundaram@pwd.tn.gov.in\",\r\n                    incomingOfficerName = \"Er. R. Kanimozhi\",\r\n                    incomingOfficerEmail = \"officer.kanimozhi@pwd.tn.gov.in\",\r\n                    department = \"Public Works Department\",\r\n                    officeJurisdiction = \"Highways & PWD Chennai Metro Division\",\r\n                    effectiveTransferDate = \"15 Mar 2025\",\r\n                    reasonForReassignment = \"Routine Departmental Transfer\",\r\n                    assignedProjectsCount = 2,\r\n                    pendingTasksCount = 3,\r\n                    handoverNotes = \"Project TN-PWD-2025-01 is 68% complete.\",\r\n                    status = HandoverStatus.ACKNOWLEDGED,\r\n                    acknowledgedDate = \"16 Mar 2025\"\r\n                )"}'::jsonb) on conflict do nothing;
insert into public.audit_logs(id,action,performed_by_name,target_type,target_id,details,is_demo,source_snapshot) values('AL-100','Officer Handover Acknowledged','Er. R. Kanimozhi','OfficerHandover','HO-2026-88','Officer handover completed.',true,'{"is_demonstration":true,"kotlin_constructor":"AuditLogEntry(\"AL-100\", \"16 Mar 2025\", \"Officer Handover Acknowledged\", \"Er. R. Kanimozhi\", \"OfficerHandover\", \"HO-2026-88\", \"Officer handover completed.\")"}'::jsonb) on conflict do nothing;
insert into public.audit_logs(id,action,performed_by_name,target_type,target_id,details,is_demo,source_snapshot) values('AL-101','Complaint Resolved','Er. R. Kanimozhi','Complaint','CMP-2026-102','Status changed to Resolved.',true,'{"is_demonstration":true,"kotlin_constructor":"AuditLogEntry(\"AL-101\", \"12 Mar 2025\", \"Complaint Resolved\", \"Er. R. Kanimozhi\", \"Complaint\", \"CMP-2026-102\", \"Status changed to Resolved.\")"}'::jsonb) on conflict do nothing;
insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','audit_discrepancy','AL-101','Audit says CMP-2026-102 resolved while source complaint is pending; preserved without rewriting either.','{"is_demonstration":true,"kotlin_constructor":"AuditLogEntry(\"AL-101\", \"12 Mar 2025\", \"Complaint Resolved\", \"Er. R. Kanimozhi\", \"Complaint\", \"CMP-2026-102\", \"Status changed to Resolved.\")"}'::jsonb) on conflict do nothing;
insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','project_conflict','CONF-TN-101','Source references missing project TN-TWAD-2025-09; no project invented and no FK relaxed.','{"is_demonstration":true,"kotlin_constructor":"ScheduleConflict(\r\n                    id = \"CONF-TN-101\",\r\n                    projectAId = \"TN-PWD-2025-01\",\r\n                    projectATitle = \"Anna Salai Stormwater Drain & Road Resurfacing\",\r\n                    departmentA = \"Public Works Department\",\r\n                    projectBId = \"TN-TWAD-2025-09\",\r\n                    projectBTitle = \"Teynampet High Pressure Water Main Extension\",\r\n                    departmentB = \"TWAD Board\",\r\n                    location = \"Anna Salai Service Road (Teynampet Signal)\",\r\n                    conflictingDates = \"10 May 2025 to 25 May 2025\",\r\n                    conflictType = ConflictType.ROAD_DIGGING_OVERLAP,\r\n                    status = ConflictStatus.UNRESOLVED,\r\n                    proposedResolution = \"Postpone TWAD pipeline trenching until PWD completes culvert linkage or execute joint trench excavation.\"\r\n                )"}'::jsonb) on conflict do nothing;
insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','legacy_user_identity','U1','Historical demo identity preserved without creating fake Auth users or equating it to a test person.','{"is_demonstration":true,"kotlin_constructor":"User(\"U1\", \"R. Anbarasan (Citizen)\", \"citizen@civictech.tn.gov.in\", UserRole.CITIZEN, isDemoAccount = true)"}'::jsonb) on conflict do nothing;
insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','legacy_user_identity','U2','Historical demo identity preserved without creating fake Auth users or equating it to a test person.','{"is_demonstration":true,"kotlin_constructor":"User(\"U2\", \"Er. R. Kanimozhi\", \"officer.kanimozhi@pwd.tn.gov.in\", UserRole.OFFICER, \"Public Works Department\", \"Highways & PWD Chennai Metro Division\", isDemoAccount = true)"}'::jsonb) on conflict do nothing;
insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','legacy_user_identity','U3','Historical demo identity preserved without creating fake Auth users or equating it to a test person.','{"is_demonstration":true,"kotlin_constructor":"User(\"U3\", \"Dr. S. Ramanathan\", \"coord.raman@tn.gov.in\", UserRole.COORDINATOR, \"Inter-Departmental Coordination Cell\", \"Tamil Nadu State Infrastructure Cell\", isDemoAccount = true)"}'::jsonb) on conflict do nothing;
insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','legacy_user_identity','U4','Historical demo identity preserved without creating fake Auth users or equating it to a test person.','{"is_demonstration":true,"kotlin_constructor":"User(\"U4\", \"L&T Infrastructure Admin\", \"contractor.lt@infra.com\", UserRole.CONTRACTOR, \"Contractor Wing\", \"L&T Infrastructure Ltd\", isDemoAccount = true)"}'::jsonb) on conflict do nothing;
insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','legacy_user_identity','U5','Historical demo identity preserved without creating fake Auth users or equating it to a test person.','{"is_demonstration":true,"kotlin_constructor":"User(\"U5\", \"System Admin (Chennai HQ)\", \"admin.civic@tn.gov.in\", UserRole.ADMINISTRATOR, \"Governance Tech Wing\", \"Tamil Nadu E-Governance Agency\", isDemoAccount = true)"}'::jsonb) on conflict do nothing;
insert into civic_private.schema_migrations(version) values('demo_seed_v1');
end $seed$;

-- ===== admin/002_assign_test_roles.sql =====
-- EXPLICIT TEST ACCOUNT MAPPING from the user's dashboard, not name matching.
-- Requires demo_seed_v1. Never rerun this to overwrite subsequent role changes.
-- Citizen: test@gmail.com; Officer: officer@test.com -> PWD Chennai scopes.
-- Coordinator: coordinator@test.com -> listed demo cross-department scopes.
-- Contractor: contractor@test.com -> CON-LT-9081 ONLY. Admin: admin@test.com.
do $$
declare r record; administrator uuid; account uuid;
begin
  if current_user<>'postgres' then raise exception 'Use SQL Editor as postgres'; end if;
  if exists(select 1 from civic_private.schema_migrations where version='test_roles_v1') then return; end if;
  if not exists(select 1 from civic_private.schema_migrations where version='demo_seed_v1') then raise exception 'Install demo seed before its test mappings'; end if;
  select p.id into administrator from public.profiles p join auth.users u on u.id=p.id
    where u.email='admin@test.com' and p.role='ADMINISTRATOR' and p.is_active;
  if administrator is null then raise exception 'Run first Administrator bootstrap first'; end if;
  if (select count(*) from auth.users where email in ('admin@test.com','officer@test.com','coordinator@test.com','contractor@test.com','test@gmail.com')
      and email_confirmed_at is not null and not coalesce(is_anonymous,false))<>5 then
    raise exception 'Five confirmed permanent test Auth accounts are required';
  end if;
  for r in select * from (values
    ('test@gmail.com','CITIZEN','Test Citizen',null,null,null),
    ('officer@test.com','OFFICER','Test Officer','PWD','PWD-CHENNAI-OFFICE',null),
    ('coordinator@test.com','COORDINATOR','Test Coordinator','COORD','COORD-CELL',null),
    ('contractor@test.com','CONTRACTOR','Test Contractor','CONTRACT','LT-OFFICE','CON-LT-9081')
  ) mapping(email,role,name,department,office,contractor) loop
    select p.id into account from public.profiles p join auth.users u on u.id=p.id where u.email=r.email and p.role='CITIZEN' and p.is_active;
    if account is null then raise exception 'Expected initial active CITIZEN profile for %; inspect rather than overwrite',r.email; end if;
    update public.profiles set role=r.role::public.civic_role,name=r.name,department_id=r.department,
      office_jurisdiction_id=r.office,contractor_id=r.contractor,is_demo=true,updated_at=now() where id=account;
    insert into public.audit_logs(event_at,action,performed_by_name,target_type,target_id,details,is_demo)
    values(now(),'TEST_ROLE_ASSIGNED','Supabase SQL Editor (postgres)','PROFILE',account::text,
      'Explicit test mapping: '||r.email||' -> '||r.role,true);
  end loop;
  update public.profiles set is_demo=true where id=administrator;
  insert into public.profile_scopes(profile_id,department_id,jurisdiction_id,granted_by)
  select p.id,'PWD',j,administrator from public.profiles p,unnest(array['PWD-CHENNAI-OFFICE','GCC']) j
  where p.email='officer@test.com' on conflict do nothing;
  insert into public.profile_scopes(profile_id,department_id,jurisdiction_id,granted_by)
  select p.id,d,j,administrator from public.profiles p,
    unnest(array['PWD','TWAD','HWY','MAWS','TANGEDCO']) d,
    unnest(array['GCC','CMC','MADURAI','TRICHY','SALEM','PWD-CHENNAI-OFFICE','TWAD-COIMBATORE-OFFICE']) j
  where p.email='coordinator@test.com' on conflict do nothing;
  insert into public.project_assignments(project_id,officer_id)
  select 'TN-PWD-2025-01',id from public.profiles where email='officer@test.com' on conflict do nothing;
  insert into civic_private.schema_migrations(version) values('test_roles_v1');
end $$;
select email,role,is_active,department_id,office_jurisdiction_id,contractor_id
from public.profiles where email in ('admin@test.com','officer@test.com','coordinator@test.com','contractor@test.com','test@gmail.com') order by role;

-- ===== migrations/005_civic_scheduler.sql =====
-- Backend job runs every minute without a phone or screen visit.
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

insert into civic_private.schema_migrations(version) values('remaining_setup_v1') on conflict do nothing;
notify pgrst,'reload schema';
commit;

-- ===== Read-only cloud verification =====

select email,role,is_active,department_id,office_jurisdiction_id,contractor_id
from public.profiles where email in ('admin@test.com','officer@test.com','coordinator@test.com','contractor@test.com','test@gmail.com') order by role;
select 'projects' as table_name,count(*) as rows from public.projects where is_demo
union all select 'milestones',count(*) from public.project_milestones where project_id in ('TN-PWD-2025-01','TN-TWAD-2025-02')
union all select 'complaints',count(*) from public.complaints where is_demo
union all select 'legacy handovers',count(*) from public.officer_handovers where is_demo
union all select 'preserved import issues',count(*) from civic_private.import_rejections where source_dataset='android_demo_v1';
select entity_type,source_id,reason from civic_private.import_rejections where source_dataset='android_demo_v1' order by entity_type,source_id;

-- Read-only configuration checks. Auth login, actual uploads and Cron execution
-- must also be exercised against the real cloud services before readiness.
select check_name,passed from (
  select '01 schema migration installed' as check_name,
    exists(select 1 from civic_private.schema_migrations where version='001_civic_schema') as passed
  union all select '02 all 21 application tables keep RLS',
    (select count(*)=21 and bool_and(relrowsecurity) from pg_class where relnamespace='public'::regnamespace and relkind='r')
  union all select '03 no direct application table writes for anon/authenticated',
    not exists(select 1 from pg_class c where c.relnamespace='public'::regnamespace and c.relkind='r'
      and (has_table_privilege('anon',c.oid,'INSERT,UPDATE,DELETE') or has_table_privilege('authenticated',c.oid,'INSERT,UPDATE,DELETE')
        or has_any_column_privilege('anon',c.oid,'INSERT,UPDATE') or has_any_column_privilege('authenticated',c.oid,'INSERT,UPDATE')))
  union all select '04 source snapshots are inaccessible to clients',
    not exists(select 1 from information_schema.columns c where c.table_schema='public' and c.column_name='source_snapshot'
      and (has_column_privilege('anon',format('public.%I',c.table_name),c.column_name,'SELECT')
        or has_column_privilege('authenticated',format('public.%I',c.table_name),c.column_name,'SELECT')))
  union all select '05 safe read views use invoker security',
    (select count(*)=21 and bool_and(reloptions @> array['security_invoker=true']) from pg_class
      where relnamespace='public'::regnamespace and relkind='v' and relname like 'civic_%')
  union all select '06 all 23 authorized RPCs deny unauthenticated execution',
    (select count(*)=23 and bool_and(not has_function_privilege('anon',p.oid,'EXECUTE'))
      and bool_and(p.prosecdef and p.proconfig @> array['search_path=""'])
     from pg_proc p where p.pronamespace='public'::regnamespace and p.proname like 'civic_%')
  union all select '07 all five expected test roles are mapped',
    (select count(*)=5 from public.profiles p join (values
      ('admin@test.com','ADMINISTRATOR'),('officer@test.com','OFFICER'),('coordinator@test.com','COORDINATOR'),
      ('contractor@test.com','CONTRACTOR'),('test@gmail.com','CITIZEN')) expected(email,role)
      on p.email=expected.email and p.role::text=expected.role and p.is_active)
  union all select '08 contractor association is explicit',
    exists(select 1 from public.profiles where email='contractor@test.com' and role='CONTRACTOR' and contractor_id='CON-LT-9081')
  union all select '09 test Officer has approved PWD office scope',
    exists(select 1 from public.profile_scopes s join public.profiles p on p.id=s.profile_id
      where p.email='officer@test.com' and s.department_id='PWD' and s.jurisdiction_id='PWD-CHENNAI-OFFICE')
  union all select '10 demo projects preserved and explicitly labeled',
    (select count(*)=2 and bool_and(is_demo) from public.projects where id in ('TN-PWD-2025-01','TN-TWAD-2025-02'))
  union all select '11 unresolved sample conflict retained privately',
    exists(select 1 from civic_private.import_rejections where source_id='CONF-TN-101' and entity_type='project_conflict')
  union all select '12 three buckets are private and size-limited',
    (select count(*)=3 and bool_and(not public and file_size_limit=10485760) from storage.buckets
      where id in ('complaint-photos','progress-evidence','deadline-documents'))
  union all select '13 Storage restrictive guards installed',
    (select count(*)=4 and bool_and(not polpermissive) from pg_policy where polrelid='storage.objects'::regclass
      and polname in ('civic_storage_read_guard','civic_storage_insert_guard','civic_storage_update_guard','civic_storage_delete_guard'))
  union all select '14 complaint deadlines remain exactly 168 hours',
    not exists(select 1 from public.complaints where extract(epoch from deadline_at-submitted_at)<>604800)
  union all select '15 minute-by-minute backend Cron job installed',civic_private.scheduler_ready()
) checks order by check_name;
