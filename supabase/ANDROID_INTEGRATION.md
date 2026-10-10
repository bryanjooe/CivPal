# Android Supabase integration

## Implemented behavior

Supabase supplies all persistent runtime collections. `CivicDataRepository` is an observable compatibility cache for the existing Compose screens; startup never seeds projects or creates demo users. Intentional Stage 1 demonstration projects remain in the database and are labeled as examples. Existing permanent project IDs, QR payloads, navigation routes, theme, cards, drawer and highlighted Scan action are preserved. Necessary additions include loading/error/refresh controls, verified account forms, catalog inputs, publishing, genuine file selection and server authorization controls.

`CivicSupabase` owns the configured Auth, Postgrest, Storage and Functions client. `CivicBackend` performs paginated, ordered reads from the existing invoker views and sends typed arguments to existing transaction RPCs. `CivicRecords` maps SQL fields and joined child records into the existing app models. Refresh builds a complete snapshot before replacing observable state; failures retain the previous snapshot with a stale indicator. RPC success precedes UI success. Uploads are separate from record creation and cannot be made one database transaction; a retained record ID lets attachment retries reuse the saved record while that form stays open. After a timeout or leaving the form, refresh and inspect history before resubmitting.

### Auth and permissions

Email/password authentication obtains the actual backend profile role. Login screen selection cannot grant privileges. Verified profiles route to CITIZEN, OFFICER, COORDINATOR, CONTRACTOR or ADMINISTRATOR. SDK sessions restore on startup; passwords are never stored by application code. Logout clears SDK session and observable identity data, including when offline. Inactive/unavailable profiles clear the protected app cache. Email confirmation and password recovery use `civic://auth-callback`.

Public reads require no account. Guest complaint/follow actions create a Supabase anonymous Auth identity with a citizen profile and RLS ownership. Anonymous identities cannot obtain privileged roles. Anonymous saved projects and complaints remain tied to that installation's session; logging out/reinstalling loses that anonymous identity. Use a permanent citizen account for access across devices.

Officer/Coordinator lists use backend department/jurisdiction scope, Officer assignments use authenticated UUIDs, Contractor assignments use the verified contractor association, and saved projects use caller-owned follow rows. Database RLS and RPC checks remain authoritative. Administrator role changes clear existing scopes: explicitly grant the desired scopes afterwards. Disabling an account clears its scopes; re-enabling requires regranting them. The administrator cannot change their own active status/role through these RPCs.

### Persistent workflows

Project registration calls the approved atomic ID generator and creates an unpublished record. Publish from My Projects before public/citizen/QR access. Progress reports and officer approval persist through the existing RPC; approval updates project progress and selected evidence publication. Delay history and deadline request/review use database transactions. Complaints preserve caller ownership, routing, server timestamps, 168-hour deadlines and response/event history. Citizen information replies retain the original deadline. Database Cron, rather than the phone, owns escalation.

Coordinator conflict recording/resolution uses real project IDs. Handover initiation freezes the outgoing officer's assigned projects in the chosen office and snapshots actual pending complaints/progress verifications as tasks. The verified incoming officer UUID must have that office/department scope; acknowledgement on/after the transfer date moves assignments atomically. Historical records remain accessible under RLS. Administrator screens read live user/catalog/audit/statistics data. Department and jurisdiction screens retain their existing catalog-list behavior; there were no working catalog editor forms to replace.

### Storage and refresh

Complaint photos and progress evidence accept JPEG/PNG/WebP; deadline supporting documents accept PDF. Files must be nonempty and at most 10 MiB. Validate before creating the associated record, upload to the authorized caller/record path, then attach through the approved RPC. Buckets remain private; image downloads use authenticated Storage requests and loading/retry UI. PDF viewing downloads through RLS into private temporary cache and grants a reader temporary access to that single file. A PDF reader must be installed. Previously downloaded content cannot be revoked by later server permissions. Linked files cannot be overwritten/deleted by clients; an upload whose linking fails may leave an orphan requiring server-side cleanup.

One lifecycle-managed foreground refresh runs every 30 seconds; a visible Refresh action always fetches the latest database data. Realtime subscriptions are not installed and the publication was not modified. Changes across devices become visible on refresh or the next successful foreground poll. No SQL migration or RLS rule was changed for this Android integration.

## Database mapping

| Existing database tables, accessed through `civic_` invoker views | Android usage |
| --- | --- |
| departments, jurisdictions, contractors | Actual catalogs, routing and assignment associations |
| profiles, profile_scopes, project_assignments | Verified identity, authorization scope and Officer responsibility |
| projects, project_milestones, progress_reports, progress_report_reviews, delay_records | Project lists/details/QR, Contractor submission, Officer review and delay history |
| complaints, complaint_responses, complaint_events | Caller-specific history, department routing, replies and status/event timeline |
| project_conflicts | Coordinator recording, resolution and reports |
| officer_handovers, handover_projects, handover_pending_tasks | Frozen project set, actual task records, acknowledgement and clarification |
| project_follows | Each citizen's saved-project association |
| deadline_revision_requests | Contractor request/document and Officer decision |
| audit_logs | Administrator event history |

Database writes use the Stage 1 `civic_*` RPCs, except account invitations through the deployed trusted function. The safe views expose only permitted columns; the underlying tables retain RLS. Storage bucket names are `complaint-photos`, `progress-evidence`, and `deadline-documents`.

## Local setup and versions

Use `local.properties.example` to configure your own ignored `local.properties`:

```properties
sdk.dir=C:/Users/YOUR_USER/AppData/Local/Android/Sdk
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_YOUR_PUBLIC_KEY
```

The configured project here is `wbczfllhlxyvziakznuy`. BuildConfig reads these values; source files contain no credential values. The build rejects a secret/service-role key in the publishable field. An APK necessarily contains its publishable key, which is intended for client use. A secret key, service_role key or database password must never be added to Android or Gradle client configuration.

Supabase Kotlin 3.2.6, Ktor OkHttp 3.3.1 and Java core-library desugaring keep minSdk 24. Auth's browser dependency is constrained to compatible androidx.browser 1.8.0 to retain compileSdk 35. Existing Kotlin 2.2.10, AGP, Compose and unrelated dependency versions remain unchanged.

## Cloud settings and invitation function

Completed in the signed-in Supabase dashboard on 2026-10-10:

- Enabled and saved anonymous sign-ins; confirmed `external.anonymous_users=true` through the live Auth settings API. CAPTCHA was not enabled.
- Added `civic://auth-callback` to the redirect allowlist.
- Deployed `civic-invite-user` using the exact local TypeScript source.
- With explicit user approval, disabled its legacy-secret gateway JWT check. The function calls Auth `/user` to verify the incoming token and requires an active ADMINISTRATOR profile before privileged access.
- Live no-session and invalid-token requests each returned the expected custom HTTP 401. No email invitations were sent by these tests.

The function uses Supabase's built-in server-only `SUPABASE_SERVICE_ROLE_KEY`. No manual secret key is necessary. To use a separately created `sb_secret_…` key, set **CIVIC_SUPABASE_SECRET_KEY** in the Edge Function secrets dashboard, never in the app. This optional override was not configured because no such key was supplied. The function supports both opaque secret and legacy JWT server keys. Do not paste a secret into chat or commit it.

For another project, apply the existing Stage 1 setup, allow the app callback, enable anonymous Auth if desired, and deploy the local function with `supabase functions deploy civic-invite-user --project-ref YOUR_PROJECT --no-verify-jwt`. The checked-in function config expresses that setting; the function still performs its own Auth verification. Configure email delivery before testing invitations. An invitation creates CITIZEN; after email confirmation, use the administrator role/scope form to authorize officials. Account roles cannot be chosen through invite metadata. Successful administrator invitation/email delivery has not yet been exercised with a real administrator session.

## Verification results and limits

- Android debug APK assembly, 16 JVM unit tests and lint passed on this machine. Final build reports are under `app/build/reports`.
- 43 isolated PostgreSQL checks passed, covering citizen/guest ownership, scoped role permissions, immutable IDs, progress/deadline/conflict/handover workflows, Storage policy predicates and escalation. These use the actual migrations in PGlite; they do not test live Storage/Auth/Cron service behavior.
- Live public project reads returned the intentional two imported projects and four milestones.
- A separate read-only Android instrumentation test passed on the connected emulator against this live project: SDK Auth initialization, public views, published-project filtering, absence of private records in the public snapshot, exact permanent-ID lookup and DTO numeric mapping. It created no accounts or records and sent no invitations. Command: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.civic.CivicPublicBackendIntegrationTest`.
- Four QR/navigation instrumented tests were attempted on the only connected emulator, Android 17/API 37 preview. Three failed before their assertions because Espresso 3.6.1 could not find `InputManager.getInstance`; one passed. This is not a successful UI-suite result. Run the suite on a stable Android device/emulator using the existing supported test stack.
- No physical two-device scenario, permanent-role login, session restoration, successful live upload, email recovery/invite completion or live Cron execution is claimed as passed.
- Contractor report decisions are visible, but Stage 1 deliberately hides internal officer review notes from contractors. The UI describes them as internal. Sharing officer feedback would need an approved separate safe feedback field/policy; no privacy rule was weakened.
- The existing map illustration now positions cloud records approximately from coordinates; this migration does not introduce a geographic map service.
- Runtime cache holds only the currently fetched session's data. A multi-table refresh is not a database-wide atomic snapshot; foreground refresh reconciles concurrent server changes. No offline write queue was added.
- This workspace has no Git repository metadata. Modified files were reviewed against the preserved initial workspace copy, not a Git diff. No screen or existing route was removed, and SQL/schema files were left unchanged.

## Exact two-device acceptance test (not yet run)

1. Install `app/build/outputs/apk/debug/app-debug.apk` on Device A and Device B. Both need internet. Use existing account passwords directly on the devices; do not send them in chat.
2. Device A: log in with `officer@test.com`. Check that the verified backend role is Officer. Registration must use the officer's assigned department and office, the actual registered municipality and contractor names, valid coordinates, dates and budget. For the existing PWD fixture, choose the registered L&T contractor associated with `contractor@test.com` and Greater Chennai Corporation. Enter a distinct acceptance-test title. Save and record the new server-generated permanent ID.
3. Device A: open My Projects and **Publish** the new project. Confirm refresh shows it. Restart the app and confirm it persists with the same ID.
4. Device B: Continue as Guest, press Refresh, find that ID and open its public details. Scan its printed QR from Device A if camera/Google scanner services are available. The payload remains the plain project ID.
5. Device A: log out and log in as `contractor@test.com`; find the same assigned project. Submit an actual milestone report with 65% progress and a small valid image. Record the report ID. A pending report must not immediately change public progress.
6. Device A: log out and log back in as `officer@test.com`. Inspect its evidence in Progress Verification and approve the report. Refresh/restart to confirm 65% persists. Device B: press Refresh and confirm the same project displays 65%. New projects avoid the seeded project's already-higher 68% progress.
7. Device B: submit a project-associated complaint with a distinct test description and optional valid image. This creates a restricted anonymous identity when needed. Record the complaint ID. Refresh/restart and confirm it appears in My Complaints for that identity.
8. Device A: refresh the scoped complaint management screen and confirm that complaint ID and image are visible. Respond with Under Review or the appropriate next status. Device B: refresh and confirm the response/status/history persist. A separate citizen account must not see this complaint.
9. Device B: follow that project; restart and confirm it remains saved for the same identity. Use another citizen identity/device and confirm saved lists are independent.
10. Repeat with network disabled: show a friendly error and stale indicator, restore internet, Refresh, and confirm the server state. Inspect history before repeating a timed-out write.

Also manually verify all five role logins, wrong-role selection, inactive accounts, denied cross-department/contractor actions, recovery email callback, coordinator conflict record/resolve, administrator invite/confirmation/role/scopes, deadline PDF request/review, delay history, incoming-officer handover acknowledgement/clarification, English/Tamil navigation, drawer/Profile and QR print/scan. For handover, create a second confirmed Officer and grant the exact department/office scope first; an administrator supplies its verified UUID. Confirm transferred assignments on both accounts. Check Cron through Stage 1 verification SQL rather than advancing the phone clock.

## File inventory

The appended inventory lists source/test changes compared with the preserved initial workspace. Paths are relative to `app/`. Build configuration additionally changed in `app/build.gradle.kts`; safe template `local.properties.example`, the Edge Function/config and this report were added. The initial local file is ignored. Existing SQL migrations and seeds were not modified.

### New source and test files

- src/androidTest/java/com/example/civic/CivicPublicBackendIntegrationTest.kt

- src/main/java/com/example/civic/data/backend/CivicBackend.kt
- src/main/java/com/example/civic/data/backend/CivicRecords.kt
- src/main/java/com/example/civic/data/backend/CivicSupabase.kt
- src/main/java/com/example/civic/ui/components/CivicStoredDocument.kt
- src/main/java/com/example/civic/ui/components/CivicStoredImage.kt
- src/main/java/com/example/civic/ui/navigation/BackendStatus.kt
- src/main/java/com/example/civic/ui/screens/admin/UserAuthorizationDialog.kt
- src/main/java/com/example/civic/ui/screens/coordinator/RecordConflictDialog.kt
- src/main/res/xml/document_paths.xml
- src/test/resources/civic-public-fixture.json

### Modified source and test files

- src/androidTest/java/com/example/civic/ProjectQRIntegrationTest.kt
- src/main/AndroidManifest.xml
- src/main/java/com/example/civic/MainActivity.kt
- src/main/java/com/example/civic/data/CivicDataRepository.kt
- src/main/java/com/example/civic/data/models/Project.kt
- src/main/java/com/example/civic/ui/components/ProjectCard.kt
- src/main/java/com/example/civic/ui/navigation/CivicNavHost.kt
- src/main/java/com/example/civic/ui/qr/ProjectQRUtils.kt
- src/main/java/com/example/civic/ui/screens/admin/AdminDashboardScreen.kt
- src/main/java/com/example/civic/ui/screens/admin/AdminSettingsScreen.kt
- src/main/java/com/example/civic/ui/screens/admin/DepartmentManagementScreen.kt
- src/main/java/com/example/civic/ui/screens/admin/JurisdictionsScreen.kt
- src/main/java/com/example/civic/ui/screens/admin/UserManagementScreen.kt
- src/main/java/com/example/civic/ui/screens/auth/RoleLoginScreens.kt
- src/main/java/com/example/civic/ui/screens/auth/SplashScreen.kt
- src/main/java/com/example/civic/ui/screens/auth/WelcomeScreen.kt
- src/main/java/com/example/civic/ui/screens/citizen/CitizenDashboardScreen.kt
- src/main/java/com/example/civic/ui/screens/citizen/CitizenProfileScreen.kt
- src/main/java/com/example/civic/ui/screens/citizen/ComplaintDetailsScreen.kt
- src/main/java/com/example/civic/ui/screens/citizen/ComplaintSubmissionScreen.kt
- src/main/java/com/example/civic/ui/screens/citizen/ProjectDetailsScreen.kt
- src/main/java/com/example/civic/ui/screens/citizen/ProjectMapScreen.kt
- src/main/java/com/example/civic/ui/screens/citizen/ProjectQRScannerScreen.kt
- src/main/java/com/example/civic/ui/screens/citizen/PublicProjectsScreen.kt
- src/main/java/com/example/civic/ui/screens/contractor/ContractorAssignedProjectsScreen.kt
- src/main/java/com/example/civic/ui/screens/contractor/ContractorDashboardScreen.kt
- src/main/java/com/example/civic/ui/screens/contractor/DeadlineRevisionScreen.kt
- src/main/java/com/example/civic/ui/screens/contractor/SubmitProgressScreen.kt
- src/main/java/com/example/civic/ui/screens/coordinator/ConflictDetectionScreen.kt
- src/main/java/com/example/civic/ui/screens/coordinator/CoordinatorDashboardScreen.kt
- src/main/java/com/example/civic/ui/screens/coordinator/CoordinatorReportsScreen.kt
- src/main/java/com/example/civic/ui/screens/coordinator/DepartmentProjectsScreen.kt
- src/main/java/com/example/civic/ui/screens/coordinator/SharedScheduleScreen.kt
- src/main/java/com/example/civic/ui/screens/handover/IncomingOfficerScreen.kt
- src/main/java/com/example/civic/ui/screens/handover/InitiateHandoverScreen.kt
- src/main/java/com/example/civic/ui/screens/officer/OfficerDashboardScreen.kt
- src/main/java/com/example/civic/ui/screens/officer/OfficerMyProjectsScreen.kt
- src/main/java/com/example/civic/ui/screens/officer/OfficialComplaintReviewDialog.kt
- src/main/java/com/example/civic/ui/screens/officer/ProgressVerificationScreen.kt
- src/main/java/com/example/civic/ui/screens/officer/ProjectQRGeneratorScreen.kt
- src/main/java/com/example/civic/ui/screens/officer/RecordDelayDialog.kt
- src/main/java/com/example/civic/ui/screens/officer/RegisterProjectScreen.kt
- src/main/java/com/example/civic/ui/screens/official/ActionInboxScreen.kt
- src/main/java/com/example/civic/ui/screens/official/OfficialDashboardScreen.kt
- src/test/java/com/example/civic/CivicRepositoryTest.kt
- src/test/java/com/example/civic/ProjectQRTest.kt
