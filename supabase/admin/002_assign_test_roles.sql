-- EXPLICIT TEST ACCOUNT MAPPING from the user's dashboard, not name matching.
-- Requires demo_seed_v1. Never rerun this to overwrite subsequent role changes.
-- Citizen: test@gmail.com; Officer: officer@test.com -> PWD Chennai scopes.
-- Coordinator: coordinator@test.com -> listed demo cross-department scopes.
-- Contractor: contractor@test.com -> CON-LT-9081 ONLY. Admin: admin@test.com.
begin;
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
commit;
select email,role,is_active,department_id,office_jurisdiction_id,contractor_id
from public.profiles where email in ('admin@test.com','officer@test.com','coordinator@test.com','contractor@test.com','test@gmail.com') order by role;
