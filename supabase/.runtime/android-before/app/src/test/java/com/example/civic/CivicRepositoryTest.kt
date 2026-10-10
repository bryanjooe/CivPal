package com.example.civic

import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.*
import org.junit.Assert.*
import org.junit.Test

class CivicRepositoryTest {

    @Test
    fun test7DayDeadlineCalculatedOnSubmission() {
        val complaint = CivicDataRepository.submitComplaint(
            category = ComplaintCategory.POTHOLE,
            projectId = "TN-PWD-2025-01",
            projectTitle = "Anna Salai Drain",
            location = "Anna Salai, Chennai",
            district = "Chennai",
            description = "Test pothole issue",
            photoUri = null,
            citizenName = "Test Citizen"
        )

        val expectedDeadline = complaint.submissionTimestampMillis + (7 * 24 * 60 * 60 * 1000L)
        assertEquals("Deadline must be exactly 7 calendar days after submission", expectedDeadline, complaint.deadlineTimestampMillis)
        assertEquals(ComplaintStatus.PENDING, complaint.status)
        assertEquals(ComplaintPriority.NORMAL, complaint.priority)
    }

    @Test
    fun testComplaintWithTimeRemainingNotEscalated() {
        val now = System.currentTimeMillis()
        CivicDataRepository.evaluateComplaintDeadlines(now)

        val pendingComplaint = CivicDataRepository.complaints.find { it.id == "CMP-2026-101" }
        assertNotNull(pendingComplaint)
        assertNotEquals(ComplaintStatus.ESCALATED_OVERDUE, pendingComplaint?.status)
    }

    @Test
    fun testExactAtDeadlineNotMarkedOverdueBeforePassed() {
        val now = System.currentTimeMillis()
        val newComplaint = CivicDataRepository.submitComplaint(
            category = ComplaintCategory.WATER_LEAK,
            projectId = null,
            projectTitle = null,
            location = "Test Location",
            district = "Chennai",
            description = "Test Description",
            photoUri = null,
            citizenName = "Tester"
        )

        // Evaluate at 1 second before deadline
        val timeBeforeDeadline = newComplaint.deadlineTimestampMillis - 1000L
        CivicDataRepository.evaluateComplaintDeadlines(timeBeforeDeadline)

        val rechecked = CivicDataRepository.complaints.find { it.id == newComplaint.id }!!
        assertNotEquals("Should not be marked overdue before deadline has strictly passed", ComplaintStatus.ESCALATED_OVERDUE, rechecked.status)
    }

    @Test
    fun testUnansweredComplaintBecomesOverdueAfterDeadline() {
        val newComplaint = CivicDataRepository.submitComplaint(
            category = ComplaintCategory.DRAINAGE_OVERFLOW,
            projectId = null,
            projectTitle = null,
            location = "Test Location Overdue",
            district = "Madurai",
            description = "Test Overdue Description",
            photoUri = null,
            citizenName = "Tester Overdue"
        )

        val timeAfterDeadline = newComplaint.deadlineTimestampMillis + 1000L
        CivicDataRepository.evaluateComplaintDeadlines(timeAfterDeadline)

        val escalated = CivicDataRepository.complaints.find { it.id == newComplaint.id }!!
        assertEquals(ComplaintStatus.ESCALATED_OVERDUE, escalated.status)
        assertEquals(ComplaintPriority.HIGH, escalated.priority)
    }

    @Test
    fun testNoDuplicateEscalationEventsOnRepeatedEvaluations() {
        val complaint = CivicDataRepository.complaints.find { it.id == "CMP-2026-103" }!!

        // Evaluate multiple times
        CivicDataRepository.evaluateComplaintDeadlines(System.currentTimeMillis() + (10 * 24 * 60 * 60 * 1000L))
        CivicDataRepository.evaluateComplaintDeadlines(System.currentTimeMillis() + (12 * 24 * 60 * 60 * 1000L))

        val rechecked = CivicDataRepository.complaints.find { it.id == "CMP-2026-103" }!!
        val escalationEvents = rechecked.eventHistory.count { it.eventType == "Escalated — Overdue" }
        assertEquals("Escalated event must appear exactly once without duplicates", 1, escalationEvents)
    }

    @Test
    fun testComplaintAnsweredBeforeDeadlineNotEscalated() {
        val complaint = CivicDataRepository.complaints.find { it.id == "CMP-2026-104" }!!
        assertEquals(ComplaintStatus.RESOLVED, complaint.status)

        CivicDataRepository.evaluateComplaintDeadlines(System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L))

        val rechecked = CivicDataRepository.complaints.find { it.id == "CMP-2026-104" }!!
        assertEquals("Answered complaint must remain Resolved and not be escalated", ComplaintStatus.RESOLVED, rechecked.status)
    }

    @Test
    fun testLateResponseDoesNotEraseOverdueHistory() {
        val complaint = CivicDataRepository.complaints.find { it.id == "CMP-2026-105" }!!
        assertEquals(ComplaintStatus.RESOLVED, complaint.status)

        val hasEscalationHistory = complaint.eventHistory.any { it.eventType == "Escalated — Overdue" }
        assertTrue("Late response must preserve the historical Escalated — Overdue event in audit trail", hasEscalationHistory)
    }

    @Test
    fun testUnassignedComplaintFlaggedForAssignment() {
        val unassigned = CivicDataRepository.complaints.find { it.id == "CMP-2026-106" }!!
        assertNull(unassigned.assignedOfficerName)
        assertEquals(ComplaintStatus.ESCALATED_OVERDUE, unassigned.status)
        assertEquals("Unassigned — Escalation Required", unassigned.lastUpdatedDate)
    }

    @Test
    fun testOfficerHandoverContinuity() {
        val handover = CivicDataRepository.initiateHandover(
            outgoingName = "Er. S. Sundaram",
            outgoingEmail = "officer.sundaram@pwd.tn.gov.in",
            incomingName = "Er. R. Kanimozhi",
            incomingEmail = "officer.kanimozhi@pwd.tn.gov.in",
            department = "Public Works Department",
            jurisdiction = "Highways & PWD Chennai Metro Division",
            transferDate = "15 Mar 2025",
            reason = "Routine Transfer",
            notes = "Test handover notes"
        )

        CivicDataRepository.acknowledgeHandover(handover.id, "Er. R. Kanimozhi")
        val project = CivicDataRepository.projects.find { it.id == "TN-PWD-2025-01" }
        assertNotNull(project)
        assertEquals("TN-PWD-2025-01", project?.id)
        assertTrue(project?.currentOfficerInCharge?.contains("Er. R. Kanimozhi") == true)
    }
}
