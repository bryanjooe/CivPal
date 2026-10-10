-- CivicTech Stage 1 / Step 1: schema only, with API access CLOSED by default.
-- Run once in a NEW project using Supabase SQL Editor as postgres.
-- No demo data, passwords, Auth accounts, or Android changes are included.
-- Later migrations grant scoped reads and authorized transactional RPC access.
begin;

-- Fail instead of silently merging into or overwriting an existing schema.
do $$
begin
  if to_regclass('public.projects') is not null
     or to_regclass('public.profiles') is not null
     or to_regnamespace('civic_private') is not null then
    raise exception 'CivicTech schema already exists or conflicts with existing tables. Do not rerun or overwrite it; inspect the setup verification first.';
  end if;
end;
$$;

create schema civic_private;
revoke all on schema civic_private from public, anon, authenticated;
create table civic_private.schema_migrations (
  version text primary key,
  applied_at timestamptz not null default now()
);
create table civic_private.id_counters (
  prefix text not null,
  department_code text not null,
  id_year integer not null check (id_year between 1900 and 9999),
  last_issued bigint not null check (last_issued >= 0),
  primary key (prefix, department_code, id_year)
);
-- Source rows with unresolved FKs stay here, never in public/API tables.
create table civic_private.import_rejections (
  source_dataset text not null,
  entity_type text not null,
  source_id text not null,
  reason text not null,
  source_record jsonb not null,
  recorded_at timestamptz not null default now(),
  primary key (source_dataset, entity_type, source_id)
);

create type public.civic_role as enum ('CITIZEN','OFFICER','COORDINATOR','CONTRACTOR','ADMINISTRATOR');
create type public.civic_project_status as enum ('PLANNED','ONGOING','DELAYED','COMPLETED');
create type public.civic_complaint_category as enum ('POTHOLE','WATER_LEAK','DRAINAGE_OVERFLOW','DELAY_QUERY','CONSTRUCTION_QUALITY','OTHER');
create type public.civic_complaint_status as enum ('PENDING','UNDER_REVIEW','ACTION_PROPOSED','RESOLVED','REJECTED','ESCALATED_OVERDUE','AWAITING_CITIZEN_INFO');
create type public.civic_complaint_priority as enum ('NORMAL','HIGH');
create type public.civic_progress_status as enum ('Pending Verification','Approved','Rejected','Correction Requested');
create type public.civic_conflict_type as enum ('ROAD_DIGGING_OVERLAP','TRAFFIC_BLOCKADE','UTILITY_INTERFERENCE','RESOURCE_CONTENTION');
create type public.civic_conflict_status as enum ('UNRESOLVED','UNDER_DISCUSSION','RESOLVED');
create type public.civic_handover_status as enum ('INITIATED','ACKNOWLEDGED','CLARIFICATION_REQUESTED');

create table public.departments (
  id text primary key check (length(btrim(id)) between 1 and 50),
  code text not null unique check (code ~ '^[A-Z0-9]{2,12}$'),
  name text not null unique check (length(btrim(name)) between 1 and 200),
  scope_description text,
  source_labels text[] not null default '{}',
  is_active boolean not null default true,
  is_demo boolean not null default false,
  created_at timestamptz not null default now()
);

create table public.jurisdictions (
  id text primary key check (length(btrim(id)) between 1 and 100),
  name text not null check (length(btrim(name)) between 1 and 200),
  kind text not null check (kind in ('STATE','DISTRICT','MUNICIPALITY','OFFICE','ZONE')),
  parent_id text references public.jurisdictions(id) on delete restrict,
  district text,
  details text,
  is_active boolean not null default true,
  is_demo boolean not null default false,
  created_at timestamptz not null default now(),
  unique (name, kind),
  check (parent_id is distinct from id)
);
create index jurisdictions_parent_idx on public.jurisdictions(parent_id);

create table public.contractors (
  id text primary key check (length(btrim(id)) between 1 and 100),
  name text not null check (length(btrim(name)) between 1 and 200),
  is_active boolean not null default true,
  is_demo boolean not null default false,
  created_at timestamptz not null default now()
);

create table public.profiles (
  id uuid primary key references auth.users(id) on delete restrict,
  legacy_user_id text unique,
  name text not null check (length(btrim(name)) between 1 and 200),
  email text,
  role public.civic_role not null default 'CITIZEN',
  department_id text references public.departments(id) on delete restrict,
  office_jurisdiction_id text references public.jurisdictions(id) on delete restrict,
  contractor_id text references public.contractors(id) on delete restrict,
  is_active boolean not null default true,
  is_demo boolean not null default false,
  preferred_language text not null default 'ENGLISH' check (preferred_language in ('ENGLISH','TAMIL')),
  notifications_enabled boolean not null default true,
  source_snapshot jsonb not null default '{}',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (contractor_id is null or role = 'CONTRACTOR')
);
create index profiles_department_idx on public.profiles(department_id);
create index profiles_jurisdiction_idx on public.profiles(office_jurisdiction_id);
create index profiles_contractor_idx on public.profiles(contractor_id);

-- Explicit administrator-approved scopes, never derived from UI-selected text.
-- Multiple department rows permit authorized cross-department coordination.
create table public.profile_scopes (
  profile_id uuid not null references public.profiles(id) on delete restrict,
  department_id text not null references public.departments(id) on delete restrict,
  jurisdiction_id text not null references public.jurisdictions(id) on delete restrict,
  granted_by uuid references public.profiles(id) on delete restrict,
  created_at timestamptz not null default now(),
  primary key (profile_id, department_id, jurisdiction_id)
);
create index profile_scopes_department_idx on public.profile_scopes(department_id, jurisdiction_id);
create index profile_scopes_jurisdiction_idx on public.profile_scopes(jurisdiction_id);

create table public.projects (
  -- Permanent public ID is the actual PK, NOT replaced by a UUID.
  id text primary key check (length(btrim(id)) between 1 and 150 and id = btrim(id)),
  title text not null check (length(btrim(title)) between 1 and 500),
  description text not null,
  department_id text not null references public.departments(id) on delete restrict,
  department_name text not null,
  district text not null,
  municipal_body_id text not null references public.jurisdictions(id) on delete restrict,
  municipal_body text not null,
  location text not null,
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  contractor_id text not null references public.contractors(id) on delete restrict,
  contractor_name text not null,
  responsible_office_id text not null references public.jurisdictions(id) on delete restrict,
  responsible_office text not null,
  current_officer_in_charge text not null,
  estimated_budget_in_lakhs numeric(18,2) not null check (estimated_budget_in_lakhs >= 0),
  funding_source text not null,
  start_date date not null,
  original_completion_date date not null,
  revised_completion_date date not null,
  status public.civic_project_status not null default 'PLANNED',
  progress_percentage smallint not null default 0 check (progress_percentage between 0 and 100),
  is_published boolean not null default false,
  is_demo boolean not null default false,
  source_snapshot jsonb not null default '{}',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (original_completion_date >= start_date),
  check (revised_completion_date >= start_date)
);
create index projects_scope_idx on public.projects(department_id, responsible_office_id);
create index projects_municipality_idx on public.projects(municipal_body_id);
create index projects_contractor_idx on public.projects(contractor_id);
create index projects_public_status_idx on public.projects(status) where is_published;

-- Internal identity/assignment information is separate from public project fields.
create table public.project_assignments (
  project_id text primary key references public.projects(id) on delete restrict,
  officer_id uuid references public.profiles(id) on delete restrict,
  updated_at timestamptz not null default now()
);
create index project_assignments_officer_idx on public.project_assignments(officer_id);

create table public.project_milestones (
  project_id text not null references public.projects(id) on delete restrict,
  milestone_id text not null check (length(btrim(milestone_id)) between 1 and 100),
  title text not null,
  target_date date not null,
  is_completed boolean not null default false,
  completion_date date,
  source_snapshot jsonb not null default '{}',
  primary key (project_id, milestone_id),
  check (is_completed or completion_date is null)
);

create table public.progress_reports (
  id text primary key default ('PR-' || gen_random_uuid()::text),
  project_id text not null,
  milestone_id text not null,
  submitted_by uuid references public.profiles(id) on delete restrict,
  submitted_by_name text not null,
  submitted_at timestamptz,
  completion_percentage smallint not null check (completion_percentage between 0 and 100),
  work_completed text not null,
  remaining_work text not null,
  obstacles text,
  photo_path text,
  status public.civic_progress_status not null default 'Pending Verification',
  is_public boolean not null default false,
  source_snapshot jsonb not null default '{}',
  created_at timestamptz not null default now(),
  foreign key (project_id, milestone_id) references public.project_milestones(project_id, milestone_id) on delete restrict,
  check (not is_public or status = 'Approved')
);
create index progress_reports_milestone_idx on public.progress_reports(project_id, milestone_id);
create index progress_reports_submitter_idx on public.progress_reports(submitted_by);
create index progress_reports_pending_idx on public.progress_reports(project_id) where status = 'Pending Verification';

-- Never grant public read of officer notes, including on approved reports.
create table public.progress_report_reviews (
  report_id text primary key references public.progress_reports(id) on delete restrict,
  reviewed_by uuid references public.profiles(id) on delete restrict,
  reviewer_name text not null,
  officer_notes text,
  decision public.civic_progress_status not null check (decision <> 'Pending Verification'),
  reviewed_at timestamptz,
  source_snapshot jsonb not null default '{}'
);
create index progress_report_reviews_actor_idx on public.progress_report_reviews(reviewed_by);

create table public.delay_records (
  id text primary key default ('DR-' || gen_random_uuid()::text),
  project_id text not null references public.projects(id) on delete restrict,
  recorded_by uuid references public.profiles(id) on delete restrict,
  recorded_by_name text not null,
  recorded_at timestamptz,
  reason text not null,
  original_completion_date date not null,
  revised_completion_date date not null,
  published_explanation text not null,
  is_published boolean not null default false,
  source_snapshot jsonb not null default '{}',
  created_at timestamptz not null default now()
);
create index delay_records_project_idx on public.delay_records(project_id);
create index delay_records_actor_idx on public.delay_records(recorded_by);

create table public.complaints (
  id text primary key default ('CMP-' || gen_random_uuid()::text),
  category public.civic_complaint_category not null,
  project_id text references public.projects(id) on delete restrict,
  project_title_snapshot text,
  citizen_id uuid references public.profiles(id) on delete restrict,
  submitted_by_citizen_name text not null,
  location text not null,
  district text not null,
  description text not null,
  photo_path text,
  submitted_at timestamptz not null default now(),
  -- Exactly 168 elapsed hours, matching the current Kotlin workflow.
  deadline_at timestamptz not null,
  assigned_department_id text not null references public.departments(id) on delete restrict,
  assigned_department_name text not null,
  jurisdiction_id text not null references public.jurisdictions(id) on delete restrict,
  assigned_officer_id uuid references public.profiles(id) on delete restrict,
  assigned_officer_name text,
  assigned_officer_email text,
  status public.civic_complaint_status not null default 'PENDING',
  priority public.civic_complaint_priority not null default 'NORMAL',
  is_demo boolean not null default false,
  source_snapshot jsonb not null default '{}',
  updated_at timestamptz not null default now()
);
create index complaints_citizen_idx on public.complaints(citizen_id, submitted_at desc);
create index complaints_project_idx on public.complaints(project_id);
create index complaints_scope_idx on public.complaints(assigned_department_id, jurisdiction_id);
create index complaints_officer_idx on public.complaints(assigned_officer_id);
create index complaints_jurisdiction_idx on public.complaints(jurisdiction_id);
create index complaints_deadline_idx on public.complaints(deadline_at) where status in ('PENDING','UNDER_REVIEW','AWAITING_CITIZEN_INFO');

create table public.complaint_responses (
  complaint_id text not null references public.complaints(id) on delete restrict,
  id text not null default ('R-' || gen_random_uuid()::text),
  responded_by uuid references public.profiles(id) on delete restrict,
  responded_by_name text not null,
  role_snapshot text not null,
  message text not null,
  responded_at timestamptz,
  source_snapshot jsonb not null default '{}',
  primary key (complaint_id, id)
);
create index complaint_responses_actor_idx on public.complaint_responses(responded_by);

create table public.complaint_events (
  complaint_id text not null references public.complaints(id) on delete restrict,
  id text not null default ('EVT-' || gen_random_uuid()::text),
  event_at timestamptz not null default now(),
  event_type text not null,
  description text not null,
  performed_by uuid references public.profiles(id) on delete restrict,
  performed_by_name text not null,
  source_snapshot jsonb not null default '{}',
  primary key (complaint_id, id)
);
create index complaint_events_time_idx on public.complaint_events(complaint_id, event_at);
create index complaint_events_actor_idx on public.complaint_events(performed_by);
create unique index complaint_events_single_escalation_idx on public.complaint_events(complaint_id)
  where event_type = 'Escalated — Overdue';

create table public.project_conflicts (
  id text primary key default ('CONF-TN-' || gen_random_uuid()::text),
  project_a_id text not null references public.projects(id) on delete restrict,
  project_b_id text not null references public.projects(id) on delete restrict,
  project_a_title_snapshot text,
  project_b_title_snapshot text,
  department_a_snapshot text,
  department_b_snapshot text,
  location text not null,
  conflicting_dates text not null,
  conflict_type public.civic_conflict_type not null,
  status public.civic_conflict_status not null default 'UNRESOLVED',
  proposed_resolution text,
  resolved_at timestamptz,
  resolved_by uuid references public.profiles(id) on delete restrict,
  is_demo boolean not null default false,
  source_snapshot jsonb not null default '{}',
  created_at timestamptz not null default now(),
  check (project_a_id <> project_b_id)
);
create index project_conflicts_a_idx on public.project_conflicts(project_a_id);
create index project_conflicts_b_idx on public.project_conflicts(project_b_id);
create index project_conflicts_actor_idx on public.project_conflicts(resolved_by);

create table public.officer_handovers (
  id text primary key default ('HO-' || gen_random_uuid()::text),
  outgoing_officer_id uuid references public.profiles(id) on delete restrict,
  outgoing_officer_name text not null,
  outgoing_officer_email text not null,
  incoming_officer_id uuid references public.profiles(id) on delete restrict,
  incoming_officer_name text not null,
  incoming_officer_email text not null,
  department_id text not null references public.departments(id) on delete restrict,
  department_name text not null,
  office_jurisdiction_id text not null references public.jurisdictions(id) on delete restrict,
  office_jurisdiction text not null,
  effective_transfer_date date not null,
  reason_for_reassignment text not null,
  -- Actual current counts derive from join/child tables, not these source claims.
  legacy_assigned_projects_count integer check (legacy_assigned_projects_count >= 0),
  legacy_pending_tasks_count integer check (legacy_pending_tasks_count >= 0),
  handover_notes text not null,
  status public.civic_handover_status not null default 'INITIATED',
  acknowledged_at timestamptz,
  clarification_notes text,
  is_demo boolean not null default false,
  source_snapshot jsonb not null default '{}',
  created_at timestamptz not null default now(),
  check (outgoing_officer_id is null or incoming_officer_id is null or outgoing_officer_id <> incoming_officer_id)
);
create index officer_handovers_outgoing_idx on public.officer_handovers(outgoing_officer_id);
create index officer_handovers_incoming_idx on public.officer_handovers(incoming_officer_id);
create index officer_handovers_scope_idx on public.officer_handovers(department_id, office_jurisdiction_id);
create index officer_handovers_jurisdiction_idx on public.officer_handovers(office_jurisdiction_id);

create table public.handover_projects (
  handover_id text not null references public.officer_handovers(id) on delete restrict,
  project_id text not null references public.projects(id) on delete restrict,
  primary key (handover_id, project_id)
);
create index handover_projects_project_idx on public.handover_projects(project_id);

create table public.handover_pending_tasks (
  handover_id text not null,
  id text not null,
  related_project_id text not null,
  title text not null,
  priority text not null check (priority in ('High','Medium','Low')),
  due_date date not null,
  source_snapshot jsonb not null default '{}',
  primary key (handover_id, id),
  foreign key (handover_id, related_project_id) references public.handover_projects(handover_id, project_id) on delete restrict
);
create index handover_pending_tasks_project_idx on public.handover_pending_tasks(related_project_id);

create table public.project_follows (
  user_id uuid not null references public.profiles(id) on delete restrict,
  project_id text not null references public.projects(id) on delete restrict,
  created_at timestamptz not null default now(),
  primary key (user_id, project_id)
);
create index project_follows_project_idx on public.project_follows(project_id);

create table public.deadline_revision_requests (
  id text primary key default ('DREV-' || gen_random_uuid()::text),
  project_id text not null references public.projects(id) on delete restrict,
  submitted_by uuid not null references public.profiles(id) on delete restrict,
  current_deadline_snapshot date not null,
  proposed_completion_date date not null,
  reason text not null,
  document_path text,
  status text not null default 'PENDING' check (status in ('PENDING','APPROVED','REJECTED')),
  reviewed_by uuid references public.profiles(id) on delete restrict,
  reviewer_notes text,
  submitted_at timestamptz not null default now(),
  reviewed_at timestamptz,
  check (proposed_completion_date > current_deadline_snapshot)
);
create index deadline_revision_requests_project_idx on public.deadline_revision_requests(project_id);
create index deadline_revision_requests_submitter_idx on public.deadline_revision_requests(submitted_by);
create index deadline_revision_requests_reviewer_idx on public.deadline_revision_requests(reviewed_by);

create table public.audit_logs (
  id text primary key default ('AL-' || gen_random_uuid()::text),
  event_at timestamptz,
  action text not null,
  performed_by uuid references public.profiles(id) on delete restrict,
  performed_by_name text not null,
  target_type text not null,
  target_id text not null,
  details text not null,
  is_demo boolean not null default false,
  source_snapshot jsonb not null default '{}',
  recorded_at timestamptz not null default now()
);
create index audit_logs_target_idx on public.audit_logs(target_type, target_id, recorded_at desc);
create index audit_logs_actor_idx on public.audit_logs(performed_by);

create function civic_private.prevent_project_id_change()
returns trigger language plpgsql set search_path = '' as $$
begin
  if new.id is distinct from old.id then
    raise exception 'Permanent Project ID cannot be changed' using errcode = '23514';
  end if;
  return new;
end;
$$;
create trigger civic_project_id_immutable before update on public.projects
  for each row execute function civic_private.prevent_project_id_change();

-- PostgreSQL marks timestamptz + interval as STABLE, so a stored column plus
-- a mandatory trigger is used instead of an invalid generated-column expression.
create function civic_private.set_complaint_deadline()
returns trigger language plpgsql set search_path = '' as $$
begin
  new.deadline_at := new.submitted_at + interval '168 hours';
  return new;
end;
$$;
create trigger civic_complaint_deadline before insert or update on public.complaints
  for each row execute function civic_private.set_complaint_deadline();

-- Never copy application role or scope from editable Auth user_metadata.
create function civic_private.sync_auth_profile()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if tg_op = 'INSERT' then
    insert into public.profiles(id, name, email)
    values (new.id,
      coalesce(nullif(left(btrim(new.raw_user_meta_data ->> 'name'),200),''),
        nullif(left(split_part(new.email,'@',1),200),''), 'Guest Citizen'), new.email);
  else
    update public.profiles set email = new.email, updated_at = now() where id = new.id;
  end if;
  return new;
end;
$$;
create trigger civic_auth_profile_created after insert on auth.users
  for each row execute function civic_private.sync_auth_profile();
create trigger civic_auth_profile_email_changed after update of email on auth.users
  for each row execute function civic_private.sync_auth_profile();
-- Existing Auth users (if any) get CITIZEN profiles, never elevated roles.
insert into public.profiles(id,name,email)
select id, coalesce(nullif(left(btrim(raw_user_meta_data ->> 'name'),200),''),
  nullif(left(split_part(email,'@',1),200),''),'Guest Citizen'), email
from auth.users;

-- Secure every exposed table immediately, before publishing any read/write API.
-- Zero policies in this migration = default-deny. Subsequent migrations add
-- explicit read policies and backend-authorized workflow functions.
do $$
declare table_name text;
begin
  foreach table_name in array array[
    'departments','jurisdictions','contractors','profiles','profile_scopes',
    'projects','project_assignments','project_milestones','progress_reports',
    'progress_report_reviews','delay_records','complaints','complaint_responses',
    'complaint_events','project_conflicts','officer_handovers','handover_projects',
    'handover_pending_tasks','project_follows','deadline_revision_requests','audit_logs'
  ] loop
    execute format('alter table public.%I enable row level security',table_name);
    execute format('revoke all on table public.%I from public, anon, authenticated',table_name);
    -- A trusted service backend may need maintenance; never put its key in Android.
    execute format('grant all on table public.%I to service_role',table_name);
  end loop;
end;
$$;
revoke all on all tables in schema civic_private from public, anon, authenticated;
revoke all on all functions in schema civic_private from public, anon, authenticated;

insert into civic_private.schema_migrations(version) values ('001_civic_schema');
commit;

-- A successful execution returns 001_civic_schema.
select version, applied_at from civic_private.schema_migrations order by version;
