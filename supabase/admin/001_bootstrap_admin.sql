-- One-time trusted bootstrap, not an application RPC or an Auth metadata edit.
-- Run the ENTIRE file in Supabase SQL Editor with execution role postgres.
-- admin@test.com is the intended test Administrator shown in the user's dashboard.
-- For another installation, change this email only to a verified account you control.
begin;
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
commit;

-- Expected: admin@test.com / ADMINISTRATOR / true.
select email, role, is_active from public.profiles where email = 'admin@test.com';
