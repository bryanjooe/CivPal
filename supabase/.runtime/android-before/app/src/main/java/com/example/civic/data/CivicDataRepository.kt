package com.example.civic.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.civic.data.models.*
import java.text.SimpleDateFormat
import java.util.*

enum class AppLanguage {
    ENGLISH, TAMIL
}

object CivicDataRepository {

    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
    var currentUser by mutableStateOf<User?>(null)

    // In-memory observable collections
    val projects = mutableStateListOf<Project>()
    val complaints = mutableStateListOf<Complaint>()
    val conflicts = mutableStateListOf<ScheduleConflict>()
    val handovers = mutableStateListOf<OfficerHandover>()
    val users = mutableStateListOf<User>()
    val auditLogs = mutableStateListOf<AuditLogEntry>()

    init {
        preloadSampleData()
        evaluateComplaintDeadlines()
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.ENGLISH) AppLanguage.TAMIL else AppLanguage.ENGLISH
    }

    // Reusable deadline calculation and overdue detection engine
    fun evaluateComplaintDeadlines(currentTimeMillis: Long = System.currentTimeMillis()) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        val dateStr = dateFormat.format(Date(currentTimeMillis))

        complaints.indices.forEach { index ->
            val c = complaints[index]
            val isUnresolved = c.status == ComplaintStatus.PENDING ||
                    c.status == ComplaintStatus.UNDER_REVIEW ||
                    c.status == ComplaintStatus.AWAITING_CITIZEN_INFO

            if (isUnresolved && currentTimeMillis > c.deadlineTimestampMillis) {
                // Prevent duplicate escalation events in history
                val alreadyHasEscalation = c.eventHistory.any { it.eventType == "Escalated — Overdue" }
                val newHistory = if (!alreadyHasEscalation) {
                    c.eventHistory + ComplaintEvent(
                        id = "EVT-ESC-${c.id}",
                        timestampMillis = c.deadlineTimestampMillis,
                        dateString = dateFormat.format(Date(c.deadlineTimestampMillis)),
                        eventType = "Escalated — Overdue",
                        description = "7-day official response deadline passed without a qualifying recorded response. Complaint automatically escalated to High Priority.",
                        performedBy = "System Escalation Engine"
                    )
                } else c.eventHistory

                complaints[index] = c.copy(
                    status = ComplaintStatus.ESCALATED_OVERDUE,
                    priority = ComplaintPriority.HIGH,
                    eventHistory = newHistory
                )

                addAuditLog(
                    action = "Complaint Auto-Escalated",
                    performedBy = "System Engine",
                    targetType = "Complaint",
                    targetId = c.id,
                    details = "Exceeded 7-day deadline. Assigned to ${c.assignedOfficerName ?: "Unassigned (Manager Action Needed)"}"
                )
            }
        }
    }

    private fun preloadSampleData() {
        if (projects.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val hourMillis = 60 * 60 * 1000L

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

        // 1. Preload Sample Projects in Tamil Nadu
        projects.addAll(
            listOf(
                Project(
                    id = "TN-PWD-2025-01",
                    title = "Anna Salai Stormwater Drain & Road Resurfacing",
                    description = "Comprehensive stormwater drain network construction and high-grade asphalt resurfacing along Anna Salai corridor to prevent monsoon flooding.",
                    department = "Public Works Department",
                    district = "Chennai",
                    municipalBody = "Greater Chennai Corporation",
                    location = "Anna Salai (Teynampet to Nandanam), Chennai",
                    latitude = 13.0418,
                    longitude = 80.2341,
                    contractorName = "L&T Public Infrastructure Ltd",
                    contractorId = "CON-LT-9081",
                    responsibleOffice = "Highways & PWD Chennai Metro Division",
                    currentOfficerInCharge = "Er. R. Kanimozhi (Formerly Er. S. Sundaram)",
                    estimatedBudgetInLakhs = 450.0,
                    fundingSource = "Tamil Nadu Urban Infrastructure Fund",
                    startDate = "15 Jan 2025",
                    originalCompletionDate = "30 May 2025",
                    revisedCompletionDate = "15 Jun 2025",
                    status = ProjectStatus.ONGOING,
                    progressPercentage = 68,
                    milestones = listOf(
                        Milestone("M1", "Site Excavation & Utility Mapping", "10 Feb 2025", true, "08 Feb 2025"),
                        Milestone("M2", "Reinforced Concrete Box Culvert Laying", "25 Mar 2025", true, "22 Mar 2025"),
                        Milestone("M3", "Stormwater Outlet Linkage to Cooum River", "30 Apr 2025", false),
                        Milestone("M4", "Asphalt Road Surface Relaying & Pavement", "30 May 2025", false)
                    ),
                    isFollowed = true
                ),
                Project(
                    id = "TN-TWAD-2025-02",
                    title = "Avinashi Road Water Pipeline Distribution Upgrade",
                    description = "Replacement of aging iron water mains with high-density polyethylene pipelines to ensure 24/7 drinking water supply across East Coimbatore.",
                    department = "TWAD Board",
                    district = "Coimbatore",
                    municipalBody = "Coimbatore Municipal Corporation",
                    location = "Avinashi Road (Peelamedu to Hope College), Coimbatore",
                    latitude = 11.0283,
                    longitude = 77.0012,
                    contractorName = "KSR Infrastructure Projects",
                    contractorId = "CON-KSR-4421",
                    responsibleOffice = "TWAD Water Supply Circle Coimbatore",
                    currentOfficerInCharge = "Er. M. Saravanan",
                    estimatedBudgetInLakhs = 280.0,
                    fundingSource = "AMRUT 2.0 Scheme",
                    startDate = "01 Feb 2025",
                    originalCompletionDate = "28 Apr 2025",
                    revisedCompletionDate = "20 Jul 2025",
                    status = ProjectStatus.DELAYED,
                    progressPercentage = 42
                )
            )
        )

        // 2. Preload Complaints with Deterministic 7-Day Deadlines & Test Cases
        val subTime1 = now - (1 * dayMillis) // 1 day ago (6 days remaining)
        val deadTime1 = subTime1 + (7 * dayMillis)

        val subTime2 = now - (6 * dayMillis + 20 * hourMillis) // Due today (4 hours remaining)
        val deadTime2 = subTime2 + (7 * dayMillis)

        val subTime3 = now - (9 * dayMillis) // 9 days ago -> OVERDUE by 2 days
        val deadTime3 = subTime3 + (7 * dayMillis)

        val subTime4 = now - (10 * dayMillis) // Answered before deadline
        val deadTime4 = subTime4 + (7 * dayMillis)

        val subTime5 = now - (12 * dayMillis) // Overdue, then answered late
        val deadTime5 = subTime5 + (7 * dayMillis)

        val subTime6 = now - (8 * dayMillis) // Overdue & Unassigned
        val deadTime6 = subTime6 + (7 * dayMillis)

        complaints.addAll(
            listOf(
                Complaint(
                    id = "CMP-2026-101",
                    category = ComplaintCategory.POTHOLE,
                    projectId = "TN-TWAD-2025-02",
                    projectTitle = "Avinashi Road Water Pipeline Upgrade",
                    location = "Avinashi Road Near Hope College, Coimbatore",
                    district = "Coimbatore",
                    description = "Unpaved trench left open after water pipeline work causing traffic bottlenecks.",
                    submittedByCitizenName = "S. Karthik",
                    submittedDate = dateFormat.format(Date(subTime1)),
                    submissionTimestampMillis = subTime1,
                    deadlineTimestampMillis = deadTime1,
                    assignedDepartment = "TWAD Board",
                    assignedOfficerName = "Er. M. Saravanan",
                    assignedOfficerEmail = "officer.saravanan@twad.tn.gov.in",
                    status = ComplaintStatus.UNDER_REVIEW,
                    priority = ComplaintPriority.NORMAL,
                    lastUpdatedDate = "1 day ago",
                    eventHistory = listOf(
                        ComplaintEvent("E1", subTime1, dateFormat.format(Date(subTime1)), "Submitted", "Complaint registered by citizen S. Karthik.", "S. Karthik"),
                        ComplaintEvent("E2", subTime1 + (2 * hourMillis), dateFormat.format(Date(subTime1 + 2 * hourMillis)), "Officer Assigned", "Assigned to Er. M. Saravanan (TWAD Board).", "System"),
                        ComplaintEvent("E3", subTime1 + (5 * hourMillis), dateFormat.format(Date(subTime1 + 5 * hourMillis)), "Review Started", "Official review initiated.", "Er. M. Saravanan")
                    )
                ),
                Complaint(
                    id = "CMP-2026-102",
                    category = ComplaintCategory.DRAINAGE_OVERFLOW,
                    projectId = "TN-PWD-2025-01",
                    projectTitle = "Anna Salai Stormwater Drain & Road Resurfacing",
                    location = "Anna Salai Service Lane, Teynampet, Chennai",
                    district = "Chennai",
                    description = "Construction debris blocking existing roadside storm drain outlet.",
                    submittedByCitizenName = "M. Meenakshi",
                    submittedDate = dateFormat.format(Date(subTime2)),
                    submissionTimestampMillis = subTime2,
                    deadlineTimestampMillis = deadTime2,
                    assignedDepartment = "Public Works Department",
                    assignedOfficerName = "Er. R. Kanimozhi",
                    assignedOfficerEmail = "officer.kanimozhi@pwd.tn.gov.in",
                    status = ComplaintStatus.PENDING,
                    priority = ComplaintPriority.NORMAL,
                    lastUpdatedDate = "Today",
                    eventHistory = listOf(
                        ComplaintEvent("E10", subTime2, dateFormat.format(Date(subTime2)), "Submitted", "Complaint registered by citizen M. Meenakshi.", "M. Meenakshi")
                    )
                ),
                Complaint(
                    id = "CMP-2026-103",
                    category = ComplaintCategory.CONSTRUCTION_QUALITY,
                    projectId = "TN-PWD-2025-01",
                    projectTitle = "Anna Salai Stormwater Drain",
                    location = "Anna Salai Teynampet Signal, Chennai",
                    district = "Chennai",
                    description = "Deep unpaved trench causing severe traffic congestion.",
                    submittedByCitizenName = "P. Selvam",
                    submittedDate = dateFormat.format(Date(subTime3)),
                    submissionTimestampMillis = subTime3,
                    deadlineTimestampMillis = deadTime3,
                    assignedDepartment = "Public Works Department",
                    assignedOfficerName = "Er. R. Kanimozhi",
                    assignedOfficerEmail = "officer.kanimozhi@pwd.tn.gov.in",
                    status = ComplaintStatus.ESCALATED_OVERDUE,
                    priority = ComplaintPriority.HIGH,
                    lastUpdatedDate = "Overdue",
                    eventHistory = listOf(
                        ComplaintEvent("E20", subTime3, dateFormat.format(Date(subTime3)), "Submitted", "Complaint registered.", "P. Selvam"),
                        ComplaintEvent("E21", deadTime3, dateFormat.format(Date(deadTime3)), "Escalated — Overdue", "7-day official response deadline passed without response. Automatically escalated.", "System Escalation Engine")
                    )
                ),
                Complaint(
                    id = "CMP-2026-104",
                    category = ComplaintCategory.WATER_LEAK,
                    projectId = null,
                    projectTitle = null,
                    location = "Main Street Junction, Salem",
                    district = "Salem",
                    description = "Water pipeline leakage near bus stand.",
                    submittedByCitizenName = "K. Raman",
                    submittedDate = dateFormat.format(Date(subTime4)),
                    submissionTimestampMillis = subTime4,
                    deadlineTimestampMillis = deadTime4,
                    assignedDepartment = "TWAD Board",
                    assignedOfficerName = "Er. V. Murugan",
                    status = ComplaintStatus.RESOLVED,
                    priority = ComplaintPriority.NORMAL,
                    lastUpdatedDate = "Resolved",
                    responses = listOf(
                        ComplaintResponse("R104", dateFormat.format(Date(subTime4 + 2 * dayMillis)), "Er. V. Murugan", "Government Officer", "Pipe leak repaired and pressure tested.")
                    ),
                    eventHistory = listOf(
                        ComplaintEvent("E30", subTime4, dateFormat.format(Date(subTime4)), "Submitted", "Complaint registered.", "K. Raman"),
                        ComplaintEvent("E31", subTime4 + (2 * dayMillis), dateFormat.format(Date(subTime4 + 2 * dayMillis)), "Resolved", "Leak repaired before deadline.", "Er. V. Murugan")
                    )
                ),
                Complaint(
                    id = "CMP-2026-105",
                    category = ComplaintCategory.POTHOLE,
                    location = "Vaigai River South Bank Road, Madurai",
                    district = "Madurai",
                    description = "Road potholes near river bridge.",
                    submittedByCitizenName = "A. Vijay",
                    submittedDate = dateFormat.format(Date(subTime5)),
                    submissionTimestampMillis = subTime5,
                    deadlineTimestampMillis = deadTime5,
                    assignedDepartment = "Highways Department",
                    assignedOfficerName = "Er. A. Vijayalakshmi",
                    status = ComplaintStatus.RESOLVED,
                    priority = ComplaintPriority.HIGH,
                    lastUpdatedDate = "Resolved (Late)",
                    responses = listOf(
                        ComplaintResponse("R105", dateFormat.format(Date(subTime5 + 9 * dayMillis)), "Er. A. Vijayalakshmi", "Government Officer", "Road potholes filled with asphalt mix on 9th day.")
                    ),
                    eventHistory = listOf(
                        ComplaintEvent("E40", subTime5, dateFormat.format(Date(subTime5)), "Submitted", "Complaint registered.", "A. Vijay"),
                        ComplaintEvent("E41", deadTime5, dateFormat.format(Date(deadTime5)), "Escalated — Overdue", "7-day deadline passed without response. Escalated.", "System Escalation Engine"),
                        ComplaintEvent("E42", subTime5 + (9 * dayMillis), dateFormat.format(Date(subTime5 + 9 * dayMillis)), "Late Response Recorded", "Officer responded late. Complaint resolved.", "Er. A. Vijayalakshmi")
                    )
                ),
                Complaint(
                    id = "CMP-2026-106",
                    category = ComplaintCategory.OTHER,
                    location = "Chinthamani Circle, Tiruchirappalli",
                    district = "Tiruchirappalli",
                    description = "Unattended construction debris on bridge approach.",
                    submittedByCitizenName = "R. Suresh",
                    submittedDate = dateFormat.format(Date(subTime6)),
                    submissionTimestampMillis = subTime6,
                    deadlineTimestampMillis = deadTime6,
                    assignedDepartment = "Highways Department",
                    assignedOfficerName = null, // Unassigned!
                    status = ComplaintStatus.ESCALATED_OVERDUE,
                    priority = ComplaintPriority.HIGH,
                    lastUpdatedDate = "Unassigned — Escalation Required",
                    eventHistory = listOf(
                        ComplaintEvent("E50", subTime6, dateFormat.format(Date(subTime6)), "Submitted", "Complaint registered.", "R. Suresh"),
                        ComplaintEvent("E51", deadTime6, dateFormat.format(Date(deadTime6)), "Escalated — Overdue", "7-day deadline passed without assigned officer response. Flagged for Manager Action.", "System Escalation Engine")
                    )
                )
            )
        )

        // 3. Preload Inter-Departmental Schedule Conflicts
        conflicts.addAll(
            listOf(
                ScheduleConflict(
                    id = "CONF-TN-101",
                    projectAId = "TN-PWD-2025-01",
                    projectATitle = "Anna Salai Stormwater Drain & Road Resurfacing",
                    departmentA = "Public Works Department",
                    projectBId = "TN-TWAD-2025-09",
                    projectBTitle = "Teynampet High Pressure Water Main Extension",
                    departmentB = "TWAD Board",
                    location = "Anna Salai Service Road (Teynampet Signal)",
                    conflictingDates = "10 May 2025 to 25 May 2025",
                    conflictType = ConflictType.ROAD_DIGGING_OVERLAP,
                    status = ConflictStatus.UNRESOLVED,
                    proposedResolution = "Postpone TWAD pipeline trenching until PWD completes culvert linkage or execute joint trench excavation."
                )
            )
        )

        // 4. Preload Officer Handovers
        handovers.addAll(
            listOf(
                OfficerHandover(
                    id = "HO-2026-88",
                    outgoingOfficerName = "Er. S. Sundaram",
                    outgoingOfficerEmail = "officer.sundaram@pwd.tn.gov.in",
                    incomingOfficerName = "Er. R. Kanimozhi",
                    incomingOfficerEmail = "officer.kanimozhi@pwd.tn.gov.in",
                    department = "Public Works Department",
                    officeJurisdiction = "Highways & PWD Chennai Metro Division",
                    effectiveTransferDate = "15 Mar 2025",
                    reasonForReassignment = "Routine Departmental Transfer",
                    assignedProjectsCount = 2,
                    pendingTasksCount = 3,
                    handoverNotes = "Project TN-PWD-2025-01 is 68% complete.",
                    status = HandoverStatus.ACKNOWLEDGED,
                    acknowledgedDate = "16 Mar 2025"
                )
            )
        )

        // 5. Preload Users
        users.addAll(
            listOf(
                User("U1", "R. Anbarasan (Citizen)", "citizen@civictech.tn.gov.in", UserRole.CITIZEN, isDemoAccount = true),
                User("U2", "Er. R. Kanimozhi", "officer.kanimozhi@pwd.tn.gov.in", UserRole.OFFICER, "Public Works Department", "Highways & PWD Chennai Metro Division", isDemoAccount = true),
                User("U3", "Dr. S. Ramanathan", "coord.raman@tn.gov.in", UserRole.COORDINATOR, "Inter-Departmental Coordination Cell", "Tamil Nadu State Infrastructure Cell", isDemoAccount = true),
                User("U4", "L&T Infrastructure Admin", "contractor.lt@infra.com", UserRole.CONTRACTOR, "Contractor Wing", "L&T Infrastructure Ltd", isDemoAccount = true),
                User("U5", "System Admin (Chennai HQ)", "admin.civic@tn.gov.in", UserRole.ADMINISTRATOR, "Governance Tech Wing", "Tamil Nadu E-Governance Agency", isDemoAccount = true)
            )
        )

        // 6. Preload Audit Logs
        auditLogs.addAll(
            listOf(
                AuditLogEntry("AL-100", "16 Mar 2025", "Officer Handover Acknowledged", "Er. R. Kanimozhi", "OfficerHandover", "HO-2026-88", "Officer handover completed."),
                AuditLogEntry("AL-101", "12 Mar 2025", "Complaint Resolved", "Er. R. Kanimozhi", "Complaint", "CMP-2026-102", "Status changed to Resolved.")
            )
        )
    }

    // Mutating actions

    fun submitComplaint(
        category: ComplaintCategory,
        projectId: String?,
        projectTitle: String?,
        location: String,
        district: String,
        description: String,
        photoUri: String?,
        citizenName: String
    ): Complaint {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val newId = "CMP-2026-${(107..999).random()}"
        val dept = when (category) {
            ComplaintCategory.POTHOLE -> "Highways Department"
            ComplaintCategory.WATER_LEAK -> "TWAD Board"
            ComplaintCategory.DRAINAGE_OVERFLOW -> "Public Works Department"
            else -> "Municipal Administration"
        }

        val initialEvent = ComplaintEvent(
            id = "EVT-SUB-$newId",
            timestampMillis = now,
            dateString = dateFormat.format(Date(now)),
            eventType = "Submitted",
            description = "Complaint registered by citizen $citizenName. Assigned 7-day official response deadline.",
            performedBy = citizenName
        )

        val newComplaint = Complaint(
            id = newId,
            category = category,
            projectId = projectId,
            projectTitle = projectTitle,
            location = location,
            district = district,
            description = description,
            photoUri = photoUri,
            submittedByCitizenName = citizenName,
            submittedDate = dateFormat.format(Date(now)),
            submissionTimestampMillis = now,
            deadlineTimestampMillis = now + (7 * 24 * 60 * 60 * 1000L), // Exactly 7 calendar days
            assignedDepartment = dept,
            assignedOfficerName = "Er. R. Kanimozhi",
            assignedOfficerEmail = "officer.kanimozhi@pwd.tn.gov.in",
            status = ComplaintStatus.PENDING,
            priority = ComplaintPriority.NORMAL,
            lastUpdatedDate = "Just now",
            eventHistory = listOf(initialEvent)
        )

        complaints.add(0, newComplaint)
        addAuditLog("Complaint Submitted", citizenName, "Complaint", newId, "Category: ${category.labelEn}, 7-Day Deadline Set")
        return newComplaint
    }

    fun respondToComplaint(
        complaintId: String,
        decision: ComplaintStatus,
        responseText: String,
        officerName: String,
        officerRole: String = "Government Officer"
    ) {
        val index = complaints.indexOfFirst { it.id == complaintId }
        if (index != -1) {
            val c = complaints[index]
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            val dateStr = dateFormat.format(Date(now))

            val wasOverdue = c.status == ComplaintStatus.ESCALATED_OVERDUE || now > c.deadlineTimestampMillis

            val newResponse = ComplaintResponse(
                id = "R-${System.currentTimeMillis()}",
                date = dateStr,
                respondedBy = officerName,
                role = officerRole,
                message = responseText
            )

            val eventTypeStr = if (wasOverdue) "Late Official Response Recorded" else "Official Response Recorded (${decision.labelEn})"
            val eventDesc = if (wasOverdue) "Officer responded after 7-day deadline passed. Decision: ${decision.labelEn}. $responseText" else "Officer decision: ${decision.labelEn}. $responseText"

            val newEvent = ComplaintEvent(
                id = "EVT-RESP-${System.currentTimeMillis()}",
                timestampMillis = now,
                dateString = dateStr,
                eventType = eventTypeStr,
                description = eventDesc,
                performedBy = officerName
            )

            complaints[index] = c.copy(
                status = decision,
                lastUpdatedDate = dateStr,
                responses = c.responses + newResponse,
                eventHistory = c.eventHistory + newEvent
            )

            addAuditLog("Complaint Responded", officerName, "Complaint", complaintId, "Decision: ${decision.labelEn}, Was Overdue: $wasOverdue")
        }
    }

    fun requestCitizenInformation(
        complaintId: String,
        requestText: String,
        officerName: String
    ) {
        val index = complaints.indexOfFirst { it.id == complaintId }
        if (index != -1) {
            val c = complaints[index]
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            val dateStr = dateFormat.format(Date(now))

            val newResponse = ComplaintResponse(
                id = "R-REQ-${System.currentTimeMillis()}",
                date = dateStr,
                respondedBy = officerName,
                role = "Government Officer",
                message = "Information Requested: $requestText"
            )

            val newEvent = ComplaintEvent(
                id = "EVT-REQ-${System.currentTimeMillis()}",
                timestampMillis = now,
                dateString = dateStr,
                eventType = "Additional Information Requested",
                description = "Officer requested clarification: $requestText",
                performedBy = officerName
            )

            complaints[index] = c.copy(
                status = ComplaintStatus.AWAITING_CITIZEN_INFO,
                lastUpdatedDate = dateStr,
                responses = c.responses + newResponse,
                eventHistory = c.eventHistory + newEvent
            )

            addAuditLog("Information Requested for Complaint", officerName, "Complaint", complaintId, requestText)
        }
    }

    fun toggleProjectFollow(projectId: String) {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index != -1) {
            val p = projects[index]
            projects[index] = p.copy(isFollowed = !p.isFollowed)
        }
    }

    fun registerProject(
        title: String,
        description: String,
        department: String,
        district: String,
        municipalBody: String,
        location: String,
        contractorName: String,
        responsibleOffice: String,
        officerInCharge: String,
        budgetLakhs: Double,
        fundingSource: String,
        startDate: String,
        completionDate: String
    ): Project {
        val count = projects.size + 1
        val newId = "TN-${department.take(3).uppercase()}-2025-0$count"
        val newProject = Project(
            id = newId,
            title = title,
            description = description,
            department = department,
            district = district,
            municipalBody = municipalBody,
            location = location,
            latitude = 13.0827,
            longitude = 80.2707,
            contractorName = contractorName,
            contractorId = "CON-GEN-550",
            responsibleOffice = responsibleOffice,
            currentOfficerInCharge = officerInCharge,
            estimatedBudgetInLakhs = budgetLakhs,
            fundingSource = fundingSource,
            startDate = startDate,
            originalCompletionDate = completionDate,
            revisedCompletionDate = completionDate,
            status = ProjectStatus.PLANNED,
            progressPercentage = 0,
            milestones = listOf(
                Milestone("M1", "Initial Work Site Preparation & Clearance", startDate, false),
                Milestone("M2", "Core Infrastructure Laying", completionDate, false)
            )
        )
        projects.add(0, newProject)
        addAuditLog("Project Registered", officerInCharge, "Project", newId, "Title: $title, Budget: ₹$budgetLakhs Lakhs")
        return newProject
    }

    fun recordDelay(projectId: String, reason: String, revisedDate: String, explanation: String, officerName: String) {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index != -1) {
            val p = projects[index]
            val newRecord = DelayRecord(
                id = "DR-${System.currentTimeMillis()}",
                projectId = projectId,
                dateRecorded = "Today",
                reason = reason,
                originalCompletionDate = p.originalCompletionDate,
                revisedCompletionDate = revisedDate,
                publishedExplanation = explanation,
                recordedBy = officerName
            )
            val updatedHistory = p.delayHistory + newRecord
            projects[index] = p.copy(
                revisedCompletionDate = revisedDate,
                status = ProjectStatus.DELAYED,
                delayHistory = updatedHistory
            )
            addAuditLog("Project Delay Recorded", officerName, "Project", projectId, "Revised Date: $revisedDate, Reason: $reason")
        }
    }

    fun submitProgressReport(
        projectId: String,
        milestoneId: String,
        submittedBy: String,
        completionPct: Int,
        workDone: String,
        remainingWork: String,
        obstacles: String?
    ): ProgressReport {
        val newReport = ProgressReport(
            id = "PR-${System.currentTimeMillis()}",
            projectId = projectId,
            milestoneId = milestoneId,
            submittedBy = submittedBy,
            submissionDate = "Today",
            completionPercentage = completionPct,
            workCompleted = workDone,
            remainingWork = remainingWork,
            obstacles = obstacles,
            status = "Pending Verification"
        )
        val index = projects.indexOfFirst { it.id == projectId }
        if (index != -1) {
            val p = projects[index]
            val updatedReports = p.progressReports + newReport
            projects[index] = p.copy(progressReports = updatedReports)
        }
        addAuditLog("Progress Report Submitted", submittedBy, "ProgressReport", newReport.id, "Project: $projectId, Completion: $completionPct%")
        return newReport
    }

    fun verifyProgressReport(projectId: String, reportId: String, approve: Boolean, officerNotes: String, officerName: String) {
        val projIndex = projects.indexOfFirst { it.id == projectId }
        if (projIndex != -1) {
            val p = projects[projIndex]
            val rList = p.progressReports.map { report ->
                if (report.id == reportId) {
                    report.copy(
                        status = if (approve) "Approved" else "Correction Requested",
                        officerNotes = officerNotes
                    )
                } else report
            }
            val targetReport = p.progressReports.find { it.id == reportId }
            val newProgressPct = if (approve && targetReport != null) {
                maxOf(p.progressPercentage, targetReport.completionPercentage)
            } else p.progressPercentage

            val newStatus = if (newProgressPct >= 100) ProjectStatus.COMPLETED else p.status

            projects[projIndex] = p.copy(
                progressReports = rList,
                progressPercentage = newProgressPct,
                status = newStatus
            )
            addAuditLog("Progress Report Verified", officerName, "ProgressReport", reportId, "Approved: $approve, Project Progress: $newProgressPct%")
        }
    }

    fun initiateHandover(
        outgoingName: String,
        outgoingEmail: String,
        incomingName: String,
        incomingEmail: String,
        department: String,
        jurisdiction: String,
        transferDate: String,
        reason: String,
        notes: String
    ): OfficerHandover {
        val assignedCount = projects.count { it.responsibleOffice.contains(jurisdiction, ignoreCase = true) || it.currentOfficerInCharge.contains(outgoingName, ignoreCase = true) }
        val newHandover = OfficerHandover(
            id = "HO-2026-${(100..999).random()}",
            outgoingOfficerName = outgoingName,
            outgoingOfficerEmail = outgoingEmail,
            incomingOfficerName = incomingName,
            incomingOfficerEmail = incomingEmail,
            department = department,
            officeJurisdiction = jurisdiction,
            effectiveTransferDate = transferDate,
            reasonForReassignment = reason,
            assignedProjectsCount = maxOf(assignedCount, 1),
            pendingTasksCount = 2,
            pendingTasks = listOf(
                PendingTask("PT-A", "Verify pending project milestones", "TN-PWD-2025-01", "High", transferDate),
                PendingTask("PT-B", "Review unresolved complaints in $jurisdiction", "TN-PWD-2025-01", "Medium", transferDate)
            ),
            handoverNotes = notes,
            status = HandoverStatus.INITIATED
        )
        handovers.add(0, newHandover)
        addAuditLog("Officer Handover Initiated", outgoingName, "OfficerHandover", newHandover.id, "Transfer to $incomingName ($jurisdiction)")
        return newHandover
    }

    fun acknowledgeHandover(handoverId: String, incomingName: String) {
        val index = handovers.indexOfFirst { it.id == handoverId }
        if (index != -1) {
            val ho = handovers[index]
            handovers[index] = ho.copy(
                status = HandoverStatus.ACKNOWLEDGED,
                acknowledgedDate = "Today"
            )

            // Update assigned officer in corresponding projects while PRESERVING responsible office & history
            projects.indices.forEach { i ->
                val p = projects[i]
                if (p.responsibleOffice.contains(ho.officeJurisdiction, ignoreCase = true) || p.currentOfficerInCharge.contains(ho.outgoingOfficerName, ignoreCase = true)) {
                    projects[i] = p.copy(
                        currentOfficerInCharge = "${ho.incomingOfficerName} (Formerly ${ho.outgoingOfficerName})"
                    )
                }
            }

            addAuditLog("Officer Handover Acknowledged", incomingName, "OfficerHandover", handoverId, "Project history & office records preserved. Active officer updated.")
        }
    }

    fun resolveConflict(conflictId: String, resolutionText: String, coordinatorName: String) {
        val index = conflicts.indexOfFirst { it.id == conflictId }
        if (index != -1) {
            val c = conflicts[index]
            conflicts[index] = c.copy(
                status = ConflictStatus.RESOLVED,
                proposedResolution = resolutionText,
                resolvedDate = "Today"
            )
            addAuditLog("Schedule Conflict Resolved", coordinatorName, "ScheduleConflict", conflictId, "Resolution: $resolutionText")
        }
    }

    fun addUser(name: String, email: String, role: UserRole, department: String?, jurisdiction: String?) {
        val newUser = User(
            id = "U${users.size + 1}",
            name = name,
            email = email,
            role = role,
            department = department,
            officeJurisdiction = jurisdiction,
            isDemoAccount = false
        )
        users.add(newUser)
        addAuditLog("User Added", "Admin", "User", newUser.id, "Name: $name, Role: ${role.labelEn}")
    }

    private fun addAuditLog(action: String, performedBy: String, targetType: String, targetId: String, details: String) {
        val log = AuditLogEntry(
            id = "AL-${System.currentTimeMillis()}",
            timestamp = "Today",
            action = action,
            performedBy = performedBy,
            targetType = targetType,
            targetId = targetId,
            details = details
        )
        auditLogs.add(0, log)
    }
}
