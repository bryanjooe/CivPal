# CivicTech remaining setup SQL

This reference describes the prepared backend. Local PostgreSQL checks passed; remaining cloud installation and service behavior are not yet verified. Android integration is still Stage 2.

Latest status: the user reported that the combined SQL returned the expected result. The actual 15-row grid has not been supplied, so this is user-reported installation success. Live service checks are still pending. Current step is to identify the Project URL and Publishable Key in the dashboard's Connect dialog.

Live update 2026-10-10: connection details received for `https://wbczfllhlxyvziakznuy.supabase.co`. Read-only requests returned the two demo projects (HTTP 200), denied signed-out profile reads (HTTP 401 / PostgreSQL 42501), and confirmed email Auth/signup/confirmation configuration. Anonymous Auth is currently OFF. Guest abuse protection and sign-in setup, role-authenticated API checks, actual Storage uploads and Cron execution remain pending. No Todo example or Android integration was added.

User subsequently confirmed CAPTCHA is OFF. Current setup instruction is to obtain hCaptcha site/secret keys and configure the hCaptcha Secret Key directly in Supabase Authentication > Attack Protection before enabling guest identities. Secret keys must never be sent in chat. No CAPTCHA setup success or real challenge result is yet confirmed.

Latest preference: the user declined CAPTCHA and asked to continue. Keep CAPTCHA OFF and retain Auth rate limits; the previous CAPTCHA prerequisite is superseded. A subsequent live Auth settings read still returned anonymous_users=false. Current action is to enable Allow anonymous sign-ins in the dashboard. `verification/live_login_check.html` is prepared for password logins and backend-role checks without Android integration; its actual five login results are pending.

## One-paste installation

Use `ALL_REMAINING_SETUP.sql` in **SQL Editor > New query**, execution role **postgres**, after the existing `001_civic_schema.sql` installation. Copy the entire file. Do not rerun schema 001. The bundle includes Administrator bootstrap, access policies, transactional functions, private Storage, the explicitly labeled Android demo seed, test account mappings and backend escalation scheduling. All setup changes are one transaction. If a statement fails, roll back the failed transaction (SQL Editor usually does this when the request ends) and report the exact error; never bypass a guard by deleting tables.

The bootstrap also works if its separate script was already executed. Repeat execution skips the demo seed and test-role mapping markers, preserving subsequent edits. It reapplies the intended policies/function definitions, and updates the named cron job instead of adding another one. Make future intentional policy changes in a new migration rather than expecting this original setup file to retain them.

The last result grid contains **15 configuration checks**. Expect each `passed` value to be true. If check 15 is false, enable **Integrations > Cron > pg_cron** and rerun `migrations/005_civic_scheduler.sql`, then `verification/003_check_backend.sql`. If extension installation itself fails, the atomic bundle rolls back; enable Cron and rerun the complete bundle. A configured job still needs a real execution check in `cron.job_run_details` or Cron's job history.

## Tables and relationships

21 application tables: departments, jurisdictions, contractors, profiles, profile_scopes, projects, project_assignments, project_milestones, progress_reports, progress_report_reviews, delay_records, complaints, complaint_responses, complaint_events, project_conflicts, officer_handovers, handover_projects, handover_pending_tasks, project_follows, deadline_revision_requests, audit_logs.

`profiles.id` references Auth's user UUID. Explicit scope rows associate an Officer/Coordinator with both a department and a jurisdiction. Scope checks also recognize ancestor jurisdictions in an approved jurisdiction tree. The current demo catalog does not invent parent boundaries: office and municipality scopes are granted explicitly. Contractors reference their approved contractor ID. Projects retain their existing immutable text IDs. Milestones use `(project_id, milestone_id)`, and reports reference that composite key. Complaints and follows reference their actual Auth owner. Conflicts reference two real projects. Handovers freeze an explicit project set, and pending tasks reference only that set. Reviews and audit logs stay separate from public updates.

## Read and write permissions

All 21 tables keep RLS. No client gets direct INSERT, UPDATE or DELETE grants on business tables. Use the explicit safe `public.civic_<table>` views for reads and `public.civic_*` functions for writes. Views use `security_invoker=true`, so base-table RLS continues to apply. Base tables have column SELECT grants; `SELECT *` on a base table intentionally fails when it includes ungranted snapshots/internal UUIDs. `SELECT *` on the safe view works. Raw source snapshots are SQL-administrator-only.

| Identity | Authorized access |
| --- | --- |
| Signed-out public visitor | Active catalogs; published projects/milestones; approved explicitly public updates/evidence; published delays. No writes or private accounts/complaints. |
| Citizen, including individual anonymous Auth guest | Public information, own profile/preferences, own follows and complaints, complaint responses/events and own complaint evidence. Can submit complaints and answer information requests on own complaints. |
| Officer | Scoped project management, progress/deadline review, delays and complaint responses. Handovers require actual outgoing assignment or authorized incoming identity. Unrelated department/office writes fail. |
| Coordinator | Approved cross-department scope; records/resolves conflicts only if BOTH project scopes are authorized. Cannot approve progress or record government project delays. |
| Contractor | Assigned contractor projects and reports; submits progress/deadline requests and evidence. Cannot approve reports/requests, edit government records, read internal review notes or read other contractors' private records. |
| Administrator | Verified active permanent identity; manages user roles/status/scopes and catalogs, scoped-independent reviews and audit reads. Cannot alter own role/status through the user-management RPC. Audit rows are never editable by clients. |

Every elevated check consults the active database profile and permanent confirmed Auth identity. Editable user metadata and UI role values never grant a role. Roles/status/scopes are rechecked on subsequent requests. Changing a user's authorization clears its old scopes; granting new scopes is a separate audited operation. Creating further Auth identities/passwords remains a trusted dashboard or server Auth Admin operation, not an app profile INSERT.

## RPC surface

| Group | Function names |
| --- | --- |
| Preferences/follows | civic_update_my_profile, civic_set_follow |
| Projects | civic_register_project, civic_publish_project |
| Complaints | civic_submit_complaint, civic_respond_complaint, civic_reply_complaint, civic_assign_complaint |
| Contractor progress | civic_submit_progress, civic_review_progress |
| Deadlines/delays | civic_record_delay, civic_request_deadline, civic_review_deadline |
| Coordination | civic_record_conflict, civic_resolve_conflict |
| Handover | civic_initiate_handover, civic_answer_handover |
| Administration | civic_admin_set_user, civic_admin_set_scope, civic_admin_department, civic_admin_jurisdiction, civic_admin_contractor |
| Evidence linking | civic_attach_file |

All 23 RPCs require the authenticated database role and then check their authorized CivicTech role. Definer functions use an empty search path and qualified tables. New project registration takes `p_data` as JSON with title, description, department_id, district, municipal_body_id, location, latitude, longitude, contractor_id, responsible_office_id, estimated_budget_in_lakhs, funding_source, start_date and original_completion_date. Optional `p_milestones` is an array of milestone_id/title/target_date objects. The server chooses ID, identity snapshots, initial status/progress and unpublished state; caller-supplied ID/role/published/progress values do not control them.

New project IDs use `TN-<department code>-<current year>-<serial>`. A primary-keyed counter is incremented with an atomic UPSERT. Imported matching IDs are considered, serial formatting does not truncate after 99, and the project PK rejects duplicates. No phone list count is used. Local tests exercise serial allocation; actual concurrent cloud requests remain a live integration check.

Project-associated complaints route to that project's approved department/responsible office. This replaces the source's hardcoded officer and inconsistent category-to-project routing. Standalone complaints require an active municipality and use the source category mapping (POTHOLE -> HWY, WATER_LEAK -> TWAD, DRAINAGE_OVERFLOW -> PWD, others -> MAWS). Officer assignment is a separate audited administrator action; no source person is falsely turned into a test Auth user. Assigned-officer RPC requires an explicit matching scope.

Progress approval locks the report, stores private review notes, updates project progress monotonically and marks completed status at 100%. A reviewed report is immutable; corrections require a new submission. Deadline requests leave the approved deadline unchanged until independent approval, which checks for a stale prior deadline and writes delay history atomically. Handover acknowledgment is available on/after the transfer date and rechecks the outgoing assignment and incoming scope before transferring its frozen project set. Counts derive from child/join rows; legacy claimed counts are historical metadata.

`civic_private.escalate_overdue()` runs once per minute via Cron and is not executable by clients. Complaints retain exactly 168 elapsed hours. Pending, under-review and awaiting-citizen-info complaints escalate once; the original deadline is never reset by a citizen reply. Response statuses, events and audit entries are written in the same transaction.

## Storage

Three private buckets: `complaint-photos`, `progress-evidence`, `deadline-documents`. Each allows at most 10 MiB. The image buckets allow JPEG/PNG/WebP; the document bucket allows PDF. These are Storage-service bucket restrictions and need real upload verification, including oversized/wrong-MIME files.

Upload flow: create the complaint/report/request first, then upload to `<auth UUID>/<record ID>/<random UUID>.<extension>` using the correct bucket. The uploader must own an eligible pending record. Then call `civic_attach_file(p_bucket,p_record,p_path)`. It validates the owned Storage object and links it to the locked business row. Only a reviewed, approved, explicitly public progress record on a published project makes its evidence publicly readable. Complaints and deadline PDFs remain private.

Client UPDATE and DELETE are denied for these buckets, including through pre-existing broader permissive policies. Evidence cannot be replaced after attachment or review. Orphan uploads from an interrupted submission require trusted cleanup through the Storage API after verifying no business record links them; never delete actual files by deleting `storage.objects` SQL rows. Storage objects and placeholder files are not seeded.

## Explicit test role mappings

| Email shown by user | Role and scope |
| --- | --- |
| test@gmail.com | CITIZEN |
| officer@test.com | OFFICER, PWD Chennai office and GCC municipality only |
| coordinator@test.com | COORDINATOR, five demo departments across five municipalities and the two project offices (35 approved pairs) |
| contractor@test.com | CONTRACTOR, CON-LT-9081 only |
| admin@test.com | ADMINISTRATOR, first trusted bootstrap |

All five are marked demonstration identities. These are test fixtures, not verified government employees. The source legacy U1–U5 identities are separately retained privately and are not equated with these people. Real email-confirmation/recovery tests need addresses that the tester controls.

## Demo/import result

The seed retains 2 projects, 4 milestones, 6 complaints, 2 responses, 13 events, 1 historical handover and 2 initial audit rows. It creates no invented reports, delays, transferred-project sets or pending tasks. New workflow audit entries are additional to the 2 imported audit rows. The demo clock is frozen at seed execution, matching the relative durations in the Kotlin fixture; ambiguous historical date-only labels/source expressions remain in private snapshots rather than becoming invented timestamps.

Eight private import issues retain their complete constructor payloads: the missing-project conflict CONF-TN-101; U1–U5 legacy Auth mappings; the globally followed project with no owner; the AL-101 complaint-status discrepancy. Historical complaints have no fabricated citizen UUID owner, so the test citizen must submit a new complaint to test ownership. Historical handover counts 2 projects/3 tasks remain legacy claims without fabricated child records.

Demo seed refuses initial ID collisions, inserts without overwriting, and skips entirely after its marker exists. It does not overwrite real records. `tests/build_demo_seed.mjs` reproduces the seed from the original Android repository without changing it.

Optional official import files under `import/` are kept outside the demo bundle. Run `001_prepare_official_project_staging.sql`, load a reviewed CSV with `source_id,record_data` where record_data is the original project's JSON using SQL column names, then run `002_import_official_projects.sql`. Alternatively insert reviewed JSON into the private staging table in SQL. Required reference catalogs must already exist. Native constraints and FKs validate every row, rejected payloads/errors remain private, existing IDs are never overwritten, and accepted projects remain unpublished with immutable IDs. No official dataset was supplied or imported. Nested milestones/reports/delays require their validated FK-ordered import when an actual dataset is available; the current optional importer covers flat project rows only.

## SQL cannot finish these dashboard/service steps

Email provider/signup/confirmation settings were user-confirmed and independently read from the live Auth settings. Anonymous Auth needs to be enabled under Authentication > Sign In / Providers for guest complaint/follow sessions. The user explicitly chose no CAPTCHA; preserve that choice and retain Auth rate limits. Signed-out project browsing needs no anonymous Auth account. Guest identities use the existing CITIZEN role, not a sixth role.

Email delivery/SMTP, signup confirmation, password recovery, guest sign-in, real role logins, real Storage uploads/downloads and Cron execution history remain live checks. Future Android redirect URLs depend on the Stage 2 auth callback implementation and should be configured when that route is chosen. Do not invent a callback that the app cannot handle. Project URL and Publishable Key still need to be identified in the dashboard; never use a Secret or service_role key in Android.

## Local tests

`node supabase/tests/check_backend.mjs` exercises role allow/deny checks, privacy, storage guards, transactions, imports, workflows and reruns. `node supabase/tests/check_setup_bundle.mjs` tests the actual one-paste bundle, including rollback after a late error. Earlier schema/bootstrap suites remain available. PGlite supplies PostgreSQL; minimal Auth/Storage stubs do not prove Supabase service behavior or Cron worker execution.

## SUPABASE READY FOR ANDROID INTEGRATION

- [x] Existing Android models, operations and source fixtures inspected.
- [x] Schema and remaining executable SQL prepared and locally tested.
- [x] Five Auth account creations visible in user's screenshot; profile checks user-confirmed.
- [ ] Remaining bundle cloud execution and 15 check results confirmed.
- [ ] Live account sign-in and role/scope allow/deny tests.
- [ ] Guest Auth and abuse protection configured and tested.
- [ ] Email delivery, confirmation and recovery verified with controlled addresses.
- [ ] Real Storage uploads, type/size rejection and public/private reads verified.
- [ ] Cron job execution and overdue event/audit idempotency verified in cloud.
- [ ] Cloud demo counts and retained import issues verified.
- [x] Project URL and Publishable Key identified; public API connectivity and signed-out profile denial verified.
- [ ] Android integration approved as separate Stage 2 work.

Official references: [RLS](https://supabase.com/docs/guides/database/postgres/row-level-security), [functions](https://supabase.com/docs/guides/database/functions), [Storage](https://supabase.com/docs/guides/storage/security/access-control), [Cron](https://supabase.com/docs/guides/cron/install), [anonymous Auth](https://supabase.com/docs/guides/auth/auth-anonymous), [API keys](https://supabase.com/docs/guides/getting-started/api-keys).
