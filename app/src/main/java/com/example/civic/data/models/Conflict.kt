package com.example.civic.data.models

enum class ConflictType(val labelEn: String, val labelTa: String) {
    ROAD_DIGGING_OVERLAP("Road Digging After Resurfacing", "தார் சாலை அமைத்த பின் தோண்டுதல்"),
    TRAFFIC_BLOCKADE("Simultaneous Traffic Blockade", "ஒரே நேரத்தில் போக்குவரத்து தடை"),
    UTILITY_INTERFERENCE("Utility Pipe/Cable Interference", "மின்/குடிநீர் பயன்பாட்டு இடைமறிப்பு"),
    RESOURCE_CONTENTION("Resource & Machinery Contention", "இயந்திரங்கள் மற்றும் வளங்கள் மோதல்")
}

enum class ConflictStatus(val labelEn: String, val labelTa: String) {
    UNRESOLVED("Unresolved / Action Needed", "தீர்க்கப்படாதது"),
    UNDER_DISCUSSION("In Discussion", "விவாதிக்கப்படுகிறது"),
    RESOLVED("Resolved / Rescheduled", "தீர்க்கப்பட்டது")
}

data class ScheduleConflict(
    val id: String, // e.g. "CONF-TN-101"
    val projectAId: String,
    val projectATitle: String,
    val departmentA: String,
    val projectBId: String,
    val projectBTitle: String,
    val departmentB: String,
    val location: String,
    val conflictingDates: String,
    val conflictType: ConflictType,
    val status: ConflictStatus,
    val proposedResolution: String? = null,
    val resolvedDate: String? = null
)
