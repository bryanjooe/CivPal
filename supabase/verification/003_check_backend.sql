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

select email,role,is_active,department_id,office_jurisdiction_id,contractor_id
from public.profiles where email in ('admin@test.com','officer@test.com','coordinator@test.com','contractor@test.com','test@gmail.com') order by role;
select 'projects' as table_name,count(*) as rows from public.projects where is_demo
union all select 'milestones',count(*) from public.project_milestones where project_id in ('TN-PWD-2025-01','TN-TWAD-2025-02')
union all select 'complaints',count(*) from public.complaints where is_demo
union all select 'legacy handovers',count(*) from public.officer_handovers where is_demo
union all select 'preserved import issues',count(*) from civic_private.import_rejections where source_dataset='android_demo_v1';
select entity_type,source_id,reason from civic_private.import_rejections where source_dataset='android_demo_v1' order by entity_type,source_id;
