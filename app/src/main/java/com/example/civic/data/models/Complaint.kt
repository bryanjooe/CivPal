package com.example.civic.data.models

enum class ComplaintCategory(val labelEn: String, val labelTa: String) {
    POTHOLE("Road Potholes & Damage", "சாலை குழிகள் மற்றும் சேதம்"),
    WATER_LEAK("Water Pipeline Leakage", "குடிநீர் குழாய் கசிவு"),
    DRAINAGE_OVERFLOW("Drainage Overflow", "கழிவுநீர் வழிந்தோடல்"),
    DELAY_QUERY("Project Delay Query", "திட்ட தாமதக் கேள்வி"),
    CONSTRUCTION_QUALITY("Construction Quality Issue", "கட்டுமானத் தரப் பிரச்சனை"),
    OTHER("Other Public Issue", "இதர பொதுப் பிரச்சனை")
}

enum class ComplaintStatus(val labelEn: String, val labelTa: String) {
    PENDING("Pending Response", "நிலுவையில்"),
    UNDER_REVIEW("Under Review", "பரிசீலனையில்"),
    ACTION_PROPOSED("Action Proposed", "நடவடிக்கை முன்மொழியப்பட்டது"),
    RESOLVED("Resolved", "தீர்வு காணப்பட்டது"),
    REJECTED("Rejected", "நிராகரிக்கப்பட்டது"),
    ESCALATED_OVERDUE("Escalated — Overdue", "காலக்கெடு மீறப்பட்டு உயர்த்தப்பட்டது"),
    AWAITING_CITIZEN_INFO("Awaiting Citizen Info", "குடிமகன் தகவல் தேவை")
}

enum class ComplaintPriority(val labelEn: String, val labelTa: String) {
    NORMAL("Normal Priority", "சாதாரண முன்னுரிமை"),
    HIGH("High Priority", "உயர் முன்னுரிமை")
}

data class ComplaintEvent(
    val id: String,
    val timestampMillis: Long,
    val dateString: String,
    val eventType: String,
    val description: String,
    val performedBy: String
)

data class ComplaintResponse(
    val id: String,
    val date: String,
    val respondedBy: String,
    val role: String,
    val message: String
)

data class Complaint(
    val id: String, // e.g., "CMP-2026-101"
    val category: ComplaintCategory,
    val projectId: String? = null,
    val projectTitle: String? = null,
    val location: String,
    val district: String,
    val description: String,
    val photoUri: String? = null,
    val submittedByCitizenName: String,
    val submittedDate: String,
    val submissionTimestampMillis: Long = System.currentTimeMillis(),
    val deadlineTimestampMillis: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L), // Exactly 7 calendar days
    val assignedDepartment: String,
    val assignedOfficerName: String? = null,
    val assignedOfficerEmail: String? = null,
    val status: ComplaintStatus = ComplaintStatus.PENDING,
    val priority: ComplaintPriority = ComplaintPriority.NORMAL,
    val lastUpdatedDate: String = "Recent",
    val responses: List<ComplaintResponse> = emptyList(),
    val eventHistory: List<ComplaintEvent> = emptyList()
)
