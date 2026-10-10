package com.example.civic.data.models

enum class UserRole(val labelEn: String, val labelTa: String) {
    CITIZEN("Citizen", "குடிமகன்"),
    OFFICER("Government Officer", "அரசு அலுவலர்"),
    COORDINATOR("Department Coordinator", "துறை ஒருங்கிணைப்பாளர்"),
    CONTRACTOR("Contractor", "ஒப்பந்ததாரர்"),
    ADMINISTRATOR("Administrator", "நிர்வாகி")
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val department: String? = null,
    val officeJurisdiction: String? = null,
    val isDemoAccount: Boolean = true,
    val isActive: Boolean = true
)

data class AuditLogEntry(
    val id: String,
    val timestamp: String,
    val action: String,
    val performedBy: String,
    val targetType: String,
    val targetId: String,
    val details: String
)
