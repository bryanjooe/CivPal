package com.example.civic.data.models

enum class HandoverStatus(val labelEn: String, val labelTa: String) {
    INITIATED("Initiated / Pending Review", "தொடங்கப்பட்டது"),
    ACKNOWLEDGED("Acknowledged & Complete", "ஏற்றுக்கொள்ளப்பட்டது"),
    CLARIFICATION_REQUESTED("Clarification Requested", "விளக்கம் கேட்கப்பட்டது")
}

data class PendingTask(
    val id: String,
    val title: String,
    val relatedProjectId: String,
    val priority: String, // High, Medium, Low
    val dueDate: String
)

data class OfficerHandover(
    val id: String, // e.g. "HO-2026-88"
    val outgoingOfficerName: String,
    val outgoingOfficerEmail: String,
    val incomingOfficerName: String,
    val incomingOfficerEmail: String,
    val department: String,
    val officeJurisdiction: String,
    val effectiveTransferDate: String,
    val reasonForReassignment: String,
    val assignedProjectsCount: Int,
    val pendingTasksCount: Int,
    val pendingTasks: List<PendingTask> = emptyList(),
    val handoverNotes: String,
    val status: HandoverStatus,
    val acknowledgedDate: String? = null,
    val clarificationNotes: String? = null
)
