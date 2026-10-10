package com.example.civic.data.models

enum class ProjectStatus(val labelEn: String, val labelTa: String) {
    PLANNED("Planned", "திட்டமிடப்பட்டது"),
    ONGOING("Ongoing", "நடைபெறுகிறது"),
    DELAYED("Delayed", "தாமதமாகிறது"),
    COMPLETED("Completed", "நிறைவடைந்தது")
}

data class Milestone(
    val id: String,
    val title: String,
    val targetDate: String,
    val isCompleted: Boolean = false,
    val completionDate: String? = null
)

data class ProgressReport(
    val id: String,
    val projectId: String,
    val milestoneId: String,
    val submittedBy: String,
    val submissionDate: String,
    val completionPercentage: Int,
    val workCompleted: String,
    val remainingWork: String,
    val obstacles: String? = null,
    val photoUri: String? = null,
    val status: String = "Pending Verification", // "Pending Verification", "Approved", "Rejected", "Correction Requested"
    val officerNotes: String? = null
)

data class DelayRecord(
    val id: String,
    val projectId: String,
    val dateRecorded: String,
    val reason: String,
    val originalCompletionDate: String,
    val revisedCompletionDate: String,
    val publishedExplanation: String,
    val recordedBy: String
)

data class Project(
    val id: String, // e.g., "TN-PWD-2025-01"
    val title: String,
    val description: String,
    val department: String,
    val district: String,
    val municipalBody: String, // e.g. "Chennai Corporation", "Coimbatore Municipality"
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val contractorName: String,
    val contractorId: String,
    val responsibleOffice: String,
    val currentOfficerInCharge: String,
    val estimatedBudgetInLakhs: Double,
    val fundingSource: String,
    val startDate: String,
    val originalCompletionDate: String,
    val revisedCompletionDate: String,
    val status: ProjectStatus,
    val progressPercentage: Int,
    val milestones: List<Milestone> = emptyList(),
    val progressReports: List<ProgressReport> = emptyList(),
    val delayHistory: List<DelayRecord> = emptyList(),
    val isFollowed: Boolean = false,
    val isDemo: Boolean = false
)
