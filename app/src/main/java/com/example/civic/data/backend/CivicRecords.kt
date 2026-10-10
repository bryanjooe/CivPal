package com.example.civic.data.backend

import com.example.civic.data.models.*
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** DTO access is isolated here; SQL column names never leak into the existing UI models. */
internal fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
internal fun JsonObject.optional(key: String) = this[key]?.jsonPrimitive?.contentOrNull
internal fun JsonObject.flag(key: String) = this[key]?.jsonPrimitive?.booleanOrNull ?: false
internal fun JsonObject.number(key: String) = text(key).toDoubleOrNull() ?: 0.0
internal fun JsonObject.integer(key: String) = text(key).toIntOrNull() ?: 0

object CivicDates {
    private val display = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    fun input(value: String): String = runCatching { LocalDate.parse(value).toString() }
        .getOrElse { LocalDate.parse(value, display).toString() }
    fun display(value: String): String = if (value.isBlank()) "" else
        runCatching { LocalDate.parse(value.take(10)).format(display) }.getOrDefault(value)
    fun millis(value: String): Long = runCatching { Instant.parse(value).toEpochMilli() }
        .getOrElse { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
}

data class CivicCatalogEntry(val id: String, val name: String, val details: String,
    val active: Boolean, val code: String = "", val kind: String = "", val parentId: String? = null)
data class CivicDeadlineRequest(val id: String, val projectId: String, val proposedDate: String,
    val reason: String, val status: String, val documentPath: String?, val notes: String?)

internal data class CivicSnapshot(val tables: Map<String, List<JsonObject>>) {
    fun rows(table: String) = tables[table].orEmpty()
    fun deadline(row: JsonObject) = CivicDeadlineRequest(row.text("id"), row.text("project_id"),
        CivicDates.display(row.text("proposed_completion_date")), row.text("reason"), row.text("status"),
        row.optional("document_path"), row.optional("reviewer_notes"))
    fun user(row: JsonObject) = User(row.text("id"), row.text("name"), row.text("email"),
        UserRole.valueOf(row.text("role")),
        rows("departments").find { it.text("id") == row.text("department_id") }?.text("name"),
        rows("jurisdictions").find { it.text("id") == row.text("office_jurisdiction_id") }?.text("name"),
        row.flag("is_demo"), row.flag("is_active"))
    fun catalog(table: String) = rows(table).map { CivicCatalogEntry(it.text("id"), it.text("name"),
        it.text(if (table == "departments") "scope_description" else "details"), it.flag("is_active"),
        it.text("code"), it.text("kind"), it.optional("parent_id")) }
    fun project(row: JsonObject): Project {
        val id = row.text("id")
        val follows = rows("project_follows").any { it.text("project_id") == id }
        return Project(id, row.text("title"), row.text("description"), row.text("department_name"),
            row.text("district"), row.text("municipal_body"), row.text("location"), row.number("latitude"),
            row.number("longitude"), row.text("contractor_name"), row.text("contractor_id"),
            row.text("responsible_office"), row.text("current_officer_in_charge"),
            row.number("estimated_budget_in_lakhs"), row.text("funding_source"),
            CivicDates.display(row.text("start_date")), CivicDates.display(row.text("original_completion_date")),
            CivicDates.display(row.text("revised_completion_date")), ProjectStatus.valueOf(row.text("status")),
            row.integer("progress_percentage"),
            rows("project_milestones").filter { it.text("project_id") == id }.map {
                Milestone(it.text("milestone_id"), it.text("title"), CivicDates.display(it.text("target_date")),
                    it.flag("is_completed"), it.optional("completion_date")?.let(CivicDates::display)) },
            rows("progress_reports").filter { it.text("project_id") == id }.map(::report),
            rows("delay_records").filter { it.text("project_id") == id }.map {
                DelayRecord(it.text("id"), id, CivicDates.display(it.text("recorded_at")), it.text("reason"),
                    CivicDates.display(it.text("original_completion_date")), CivicDates.display(it.text("revised_completion_date")),
                    it.text("published_explanation"), it.text("recorded_by_name")) }, follows, row.flag("is_demo"))
    }
    fun report(row: JsonObject) = ProgressReport(row.text("id"), row.text("project_id"),
        row.text("milestone_id"), row.text("submitted_by_name"), CivicDates.display(row.text("submitted_at")),
        row.integer("completion_percentage"), row.text("work_completed"), row.text("remaining_work"),
        row.optional("obstacles"), row.optional("photo_path"), row.text("status"),
        rows("progress_report_reviews").find { it.text("report_id") == row.text("id") }?.optional("officer_notes"))
    fun complaint(row: JsonObject): Complaint {
        val id = row.text("id")
        return Complaint(id, ComplaintCategory.valueOf(row.text("category")), row.optional("project_id"),
            row.optional("project_title_snapshot"), row.text("location"), row.text("district"), row.text("description"),
            row.optional("photo_path"), row.text("submitted_by_citizen_name"), CivicDates.display(row.text("submitted_at")),
            CivicDates.millis(row.text("submitted_at")), CivicDates.millis(row.text("deadline_at")),
            row.text("assigned_department_name"), row.optional("assigned_officer_name"), row.optional("assigned_officer_email"),
            ComplaintStatus.valueOf(row.text("status")), ComplaintPriority.valueOf(row.text("priority")),
            CivicDates.display(row.text("updated_at")),
            rows("complaint_responses").filter { it.text("complaint_id") == id }.sortedBy { it.text("responded_at") }.map {
                ComplaintResponse(it.text("id"), CivicDates.display(it.text("responded_at")), it.text("responded_by_name"),
                    it.text("role_snapshot"), it.text("message")) },
            rows("complaint_events").filter { it.text("complaint_id") == id }.sortedBy { it.text("event_at") }.map {
                ComplaintEvent(it.text("id"), CivicDates.millis(it.text("event_at")), CivicDates.display(it.text("event_at")),
                    it.text("event_type"), it.text("description"), it.text("performed_by_name")) })
    }
    fun conflict(row: JsonObject) = ScheduleConflict(row.text("id"), row.text("project_a_id"),
        row.text("project_a_title_snapshot"), row.text("department_a_snapshot"), row.text("project_b_id"),
        row.text("project_b_title_snapshot"), row.text("department_b_snapshot"), row.text("location"),
        row.text("conflicting_dates"), ConflictType.valueOf(row.text("conflict_type")),
        ConflictStatus.valueOf(row.text("status")), row.optional("proposed_resolution"),
        row.optional("resolved_at")?.let(CivicDates::display))
    fun handover(row: JsonObject): OfficerHandover {
        val id = row.text("id")
        val tasks = rows("handover_pending_tasks").filter { it.text("handover_id") == id }.map {
            PendingTask(it.text("id"), it.text("title"), it.text("related_project_id"), it.text("priority"),
                CivicDates.display(it.text("due_date"))) }
        return OfficerHandover(id, row.text("outgoing_officer_name"), row.text("outgoing_officer_email"),
            row.text("incoming_officer_name"), row.text("incoming_officer_email"), row.text("department_name"),
            row.text("office_jurisdiction"), CivicDates.display(row.text("effective_transfer_date")),
            row.text("reason_for_reassignment"), rows("handover_projects").count { it.text("handover_id") == id },
            tasks.size, tasks, row.text("handover_notes"), HandoverStatus.valueOf(row.text("status")),
            row.optional("acknowledged_at")?.let(CivicDates::display), row.optional("clarification_notes"))
    }
    fun audit(row: JsonObject) = AuditLogEntry(row.text("id"), row.text("event_at"), row.text("action"),
        row.text("performed_by_name"), row.text("target_type"), row.text("target_id"), row.text("details"))
}
