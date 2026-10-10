package com.example.civic

import com.example.civic.data.backend.*
import com.example.civic.data.models.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/** Database-owned workflows are covered by supabase/tests/check_backend.mjs.
 * These checks validate the Android adapter against actual public response shapes. */
class CivicRepositoryTest {
    private val fixture: CivicSnapshot get() {
        val json = checkNotNull(javaClass.getResource("/civic-public-fixture.json")).readText()
        return CivicSnapshot(Json.parseToJsonElement(json).jsonObject.mapValues { (_, value) -> value.jsonArray.map { it.jsonObject } })
    }
    private fun row(vararg entries: Pair<String, Any?>) = arguments(*entries)
    @Test fun permanentProjectIdsAndNumericFieldsSurviveDecoding() {
        val projects = fixture.rows("projects").map(fixture::project)
        assertEquals(setOf("TN-PWD-2025-01", "TN-TWAD-2025-02"), projects.map { it.id }.toSet())
        assertEquals(68, projects.first { it.id == "TN-PWD-2025-01" }.progressPercentage)
        assertTrue(projects.all { it.latitude != 0.0 && it.longitude != 0.0 && it.estimatedBudgetInLakhs > 0.0 })
    }
    @Test fun milestonesAreJoinedByProjectAndMilestoneId() {
        val p = fixture.rows("projects").first()
        val snapshot = CivicSnapshot(fixture.tables + ("project_milestones" to listOf(
            row("project_id" to p.text("id"), "milestone_id" to "M1", "title" to "Correct", "target_date" to "2026-11-01"),
            row("project_id" to "ANOTHER-ID", "milestone_id" to "M1", "title" to "Other", "target_date" to "2026-11-01"))))
        assertEquals(listOf("Correct"), snapshot.project(p).milestones.map { it.title })
    }
    @Test fun followingComesFromCurrentUsersJoinRecords() {
        val p = fixture.rows("projects").first()
        assertFalse(fixture.project(p).isFollowed)
        val following = CivicSnapshot(fixture.tables + ("project_follows" to listOf(row("project_id" to p.text("id")))))
        assertTrue(following.project(p).isFollowed)
    }
    @Test fun serverProfileDeterminesAllFiveRoles() {
        UserRole.entries.forEach { role ->
            val user = fixture.user(row("id" to "actual-auth-uuid", "name" to "Account", "email" to "test@example.com",
                "role" to role.name, "is_active" to true, "is_demo" to false))
            assertEquals(role, user.role); assertEquals("actual-auth-uuid", user.id); assertTrue(user.isActive)
        }
    }
    @Test fun timestampsPreserveExact168HourDeadlineAndOffset() {
        val start = CivicDates.millis("2026-10-01T10:00:00+05:30")
        val end = CivicDates.millis("2026-10-08T10:00:00+05:30")
        assertEquals(168 * 60 * 60 * 1000L, end - start)
        assertEquals(CivicDates.millis("2026-10-01T04:30:00Z"), start)
    }
    @Test fun dateInputsAcceptExistingDisplayFormatAndIso() {
        assertEquals("2026-11-15", CivicDates.input("15 Nov 2026"))
        assertEquals("2026-11-15", CivicDates.input("2026-11-15"))
        assertEquals("15 Nov 2026", CivicDates.display("2026-11-15"))
    }
    @Test fun invalidDateIsRejectedBeforeSendingRpc() {
        assertThrows(java.time.format.DateTimeParseException::class.java) { CivicDates.input("tomorrow") }
    }
    @Test fun absentInternalReviewNeverBecomesInventedOfficerFeedback() {
        val report = row("id" to "PR-1", "project_id" to "TN-PWD-2025-01", "milestone_id" to "M1",
            "status" to "Approved", "completion_percentage" to 65, "work_completed" to "Work", "remaining_work" to "More")
        assertNull(fixture.report(report).officerNotes)
        val official = CivicSnapshot(fixture.tables + ("progress_report_reviews" to listOf(row("report_id" to "PR-1", "officer_notes" to "Internal"))))
        assertEquals("Internal", official.report(report).officerNotes)
    }
    @Test fun handoverCountsComeFromActualJoinedRecords() {
        val handoverRow = row("id" to "HO-1", "status" to "INITIATED", "legacy_assigned_projects_count" to 99, "legacy_pending_tasks_count" to 88)
        val snapshot = CivicSnapshot(mapOf("handover_projects" to listOf(row("handover_id" to "HO-1", "project_id" to "P1"))))
        assertEquals(1, snapshot.handover(handoverRow).assignedProjectsCount)
        assertEquals(0, snapshot.handover(handoverRow).pendingTasksCount)
    }
    @Test fun rpcArgumentsKeepNullsBooleansNumbersAndArraysTyped() {
        val args = arguments("p_notes" to null, "p_approve" to true, "p_percentage" to 65, "p_projects" to listOf("P1", "P2"))
        assertEquals(JsonNull, args["p_notes"])
        assertTrue(args.getValue("p_approve").jsonPrimitive.boolean)
        assertEquals(65, args.getValue("p_percentage").jsonPrimitive.int)
        assertEquals(2, args.getValue("p_projects").jsonArray.size)
    }
}
