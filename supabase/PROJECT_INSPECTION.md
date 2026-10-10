# CivicTech Supabase Stage 1: source inspection and proposed design

Status: source inspection completed; account sign-in, cloud project creation and initial schema installation/verification reported complete by the user's Done replies. Detailed cloud query results have not been supplied, so independent database verification remains pending. The first schema migration passed 13 isolated local PostgreSQL checks. Android application files were not modified for Stage 1. Migrations are executed through the user's confirmed, step-by-step setup flow.

## Architecture and source inventory

The app uses Kotlin, Jetpack Compose, Material 3, and one Navigation Compose NavHost. CivicDataRepository is a singleton with six observable, in-memory collections: projects, complaints, conflicts, handovers, users, and auditLogs. It also holds currentUser and currentLanguage. Project children and complaint children are nested Kotlin lists. There is no shared backend, persistent database, real authentication, or real attachment upload implementation.

Inspected model files: Project.kt, Complaint.kt, Conflict.kt, Handover.kt, and User.kt. Inspected the repository's initialization, sample records, and all mutation methods; auth/navigation entry points; and the data access, mutation, hardcoded list, and statistics portions of every screen. Current QR generation/scanning uses the exact permanent Project.id; the backend must keep those IDs compatible.

### Screen operations

| Screens | Current reads/writes and backend implications |
| --- | --- |
| RoleLoginScreens, Welcome, LoginTypeSelect, CivicNavHost | All five roles use local demo identities; entered passwords are not verified. Guest creates GUEST-001 locally. Real identity must come from Supabase Auth, with backend-assigned roles. |
| CitizenDashboard, PublicProjects | Project lists, status counts, filters, followed-project ordering, follow/unfollow. Department filters are hardcoded. |
| ProjectDetails | Reads one project by exact ID and its nested milestones/delays; follow/unfollow; complaint navigation. Public projection must exclude internal notes and private account fields. |
| ProjectMap | Reads projects; illustrated map markers use fixed screen offsets rather than geographic mapping. Do not seed those offsets as geographic data. |
| CitizenProfile | Reads globally followed projects; displayed identity/contact/ward are hardcoded; notification switch is local state. |
| ComplaintSubmission | Creates complaints; citizen name is hardcoded S. Karthik; selecting an image fabricates a filename. Real submission must bind the caller's identity. |
| MyComplaints, ComplaintDetails | Reads all complaints without ownership filtering; displays responses/events and seven-day deadlines. MyComplaints also triggers overdue evaluation. Backend must restrict complaint ownership and private evidence. |
| OfficerDashboard | Uses officer name or responsible-office substring matching for project scope; complaint count remains global. Replace text matching with trusted relationships in the backend design. |
| OfficerMyProjects | Searches all projects; records delays; navigates to project registration/details. It has no authorization filter. |
| RegisterProject | Creates project and M1/M2 milestones; office/name taken from currentUser, contractor ID and coordinates are repository defaults. |
| RecordDelayDialog | Adds delay, revised completion date, delayed status, and audit entry. |
| ProgressVerification | Reads all pending reports; approves or requests correction, writes notes, advances project progress, sets completion status at 100%. |
| OfficialComplaintReviewDialog | Records a response and status decision or requests citizen information, then adds timeline/audit entries. |
| OfficialDashboard | Reads all four business collections for dashboard statistics; role actions are UI-based. |
| ActionInbox | Derived from complaints, pending reports, conflicts, and handovers; filters by UI role; triggers deadline evaluation. No independent persistent inbox table is necessary. |
| ContractorDashboard, ContractorAssignedProjects | Read all projects without contractor assignment enforcement. Pending request count is hardcoded 1. |
| SubmitProgress | Creates a report for selected project, hardcodes milestone M1 and submitter name; selected photo filename is not passed to the repository method. |
| DeadlineRevision | Collects project/date/reason and a placeholder PDF filename, then shows success without storing a request. Schema support is proposed for this existing intended feature; there are no requests to import. |
| CoordinatorDashboard, DepartmentProjects | Read projects/conflicts; department count/list are hardcoded. |
| SharedSchedule | Reads project dates; no independent schedule collection or write operation. Use project timelines rather than duplicate scheduling tables. |
| ConflictDetection | Reads conflicts and resolves one; repository updates current resolution/date/status and adds audit log. |
| ResolutionHistory | Reads resolved conflicts; no separate historical list exists. Preserve resolved records and their changes in audit logs. |
| CoordinatorReports | Department totals are hardcoded and do not match the two seeded projects. Derive future statistics from authorized database rows. |
| HandoverDashboard | Reads handovers, reported counts, and status. |
| InitiateHandover | Creates handover with placeholder pending tasks and counts; substring matching estimates assigned projects. |
| IncomingOfficer | Always takes the first handover; acknowledgment changes officer responsibility using substring matching. Clarification displays a dialog without persistence. |
| AdminDashboard | Users/projects/audit counts from repository; department count is hardcoded. |
| UserManagement | Adds a local User; Change Role and Toggle Status callbacks are empty. Creating a profile is not creating an Auth account. |
| DepartmentManagement, Jurisdictions | Display five hardcoded records each; no repository collections or persistence. Office jurisdictions in users and municipalities in projects are different concepts. |
| AdminSettings | Notification and audit switches are local state only. Backend audit logging must remain compulsory and cannot be disabled by a client toggle. |
| AuditLog | Reads repository audit entries; future logs must be append-only and server-generated. |
| ProjectQRGenerator, ProjectQRScanner | Read existing projects; generate/print stable payloads and look up exact IDs. No QR ID table is needed. |
| Splash and other navigation-only screens | No business-record persistence requirements. |

No existing business-record delete operation was found. UI draft/search/filter/menu state does not need database tables.

## Proposed relational mapping

This is a design proposal, not deployed SQL.

| Kotlin source / existing feature | Proposed tables and relationships |
| --- | --- |
| User | profiles.id references auth.users.id. Retain a legacy_user_id mapping, name, protected role, protected account status, department/jurisdiction references, demo flag. Email identity is synchronized from Auth, not trusted from arbitrary client writes. |
| Hardcoded department catalogs | departments with unique codes/names and original source labels for import mapping. Public Works Department and Public Works Department (PWD), and the municipal-administration labels, need explicit mapping. |
| Jurisdictions, responsible offices, municipality, district | jurisdictions with jurisdiction type and parent relationship. Preserve source names; map municipality and responsible office separately. Trusted profile scope determines authorized jurisdiction. |
| Project contractorId / contractorName | contractors with existing contractor IDs and names. A trusted profiles.contractor_id association binds contractor accounts to the correct contractor record; it is not a new application role. |
| Project | projects.id remains the existing text primary key. All related project FKs use this exact ID. Keep title, description, municipality, responsible office, coordinates, budget/funding, dates, statuses and percentages; preserve historical name snapshots alongside identity references. |
| Milestone | project_milestones primary key (project_id, milestone_id). Reused M1/M2 identifiers cannot collide across projects. |
| ProgressReport | progress_reports with composite FK (project_id, milestone_id) to project_milestones; contractor submitter, evidence path, status and verification metadata. Private officer notes require column isolation or protected projections. |
| DelayRecord | delay_records linked to projects, preserving original/revised dates, reason, published explanation and recorded-by identity/name snapshot. |
| Complaint | complaints with optional project FK, owner profile, citizen-name snapshot, category, department/assigned officer, timestamps, status, priority, and evidence object path. |
| ComplaintResponse | complaint_responses linked to complaint and authorized actor; preserve actor display snapshot, role snapshot, message and timestamp. |
| ComplaintEvent | complaint_events linked to complaint; preserve source event IDs within each complaint, timestamp, event type, description and actor. This table is required even though omitted from the suggested list. |
| ScheduleConflict | project_conflicts with two project FKs, location/date details, type/status, proposed/final resolution and resolved timestamp; original source title/department snapshots can be retained for historical import. |
| OfficerHandover | officer_handovers with outgoing/incoming identities, source name/email snapshots, department/jurisdiction, transfer date, reason, notes, acknowledgment and clarification metadata. |
| Handover assigned project set | handover_projects join table explicitly freezes which projects are transferred. No string matching during acknowledgment. |
| PendingTask | handover_pending_tasks scoped to handover, referencing valid related project; title/priority/due date retained. |
| Project.isFollowed | project_follows primary key (user_id, project_id). Never a global project flag. |
| AuditLogEntry | audit_logs preserving imported legacy IDs and snapshots; new audit rows generated by protected server workflows, not forged by clients. |
| DeadlineRevision screen | deadline_revision_requests linked to project/contractor, proposed date/reason/evidence, review decision and timestamps. Submitting does not change the approved project deadline. |

Future new project IDs will be generated by an atomic database counter/sequence and unique constraints, using a department code and year. Imported IDs stay exactly as supplied. An immutability trigger will reject project ID changes. There is no separate QR identifier.

Native PostgreSQL date/timestamptz columns will replace display strings for new records. Original display strings/snapshots are retained where source dates are ambiguous (Today, Recent, or relative labels). Those values must not silently become invented historical timestamps.

## Security decisions to implement and test

- Exactly the current CITIZEN, OFFICER, COORDINATOR, CONTRACTOR and ADMINISTRATOR roles.
- Default new profiles to CITIZEN. Ignore caller-supplied role metadata; protect role, department, jurisdiction, contractor association, and status against self-editing.
- Bootstrap the first administrator only through trusted SQL Editor administration after creating its real Auth identity. No public self-promotion function.
- RLS on every exposed table; explicit grants. Public read access only to published project fields, milestones, approved public progress and published delay information. RLS filters rows, not columns: public projections/grants must also prevent private notes and contact fields from leaking.
- OFFICER writes require a trusted department/jurisdiction scope. COORDINATOR conflict actions require authorized geographic/cross-department scope. CONTRACTOR submissions require matching assigned contractor and cannot approve themselves. Administrator operations require verified role and active account.
- Complaint ownership is based on auth.uid(), never a supplied name. Ordinary unauthenticated visitors can browse public information. To preserve guest complaint submission/follows securely, use a Supabase anonymous Auth identity with CITIZEN permissions; each guest gets a different identity. It must never share GUEST-001. Anonymous sessions are distinct from the unauthenticated anon database role. Configure abuse protections before enabling them.
- Sensitive compound changes (verification + project progress, complaint response + timeline/status, handover + responsibility transfer) use authorized transactional functions. Server time and protected workflow rules control status transitions and seven-day deadlines.
- Overdue escalation must run in the backend without requiring a screen visit, with idempotent escalation events and audit records. The exact seven-day duration will preserve the source's 168-hour calculation.
- Proposed private buckets: complaint-photos, progress-evidence, deadline-documents. The last bucket is justified by the existing deadline-request PDF UI, which currently has no real uploads. Complaint images remain private. Approved progress evidence can have read policies tied to approved public reports; never make the entire evidence bucket public.
- Proposed limits: complaint/progress images up to 10 MiB, JPEG/PNG/WebP; deadline supporting PDFs up to 10 MiB, application/pdf. User/record/unique-object paths and Storage RLS bind uploads and reads to authorized owners and actual business records. Placeholder filenames are not imported as real Storage objects.

## Demonstration import audit

Raw source preload contains:

- 2 projects: TN-PWD-2025-01 and TN-TWAD-2025-02.
- 4 milestones, all under TN-PWD-2025-01; the second project has none.
- 0 progress reports and 0 delay records.
- 6 complaints (CMP-2026-101 through CMP-2026-106), 2 responses and 13 complaint events.
- 1 conflict (CONF-TN-101).
- 1 acknowledged handover (HO-2026-88) with no pending-task child records.
- 5 demo users and 2 initial audit logs.
- 5 hardcoded admin department listings and 5 municipality jurisdiction listings; additional office/department labels appear in users, project and login code.

These counts describe source preload, not live state after user interactions or repository deadline evaluation.

Import issues requiring an explicit policy:

1. Conflict CONF-TN-101 references missing project TN-TWAD-2025-09. Preserve the original conflict in a non-exposed import rejection/staging report until a legitimate matching project is supplied. Do not manufacture the project or weaken the FK.
2. Handover HO-2026-88 claims 2 assigned projects and 3 tasks, but has no task children and no explicit transferred-project list. Preserve the reported source counts as legacy metadata; do not fabricate child rows.
3. Several source officer/citizen names have no User entry. Preserve historical name snapshots; do not fabricate Auth accounts or map people to unrelated users.
4. Contractor User U4 has no explicit contractor ID. Its apparent association with CON-LT-9081 needs a trusted demo-account mapping, not matching based on display-name similarity.
5. Complaint dates are generated relative to startup, and one audit log says CMP-2026-102 was resolved although its current sample status is pending. Retain clearly labeled demonstration data and flag discrepancies.
6. The Anna Salai globally followed flag has no owner. Assigning it to a particular test citizen requires an explicit demo mapping; do not import it as everyone following that project.
7. Placeholder photos/PDFs, missing reports, missing delays, fake analytics and hardcoded profile identities are not proof of actual uploaded files or verified government records.

The demo seed must be separate and explicitly enabled, preserve IDs, use collision detection and idempotent inserts, and never update real records on conflict. Official CSV/SQL imports will use staging validation and dependency order with rejection reporting.

## Setup progress

Latest user instruction requests all remaining SQL together. `ALL_REMAINING_SETUP.sql` is now prepared with the previously separate bootstrap, migrations 002–005, labeled source demo seed and explicit test-role mappings, followed by 15 read-only configuration checks. The combined transaction and backend behavior passed 46 new local PostgreSQL checks (43 backend and 3 bundled execution/rollback/rerun checks). Cloud execution remains pending; account/provider/profile confirmations remain user-reported. See `BACKEND_REFERENCE.md`. This combined delivery supersedes the previous request to wait after each SQL substep, but does not authorize Stage 2 Android edits.

The user requested one major step at a time and a Done reply before advancing. No connected Supabase-specific tools were found in this session. SQL files and verification queries can be prepared locally; the user will execute dashboard actions and report results.

Account sign-in: confirmed by the user's Done reply.

Cloud project creation/provisioning: confirmed by the user's second Done reply. Actual project name and region have not been supplied; do not assume them.

Initial schema step: user confirmed completion after being instructed to execute migrations/001_civic_schema.sql and verification/001_check_schema.sql. This creates 21 protected application tables, plus private migration/counter/import-rejection tables. Detailed results remain unprovided. Email provider, email confirmation and new-user signup settings were subsequently confirmed by Done. Five Auth accounts were shown in the user's screenshot and profile verification was confirmed by Done; detailed query output was not supplied. Current action: run admin/001_bootstrap_admin.sql as postgres to assign the first Administrator to admin@test.com, then wait for Done. Eight local PostgreSQL bootstrap checks passed. All other accounts remain CITIZEN pending trusted role/scope assignment. Scoped RLS read policies and transactional write functions remain pending; API access is closed at this stage. No database credentials or secret/service-role API keys are needed in chat.

Implementation refinements: profile_scopes stores explicit department/jurisdiction authorizations; project_assignments separates internal officer identity references from public project information; progress_report_reviews isolates private officer notes from report payloads. These are access-control relationships for existing roles/features, not new application roles.

### SUPABASE READY FOR ANDROID INTEGRATION

- [x] Existing models, repository operations, demo seed inconsistencies and screen data accesses inspected.
- [x] Cloud project creation/provisioning confirmed by the user.
- [ ] Live database configuration independently checked through verification-query results.
- [ ] SQL migrations created, executed and verified.
- [ ] Auth providers/guest workflow configured and verified.
- [ ] Five real test identities and protected role assignments verified.
- [ ] RLS/grants/function privilege boundaries tested for allowed and denied operations.
- [ ] Storage buckets and attachment permissions verified.
- [ ] Demo/import records and rejected references reconciled and verified.
- [ ] Permanent IDs, composite milestone relationships, transactional workflows and escalation verified.
- [ ] Project URL and publishable key identified for Stage 2; secrets excluded.

Stage 2 Kotlin/Gradle/auth/navigation/QR changes remain deferred.

## Current official references

- https://supabase.com/docs/guides/auth/managing-user-data
- https://supabase.com/docs/guides/database/postgres/row-level-security
- https://supabase.com/docs/guides/api/securing-your-api
- https://supabase.com/docs/guides/auth/auth-anonymous
- https://supabase.com/docs/guides/storage/security/access-control
- https://supabase.com/docs/guides/getting-started/api-keys
