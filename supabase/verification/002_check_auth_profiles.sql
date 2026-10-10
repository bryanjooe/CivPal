-- Read-only. Run in SQL Editor as postgres after creating test Auth users.
-- Keep account emails/UUIDs locally; passwords are never queried.
-- At this step each new test account should have CITIZEN as its role.
select
  u.id as auth_user_id,
  u.email,
  (u.email_confirmed_at is not null) as email_confirmed,
  (p.id is not null) as profile_exists,
  p.role,
  p.is_active,
  (p.email is not distinct from u.email) as email_matches
from auth.users u
left join public.profiles p on p.id = u.id
order by u.created_at, u.id;

-- Expect zero missing profiles and zero mismatched emails.
select
  count(*) as auth_accounts,
  count(*) filter (where p.id is null) as missing_profiles,
  count(*) filter (where p.id is not null and p.email is distinct from u.email)
    as mismatched_emails
from auth.users u
left join public.profiles p on p.id = u.id;
