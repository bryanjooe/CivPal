# CivicTech Supabase setup — interactive progress

Stage 1 only. No Android application, dependency, navigation, login, or QR files are changed.

## Completed SQL step: remaining bundle (user-reported result)

The user replied "yea it did give it" after being asked whether the combined file returned all 15 checks passed. This is treated as user-reported success, subject to correction; the actual result grid has not been supplied. Live service verification remains pending.

Current action: use the project's **Connect** dialog to find its Project URL and Publishable Key (`sb_publishable_...`). These public connection details may be shared for read-only API checks. Never share passwords, Secret Keys or service_role keys. Settings > API Keys is the alternative location for the publishable key. No Android integration is authorized yet.

## Live API checks after receiving connection details

Project URL: `https://wbczfllhlxyvziakznuy.supabase.co`. Publishable Key was supplied by the user in the dashboard's generic Kotlin Todo example and used only for read-only API checks; it is not added to Android code.

Verified on 2026-10-10:

- GET `/rest/v1/civic_projects?select=id,title,is_demo&order=id`: HTTP 200, the two exact permanent source Project IDs, both marked demo.
- GET `/rest/v1/profiles?select=id,role&limit=1`: HTTP 401 with PostgreSQL 42501 permission denied, confirming signed-out profile protection. Do not follow the generic error hint to grant anon access; denial is expected.
- GET `/auth/v1/settings`: HTTP 200. Email provider enabled; signup enabled; email confirmation required. Anonymous Auth sign-ins are disabled.

These checks confirm public API connectivity and this specific private-profile denial, not all role permissions or upload/cron behavior. Next prerequisite is configuring guest Auth abuse protection (CAPTCHA) before enabling anonymous identities. The generic `todos`/TodoList/MainActivity snippet should not be added to this CivicTech application.

## Superseded step: configure CAPTCHA protection

User confirmed CAPTCHA is currently OFF in the dashboard. Guest Auth remains disabled according to the live settings check.

1. Create/sign in to an hCaptcha account at https://dashboard.hcaptcha.com/signup.
2. Keep the public Sitekey from **Sites** (create a CivicTech site if needed) for the later client integration; obtain the private Secret Key from **Settings**.
3. In Supabase open **Authentication > Attack Protection** (some versions label this Bot and Abuse Protection).
4. Enable **CAPTCHA protection**, select **hCaptcha**, enter the hCaptcha Secret Key directly into the dashboard's Secret Key field and click **Save**. Never paste this secret into chat or Android files. The Sitekey is not the server secret.
5. Reply Done after saving. Confirm provider setup before enabling guest Auth. The app-side CAPTCHA/token flow belongs to Stage 2; it has not been implemented. CAPTCHA configuration alone does not prove a real challenge/sign-in succeeds.

References: https://supabase.com/docs/guides/auth/auth-captcha and https://docs.hcaptcha.com/.

## Current major step: enable guest Auth without CAPTCHA (user preference)

The user explicitly declined CAPTCHA and requested continuing without it. That preference supersedes the previous CAPTCHA prerequisite. Keep CAPTCHA OFF; do not require an hCaptcha account. Leave Supabase Auth rate limits enabled.

The live Auth settings were rechecked after the user's continue request: anonymous_users remains false. Open **Authentication > Sign In / Providers**, turn **Allow anonymous sign-ins** ON, click **Save**, then reply Done. No guest identity is created by the settings check, and no cloud setting has been changed by the assistant.

Independent preparation: `verification/live_login_check.html` is a standalone browser tool for public API checks and the five password logins/backend roles. Passwords are entered locally, sent only to this project's Supabase Auth endpoint, cleared from the input, and never saved in results. Sessions are held in memory and signed out after each account check. This does not modify Android source or government records. Its five login checks remain unrun; use it after the current dashboard step. Actual uploads and Cron execution remain pending.

The user changed the step-by-step delivery request to a combined SQL delivery. Open `ALL_REMAINING_SETUP.sql`, copy the entire file into **SQL Editor > New query**, keep execution role **postgres**, and run it. This assumes schema 001 is already installed and the five confirmed accounts already exist. Do not rerun schema 001.

The combined file includes the first-admin bootstrap (safe if already done), scoped reads and safe views, 23 authorized transactional RPCs, private Storage buckets/policies, the labeled Android demo seed, all five test-role mappings and backend overdue scheduling. It runs setup as one transaction and finishes with 15 configuration checks. Expect every `passed` value true. If check 15 is false, enable Cron under **Integrations > Cron**, rerun `migrations/005_civic_scheduler.sql`, then run `verification/003_check_backend.sql`.

Local verification: **43 backend PostgreSQL checks + 3 actual combined-file checks passed**. The latter verify complete rollback after a late error and that reruns preserve later account/data changes. Real cloud execution, Auth/Storage services and cron-worker execution are not verified by local tests. See `BACKEND_REFERENCE.md` for the permission matrix, workflow contracts, demo/import summary, dashboard-only Auth work and readiness checklist.

Reply Done with the 15 check results or send the exact SQL error. Guest Auth/abuse protection, email delivery/recovery, actual role logins, actual uploads and cron job history remain separate live checks. No Android code has changed.

## Confirmed so far

- Existing Android data layer and screen operations inspected; see PROJECT_INSPECTION.md.
- User signed in to Supabase.
- User confirmed cloud project creation and provisioning. Project name/region are not yet recorded.
- Schema migration prepared; 13 local PostgreSQL checks passed.
- User replied Done to the initial schema installation/verification step. Detailed cloud query results have not been supplied; this is user-reported completion, not independent verification.
- User confirmed Email provider ON, Confirm email ON and new-user signup ON. Sign-in and email delivery have not yet been tested.
- Five Auth accounts are visible in the user's screenshot: admin@test.com, contractor@test.com, coordinator@test.com, officer@test.com, test@gmail.com. User subsequently confirmed the profile verification query succeeded; detailed query output was not supplied.
- Remaining combined SQL execution and expected checks were reported successful by the user; actual output and live service tests are pending.

## Completed step: install the protected schema (user confirmed)

Use the newly created CivicTech project, not a project containing unrelated production data.

1. Open that project's Dashboard, then **SQL Editor** in the left sidebar.
2. Click **New query** (or the **+** button for a new query).
3. Open `migrations/001_civic_schema.sql` locally and copy the ENTIRE file, including BEGIN/COMMIT and the final SELECT.
4. Paste it into the SQL Editor. Keep the execution role as **postgres** if the editor shows a role selector.
5. Click **Run** (Ctrl+Enter also works).
6. Expected final result: a row with version `001_civic_schema` and an `applied_at` timestamp. The transaction creates all tables together or rolls back if any statement fails.
7. Open another new query. Copy/paste the entire `verification/001_check_schema.sql` and click **Run**.
8. Expected verification: 21 table rows, each with `table_exists = true`, `rls_enabled = true`, and `api_access_closed = true`. Other result sets list the migration, primary/foreign keys, and four enabled triggers.

Stop here and reply **Done** with those results. If anything fails, send the exact error text and any line number. No database password or secret/service-role key is needed. If a dashboard warning requests confirmation for the REVOKE statements, these intentionally close access to newly created CivicTech tables; the script does not delete existing records. It fails safely if CivicTech tables already exist.

The migration is intended to run once. Do not rerun it after success: the preflight rejects an existing CivicTech schema instead of overwriting it. If execution fails inside the transaction, fix the reported error before retrying. Do not remove the preflight, use CASCADE, or delete tables to bypass errors.

The Table Editor should show the new tables. Business tables remain empty until the separate demo/import step. Existing Auth accounts, if any, receive CITIZEN profiles; no roles are copied from user metadata.

## Important current state

All 21 application tables have RLS enabled, but no application read/write grants or policies have been opened yet. Guest/public reads also remain closed at this step. This is deliberate staging, not a completed backend.

## Completed step: configure email/password authentication (user confirmed)

1. Open the project's **Authentication** section.
2. Select **Sign In / Providers** (some dashboard versions label this **Providers**).
3. Open **Email**. Ensure **Enable Email provider** is ON and **Confirm email** is ON; save any changes.
4. On the same sign-in configuration page, ensure **Allow new users to sign up** is ON; save any changes. If already enabled, leave it enabled.
5. Reply **Done**. Do not create role assignments in user metadata. Test accounts and the trusted first Administrator assignment follow in a separate step.

The profile trigger already gives newly created identities the CITIZEN role. Elevated roles will be assigned through a trusted process. Anonymous guest identity configuration, email delivery and mobile redirect verification are separate pending steps. No passwords or secret keys are needed in chat.

## Completed step: create five test identities and verify profile creation (user confirmed)

1. Open **Authentication > Users > Add user > Create new user**.
2. Use an email address you control and a strong unique password saved privately. Enable **Auto Confirm User** for this manually provisioned test identity, then click **Create user**. This confirms just the selected test account; leave the global Confirm email setting ON.
3. Repeat for five distinct identities intended for Citizen, Officer, Coordinator, Contractor and Administrator. Supported email aliases may be used (for example, `yourname+civic-officer@your-domain`), but only use addresses/aliases your provider supports. Keep a private list mapping each account email/UUID to its intended role. Do not paste passwords into chat.
4. Leave role metadata empty. All five profiles should initially be CITIZEN; this is expected. Actual roles/scopes and the first Administrator bootstrap follow separately.
5. In **SQL Editor > New query**, run `verification/002_check_auth_profiles.sql` as postgres. Each new test identity should have `email_confirmed`, `profile_exists`, `is_active`, and `email_matches` true, with role CITIZEN. Summary results should show zero missing profiles and zero mismatched emails.
6. Reply **Done — five accounts, profiles verified**, or send the exact error if creation/verification fails. Detailed emails/UUIDs need not be shared.

Dashboard-created, manually confirmed accounts avoid depending on email delivery for these tests. Signup confirmation email delivery, recovery and real sign-in still need verification later. No elevated permission has been granted by creating these identities. If an account already exists, verify/reuse it rather than attempting a duplicate.

## Earlier prepared step: bootstrap the first Administrator (included in combined file)

Open **SQL Editor > New query**, copy the entire `admin/001_bootstrap_admin.sql` and run as **postgres**. The script targets `admin@test.com`, the intended test admin account in the screenshot. It requires an active profile and a confirmed, non-anonymous Auth identity, refuses to create a second first Administrator, and writes an audit record in the same transaction. It creates no public role-assignment function and changes no RLS grants. Repeating a successful bootstrap performs no additional writes.

Expected final result: one row with `email = admin@test.com`, `role = ADMINISTRATOR`, `is_active = true`. Reply **Done** after verifying that row, or report the exact error. The Officer, Coordinator and Contractor accounts still have CITIZEN profiles; their role and scope assignments follow after department/jurisdiction/contractor setup. Backend integration remains pending.

Local command `node supabase/tests/check_admin_bootstrap.mjs` passed eight isolated PostgreSQL checks: missing account, unconfirmed account, anonymous identity, inactive profile, existing Administrator all rejected; correct account elevation and one audit entry; repeat without duplicate writes; authenticated client denied. Cloud execution is pending user confirmation.

Later steps add scoped read policies, protected transactional workflows, administrator/test-account setup, Storage policies, overdue scheduling, demo imports and final permission tests. No later step is assumed completed.

The schema preserves permanent Project IDs as text primary keys and makes them immutable. Milestones use a project-scoped composite key. A mandatory trigger computes each complaint's deadline as submission time plus exactly 168 hours. Private internal officer notes live in a separate reviews table. Missing sample project references will be retained in private rejection records during import.

## Local verification performed

Command: `node supabase/tests/check_schema.mjs` from the workspace root.

The isolated test runtime uses PGlite's embedded PostgreSQL and minimal mock Supabase Auth objects. It does not connect to or prove the state of the user's cloud project. There are no Android dependency additions.

All 13 checks passed:

1. Migration executes atomically.
2. All 21 exposed tables have RLS and no anonymous/authenticated table grants.
3. Auth user metadata cannot self-assign elevated roles.
4. Auth email updates synchronize the correct profile.
5. Project IDs cannot be changed.
6. Identical milestone IDs can belong to different projects.
7. A progress report cannot reference another project's milestone.
8. Complaint deadline remains exactly 168 hours across DST and resists tampering.
9. Unapproved progress cannot be published.
10. Anonymous API cannot read profiles or update projects.
11. Authenticated API cannot self-promote or read private review notes.
12. Setup verification queries execute and return the expected protected tables.
13. Rerunning installation fails safely without overwriting records.

Optional test-runtime installation: `npm.cmd install --prefix supabase/tests/.runtime --no-save --package-lock=false --ignore-scripts @electric-sql/pglite`. This directory is ignored for version control and is only for local SQL tests.

## SUPABASE READY FOR ANDROID INTEGRATION

- [x] Application source inspection.
- [x] Cloud project creation reported complete by the user.
- [x] Initial schema file and local constraint/default-deny checks.
- [x] Remaining combined SQL, scoped policies, transactional workflows, Storage and safe seed locally tested.
- [ ] Cloud schema installation and verification results.
- [ ] Scoped RLS reads and protected workflow RPCs installed and tested.
- [ ] Auth configuration, guest identities and five role accounts verified.
- [ ] Storage buckets, paths, upload/read permissions verified.
- [ ] Backend overdue scheduler verified.
- [ ] Demo/import rows and relationship rejection report verified.
- [ ] Publishable key and Project URL identified for Stage 2.
- [ ] Final allow/deny security tests and integration readiness confirmed.

## Official references

- SQL Editor function/query execution: https://supabase.com/docs/guides/database/functions
- Auth provider settings: https://supabase.com/docs/guides/auth/general-configuration
- Email/password authentication: https://supabase.com/docs/guides/auth/passwords
- RLS and default-deny behavior: https://supabase.com/docs/guides/database/postgres/row-level-security
- Storage access control: https://supabase.com/docs/guides/storage/security/access-control
- Safe Android key selection: https://supabase.com/docs/guides/getting-started/api-keys
