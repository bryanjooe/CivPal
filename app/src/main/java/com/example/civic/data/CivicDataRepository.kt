package com.example.civic.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import com.example.civic.data.backend.*
import com.example.civic.data.models.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

enum class AppLanguage { ENGLISH, TAMIL }

/** Observable compatibility layer. Collections contain server results, never startup fixtures. */
object CivicDataRepository {
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
    var currentUser by mutableStateOf<User?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var working by mutableStateOf(false)
        private set
    var ready by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var lastRefresh by mutableStateOf<Long?>(null)
        private set
    var stale by mutableStateOf(true)
        private set
    var notificationsEnabled by mutableStateOf(true)
        private set
    var recoverPassword by mutableStateOf(false)
    val projects = mutableStateListOf<Project>()
    val complaints = mutableStateListOf<Complaint>()
    val conflicts = mutableStateListOf<ScheduleConflict>()
    val handovers = mutableStateListOf<OfficerHandover>()
    val users = mutableStateListOf<User>()
    val auditLogs = mutableStateListOf<AuditLogEntry>()
    val departments = mutableStateListOf<CivicCatalogEntry>()
    val jurisdictions = mutableStateListOf<CivicCatalogEntry>()
    val contractors = mutableStateListOf<CivicCatalogEntry>()
    val deadlineRequests = mutableStateListOf<CivicDeadlineRequest>()
    private var snapshot by mutableStateOf(CivicSnapshot(emptyMap()))
    private val backend = CivicBackend()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Mutex()
    private var identityGeneration = 0
    val isGuest get() = currentUser == null || currentUser?.email.isNullOrBlank()
    val managedProjects get() = projects.filter { p ->
        val row = snapshot.rows("projects").find { it.text("id") == p.id } ?: return@filter false
        when (currentUser?.role) {
            UserRole.ADMINISTRATOR -> true
            UserRole.CONTRACTOR -> row.text("contractor_id") == ownProfile()?.text("contractor_id")
            UserRole.OFFICER, UserRole.COORDINATOR -> hasScope(row.text("department_id"), row.text("responsible_office_id"))
            else -> false
        }
    }
    val assignedProjects get() = managedProjects.filter { p -> currentUser?.role != UserRole.OFFICER ||
        snapshot.rows("project_assignments").any { it.text("project_id") == p.id && it.text("officer_id") == currentUser?.id } }
    val publicProjects get() = projects.filter { p -> snapshot.rows("projects").any { it.text("id") == p.id && it.flag("is_published") } }
    val incomingHandovers get() = handovers.filter { h -> snapshot.rows("officer_handovers").any {
        it.text("id") == h.id && it.text("incoming_officer_id") == currentUser?.id } }
    private fun ownProfile() = snapshot.rows("profiles").find { it.text("id") == currentUser?.id }
    private fun hasScope(department: String, jurisdiction: String): Boolean {
        val ancestors = mutableSetOf<String>()
        var next: String? = jurisdiction
        while (next != null && ancestors.add(next)) next = jurisdictions.find { it.id == next }?.parentId
        return snapshot.rows("profile_scopes").any { it.text("profile_id") == currentUser?.id &&
            it.text("department_id") == department && it.text("jurisdiction_id") in ancestors }
    }
    fun clearError() { error = null }
    fun reportError(message: String) { error = message }
    fun toggleLanguage() {
        setLanguage(if (currentLanguage == AppLanguage.ENGLISH) AppLanguage.TAMIL else AppLanguage.ENGLISH)
    }
    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        if (currentUser != null) launchOperation { updatePreferences() }
    }
    /** Success callbacks run only after server success. Double taps are ignored while a write is pending. */
    fun launchOperation(action: suspend () -> Unit) {
        if (working) return
        working = true; error = null
        scope.launch {
            try { withTimeout(60_000) { action() } }
            catch (e: TimeoutCancellationException) { error = "Request timed out. Refresh to check whether it saved before retrying." }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = friendlyError(e) }
            finally { working = false }
        }
    }
    private fun friendlyError(e: Exception): String {
        if (e is IllegalArgumentException || e is IllegalStateException) return e.message ?: "Check your input."
        val message = e.message.orEmpty()
        val tamil = currentLanguage == AppLanguage.TAMIL
        return when {
            tamil && "Invalid login" in message -> "மின்னஞ்சல் அல்லது கடவுச்சொல் தவறு."
            tamil && "Email not confirmed" in message -> "உள்நுழையும் முன் மின்னஞ்சலை உறுதிப்படுத்தவும்."
            tamil && ("42501" in message || "403" in message) -> "இந்தச் செயலைச் செய்ய உங்கள் கணக்கிற்கு அனுமதி இல்லை."
            tamil && "401" in message -> "அமர்வு காலாவதியானது. மீண்டும் உள்நுழையவும்."
            tamil && "429" in message -> "அதிக கோரிக்கைகள். சிறிது நேரம் கழித்து முயற்சிக்கவும்."
            tamil -> "கோரிக்கையை முடிக்க முடியவில்லை. இணைய இணைப்பைச் சரிபார்த்து மீண்டும் முயற்சிக்கவும்."
            "Invalid login" in message -> "Invalid email or password."
            "Email not confirmed" in message -> "Confirm your email before signing in."
            "42501" in message || "403" in message -> "Your account is not authorized for this operation."
            "401" in message -> "Your session has expired. Sign in again."
            "429" in message -> "Too many requests. Please try again later."
            else -> "Unable to complete the request. Check your connection and retry."
        }
    }
    suspend fun initialize() {
        if (ready) return
        try { withTimeout(45_000) { backend.client.auth.awaitInitialization(); refresh() } }
        catch (e: CancellationException) { if (e !is TimeoutCancellationException) throw e; error = "Connection timed out. Tap Refresh to retry." }
        catch (e: Exception) { error = friendlyError(e) }
        finally { ready = true }
    }
    suspend fun refresh() = lock.withLock {
        loading = true
        val generation = identityGeneration
        try {
            val next = backend.snapshot()
            if (generation != identityGeneration) return@withLock
            val authId = backend.client.auth.currentUserOrNull()?.id
            val profile = next.rows("profiles").find { it.text("id") == authId }
            if (authId != null && (profile == null || !profile.flag("is_active"))) {
                clearIdentity()
                error("Your profile is unavailable or inactive. Sign out and contact an administrator.")
            }
            val p = next.rows("projects").map(next::project)
            val c = next.rows("complaints").map(next::complaint)
            val f = next.rows("project_conflicts").map(next::conflict)
            val h = next.rows("officer_handovers").map(next::handover)
            val u = next.rows("profiles").map(next::user)
            val a = next.rows("audit_logs").sortedByDescending { it.text("event_at") }.map(next::audit)
            Snapshot.withMutableSnapshot {
                if (currentUser?.id != authId && profile != null) {
                    currentLanguage = if (profile.text("preferred_language") == "TAMIL") AppLanguage.TAMIL else AppLanguage.ENGLISH
                }
                snapshot = next
                projects.replace(p); complaints.replace(c); conflicts.replace(f); handovers.replace(h)
                users.replace(u); auditLogs.replace(a)
                departments.replace(next.catalog("departments")); jurisdictions.replace(next.catalog("jurisdictions"))
                contractors.replace(next.catalog("contractors"))
                deadlineRequests.replace(next.rows("deadline_revision_requests").map(next::deadline))
                currentUser = profile?.let(next::user)
                notificationsEnabled = profile?.flag("notifications_enabled") ?: true
                lastRefresh = System.currentTimeMillis(); stale = false; error = null
            }
        } catch (e: Exception) { stale = true; throw e }
        finally { loading = false }
    }
    private fun clearIdentity() {
        identityGeneration++; snapshot = CivicSnapshot(emptyMap())
        currentUser = null; projects.clear(); complaints.clear(); conflicts.clear(); handovers.clear()
        users.clear(); auditLogs.clear(); deadlineRequests.clear(); lastRefresh = null; stale = true
    }
    suspend fun login(email: String, password: String) {
        clearIdentity(); backend.client.auth.clearSession(); backend.login(email, password)
        try { refresh() } catch (e: Exception) { backend.client.auth.clearSession(); clearIdentity(); throw e }
    }
    suspend fun logout() {
        clearIdentity()
        try { backend.client.auth.signOut() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { /* Clear local session even offline. */ }
        finally { backend.client.auth.clearSession() }
    }
    suspend fun resetPassword(email: String) {
        require(email.contains('@')) { "Enter your account email." }
        backend.client.auth.resetPasswordForEmail(email.trim(), redirectUrl = "civic://auth-callback")
    }
    suspend fun setPassword(password: String) {
        require(password.length >= 8) { "Use a password of at least 8 characters." }
        backend.client.auth.updateUser { this.password = password }
        recoverPassword = false
    }
    suspend fun signUp(email: String, password: String) {
        require(email.contains('@') && password.length >= 8) { "Enter an email and a password of at least 8 characters." }
        backend.client.auth.signUpWith(Email) { this.email = email.trim(); this.password = password }
    }
    suspend fun continueAsGuest() { if (currentUser != null) logout(); refresh() }
    private suspend fun requireCitizen() {
        if (backend.client.auth.currentUserOrNull() == null) { backend.client.auth.signInAnonymously(); refresh() }
        check(currentUser?.role == UserRole.CITIZEN) { "Use a citizen account for this action." }
    }
    fun evaluateComplaintDeadlines(currentTimeMillis: Long = System.currentTimeMillis()) {
        // The database scheduler owns escalation, event history and deadlines.
    }
    suspend fun findProject(id: String): Project? {
        val row = backend.rows("projects", id).singleOrNull()
        refresh(); return row?.let { projects.find { project -> project.id == it.text("id") } }
    }
    private suspend fun mutate(name: String, params: JsonObject): String? {
        val result = backend.rpc(name, params)
        try { refresh() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = "Saved on the server, but refresh failed. Tap Refresh to load the latest data." }
        return result
    }
    suspend fun toggleProjectFollow(projectId: String) {
        requireCitizen()
        mutate("set_follow", arguments("p_project" to projectId, "p_follow" to (projects.find { it.id == projectId }?.isFollowed != true)))
    }
    suspend fun submitComplaint(category: ComplaintCategory, projectId: String?, projectTitle: String?, location: String,
        district: String, description: String, photoUri: String?, citizenName: String): Complaint {
        requireCitizen()
        val jurisdiction = if (projectId == null) jurisdictions.singleOrNull { it.kind == "MUNICIPALITY" && it.name == district }?.id else null
        check(projectId != null || jurisdiction != null) { "Select a project or a specific municipality for complaint routing." }
        val id = checkNotNull(mutate("submit_complaint", arguments("p_category" to category.name, "p_location" to location,
            "p_description" to description, "p_project" to projectId, "p_jurisdiction" to jurisdiction)))
        return complaints.find { it.id == id } ?: backend.rows("complaints", id).single().let(snapshot::complaint)
    }
    suspend fun respondToComplaint(complaintId: String, decision: ComplaintStatus, responseText: String,
        officerName: String, officerRole: String = "Government Officer") {
        mutate("respond_complaint", arguments("p_complaint" to complaintId, "p_status" to decision.name, "p_message" to responseText))
    }
    suspend fun requestCitizenInformation(complaintId: String, requestText: String, officerName: String) =
        respondToComplaint(complaintId, ComplaintStatus.AWAITING_CITIZEN_INFO, requestText, officerName)
    suspend fun replyComplaint(id: String, message: String) { mutate("reply_complaint", arguments("p_complaint" to id, "p_message" to message)) }
    suspend fun registerProject(title: String, description: String, department: String, district: String, municipalBody: String,
        location: String, contractorName: String, responsibleOffice: String, officerInCharge: String, budgetLakhs: Double,
        fundingSource: String, startDate: String, completionDate: String, latitude: Double, longitude: Double): String {
        fun catalogId(items: List<CivicCatalogEntry>, name: String) = checkNotNull(items.singleOrNull { it.active && it.name == name }) {
            "Choose a registered catalog entry: $name" }.id
        val start = CivicDates.input(startDate); val end = CivicDates.input(completionDate)
        val data = arguments("title" to title, "description" to description, "department_id" to catalogId(departments, department),
            "district" to district, "municipal_body_id" to catalogId(jurisdictions.filter { it.kind == "MUNICIPALITY" }, municipalBody),
            "location" to location, "latitude" to latitude, "longitude" to longitude,
            "contractor_id" to catalogId(contractors, contractorName),
            "responsible_office_id" to catalogId(jurisdictions.filter { it.kind == "OFFICE" }, responsibleOffice),
            "estimated_budget_in_lakhs" to budgetLakhs, "funding_source" to fundingSource, "start_date" to start, "original_completion_date" to end)
        val milestones = JsonArray(listOf(arguments("milestone_id" to "M1", "title" to "Initial Work Site Preparation & Clearance", "target_date" to start),
            arguments("milestone_id" to "M2", "title" to "Core Infrastructure Laying", "target_date" to end)))
        return checkNotNull(mutate("register_project", arguments("p_data" to data, "p_milestones" to milestones)))
    }
    suspend fun publishProject(id: String, publish: Boolean) { mutate("publish_project", arguments("p_project" to id, "p_publish" to publish)) }
    fun isPublished(id: String) = snapshot.rows("projects").find { it.text("id") == id }?.flag("is_published") == true
    suspend fun recordDelay(projectId: String, reason: String, revisedDate: String, explanation: String, officerName: String) {
        mutate("record_delay", arguments("p_project" to projectId, "p_date" to CivicDates.input(revisedDate), "p_reason" to reason, "p_explanation" to explanation))
    }
    suspend fun submitProgressReport(projectId: String, milestoneId: String, submittedBy: String, completionPct: Int,
        workDone: String, remainingWork: String, obstacles: String?): String = checkNotNull(mutate("submit_progress",
        arguments("p_project" to projectId, "p_milestone" to milestoneId, "p_percentage" to completionPct,
            "p_completed" to workDone, "p_remaining" to remainingWork, "p_obstacles" to obstacles)))
    suspend fun verifyProgressReport(projectId: String, reportId: String, approve: Boolean, officerNotes: String, officerName: String) {
        mutate("review_progress", arguments("p_report" to reportId, "p_decision" to if (approve) "Approved" else "Correction Requested",
            "p_notes" to officerNotes, "p_publish" to approve))
    }
    suspend fun requestDeadline(project: String, date: String, reason: String): String = checkNotNull(
        mutate("request_deadline", arguments("p_project" to project, "p_date" to CivicDates.input(date), "p_reason" to reason)))
    suspend fun reviewDeadline(request: String, approve: Boolean, notes: String) {
        mutate("review_deadline", arguments("p_request" to request, "p_approve" to approve, "p_notes" to notes))
    }
    suspend fun initiateHandover(outgoingName: String, outgoingEmail: String, incomingName: String, incomingEmail: String,
        department: String, jurisdiction: String, transferDate: String, reason: String, notes: String): String {
        val incoming = users.singleOrNull { it.email.equals(incomingEmail, true) && it.role == UserRole.OFFICER }
        val incomingId = incoming?.id ?: incomingEmail.takeIf { runCatching { java.util.UUID.fromString(it) }.isSuccess }
        checkNotNull(incomingId) { "Enter the incoming officer's verified user UUID (available from the administrator)." }
        val selected = assignedProjects.filter { it.responsibleOffice == jurisdiction && it.department == department }
        check(selected.isNotEmpty()) { "No projects assigned to you in this office." }
        val selectedIds = selected.map { it.id }.toSet()
        val tasks = selected.flatMap { project -> project.progressReports.filter { it.status == "Pending Verification" }.map { report ->
            arguments("project_id" to project.id, "title" to "Verify progress report ${report.id}",
                "priority" to "Medium", "due_date" to CivicDates.input(transferDate))
        } } + snapshot.rows("complaints").filter { it.text("project_id") in selectedIds &&
            it.text("status") !in listOf("RESOLVED", "REJECTED") }.map { complaint ->
            arguments("project_id" to complaint.text("project_id"), "title" to "Review complaint ${complaint.text("id")}",
                "priority" to if (complaint.text("priority") == "HIGH") "High" else "Medium",
                "due_date" to complaint.text("deadline_at").take(10))
        }
        require(tasks.size <= 100) { "This office has more than 100 pending tasks. Complete or split the handover before submitting." }
        return checkNotNull(mutate("initiate_handover", arguments("p_incoming" to incomingId, "p_projects" to selected.map { it.id },
            "p_effective" to CivicDates.input(transferDate), "p_reason" to reason, "p_notes" to notes, "p_tasks" to JsonArray(tasks))))
    }
    suspend fun acknowledgeHandover(handoverId: String, incomingName: String) { answerHandover(handoverId, true, null) }
    suspend fun answerHandover(id: String, acknowledge: Boolean, clarification: String?) {
        mutate("answer_handover", arguments("p_handover" to id, "p_acknowledge" to acknowledge, "p_clarification" to clarification))
    }
    suspend fun resolveConflict(conflictId: String, resolutionText: String, coordinatorName: String) {
        mutate("resolve_conflict", arguments("p_conflict" to conflictId, "p_resolution" to resolutionText))
    }
    suspend fun recordConflict(a: String, b: String, type: ConflictType, location: String, dates: String, resolution: String?) {
        mutate("record_conflict", arguments("p_a" to a, "p_b" to b, "p_type" to type.name,
            "p_location" to location, "p_dates" to dates, "p_resolution" to resolution))
    }
    suspend fun updatePreferences(notifications: Boolean = notificationsEnabled) {
        val user = currentUser ?: return
        mutate("update_my_profile", arguments("p_name" to user.name, "p_language" to currentLanguage.name, "p_notifications" to notifications))
    }
    suspend fun setUser(user: User, role: UserRole = user.role, active: Boolean = user.isActive) {
        val row = snapshot.rows("profiles").single { it.text("id") == user.id }
        check(role == user.role) { "Use the authorization form to change a role." }
        val oldScopes = snapshot.rows("profile_scopes").filter { it.text("profile_id") == user.id }
        mutate("admin_set_user", arguments("p_user" to user.id, "p_role" to role.name, "p_active" to active,
            "p_department" to row.optional("department_id"), "p_office" to row.optional("office_jurisdiction_id"),
            "p_contractor" to if (role == UserRole.CONTRACTOR) row.optional("contractor_id") else null))
        if (active && role in listOf(UserRole.OFFICER, UserRole.COORDINATOR)) {
            oldScopes.forEach { backend.rpc("admin_set_scope", arguments("p_user" to user.id,
                "p_department" to it.text("department_id"), "p_jurisdiction" to it.text("jurisdiction_id"), "p_grant" to true)) }
            refresh()
        }
    }
    fun profileIds(user: User): Triple<String, String, String> {
        val row = snapshot.rows("profiles").find { it.text("id") == user.id }
        return Triple(row?.text("department_id").orEmpty(), row?.text("office_jurisdiction_id").orEmpty(), row?.text("contractor_id").orEmpty())
    }
    suspend fun authorizeUser(user: User, role: UserRole, department: String?, office: String?, contractor: String?) {
        mutate("admin_set_user", arguments("p_user" to user.id, "p_role" to role.name, "p_active" to user.isActive,
            "p_department" to department, "p_office" to office, "p_contractor" to contractor))
    }
    suspend fun grantScope(user: User, department: String, jurisdiction: String, grant: Boolean) {
        mutate("admin_set_scope", arguments("p_user" to user.id, "p_department" to department,
            "p_jurisdiction" to jurisdiction, "p_grant" to grant))
    }
    suspend fun addUser(name: String, email: String, role: UserRole, department: String?, jurisdiction: String?) {
        backend.invite(name, email); refresh()
    }
    suspend fun uploadAttachment(context: Context, uri: String?, bucket: String, record: String) {
        if (uri == null) return
        val file = withContext(Dispatchers.IO) {
            val parsed = Uri.parse(uri)
            val type = context.contentResolver.getType(parsed) ?: error("Unable to determine file type.")
            val bytes = context.contentResolver.openInputStream(parsed)?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer); if (count < 0) break
                    output.write(buffer, 0, count)
                    require(output.size() <= 10 * 1024 * 1024) { "Attachment must be 10 MB or smaller." }
                }
                output.toByteArray()
            } ?: error("Unable to read the selected file.")
            bytes to type
        }
        try { backend.attach(bucket, record, file.first, file.second); refresh() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            throw IllegalStateException("Record $record is saved. Its attachment could not be completed. Check this record before retrying; keep this screen open to retry the attachment.", e)
        }
    }
    suspend fun validateAttachment(context: Context, uri: String?, pdf: Boolean = false) {
        if (uri == null) return
        withContext(Dispatchers.IO) {
            val parsed = Uri.parse(uri)
            val allowed = if (pdf) setOf("application/pdf") else setOf("image/jpeg", "image/png", "image/webp")
            require(context.contentResolver.getType(parsed) in allowed) { "Choose a supported JPEG, PNG, WebP image or PDF document." }
            context.contentResolver.openInputStream(parsed)?.use { stream ->
                var size = 0L
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer); if (count < 0) break
                    size += count
                    require(size <= 10 * 1024 * 1024) { "Attachment must be 10 MB or smaller." }
                }
                require(size > 0) { "The selected file is empty." }
            } ?: error("Unable to read the selected file.")
        }
    }
    suspend fun image(bucket: String, path: String): ByteArray = backend.client.storage.from(bucket).downloadAuthenticated(path)
    private fun <T> MutableList<T>.replace(items: List<T>) { clear(); addAll(items) }
}
