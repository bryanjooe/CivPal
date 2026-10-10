package com.example.civic.data.backend

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*

internal class CivicBackend {
    val client get() = CivicSupabase.client
    suspend fun rows(table: String, id: String? = null): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        var offset = 0L
        do {
            val page = client.from("civic_$table").select {
                if (id != null) filter { eq("id", id) }
                val keys = when (table) {
                    "profile_scopes" -> listOf("profile_id", "department_id", "jurisdiction_id")
                    "project_assignments" -> listOf("project_id")
                    "project_milestones" -> listOf("project_id", "milestone_id")
                    "progress_report_reviews" -> listOf("report_id")
                    "complaint_responses", "complaint_events" -> listOf("complaint_id", "id")
                    "handover_projects" -> listOf("handover_id", "project_id")
                    "handover_pending_tasks" -> listOf("handover_id", "id")
                    "project_follows" -> listOf("user_id", "project_id")
                    else -> listOf("id")
                }
                keys.forEach { order(it, io.github.jan.supabase.postgrest.query.Order.ASCENDING) }
                range(offset, offset + 499)
            }.decodeList<JsonObject>()
            result.addAll(page)
            offset += page.size
        } while (page.size == 500)
        return result
    }
    suspend fun snapshot(): CivicSnapshot = coroutineScope {
        val public = listOf("departments", "jurisdictions", "contractors", "projects",
            "project_milestones", "progress_reports", "delay_records")
        val private = listOf("profiles", "profile_scopes", "project_assignments", "progress_report_reviews",
            "complaints", "complaint_responses", "complaint_events", "project_conflicts", "officer_handovers",
            "handover_projects", "handover_pending_tasks", "project_follows", "deadline_revision_requests", "audit_logs")
        CivicSnapshot((public + if (client.auth.currentUserOrNull() != null) private else emptyList())
            .map { table -> async { table to rows(table) } }.awaitAll().toMap())
    }
    suspend fun rpc(name: String, params: JsonObject): String? {
        val data = client.postgrest.rpc("civic_$name", params).data
        return Json.parseToJsonElement(data.ifBlank { "null" }).jsonPrimitive.contentOrNull
    }
    suspend fun login(email: String, password: String) {
        require(email.contains('@') && password.isNotBlank()) { "Enter your account email and password." }
        client.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
    }
    suspend fun invite(name: String, email: String) {
        client.functions.invoke("civic-invite-user", body = arguments("name" to name, "email" to email))
    }
    suspend fun attach(bucket: String, record: String, bytes: ByteArray, type: String) {
        require(bytes.isNotEmpty() && bytes.size <= 10 * 1024 * 1024) { "Attachment must be 10 MB or smaller." }
        val extension = when (type) {
            "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/webp" -> "webp"
            "application/pdf" -> "pdf"; else -> error("Choose a JPEG, PNG, WebP image or a PDF document.")
        }
        require((bucket == "deadline-documents") == (extension == "pdf")) { "Invalid attachment type for this record." }
        val user = checkNotNull(client.auth.currentUserOrNull()) { "Sign in to attach a file." }
        val path = "${user.id}/$record/${java.util.UUID.randomUUID()}.$extension"
        client.storage.from(bucket).upload(path, bytes) { contentType = io.ktor.http.ContentType.parse(type); upsert = false }
        rpc("attach_file", arguments("p_bucket" to bucket, "p_record" to record, "p_path" to path))
    }
}

internal fun arguments(vararg values: Pair<String, Any?>): JsonObject = JsonObject(values.associate { (key, value) ->
    key to when (value) {
        null -> JsonNull; is JsonElement -> value; is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value); is Collection<*> -> JsonArray(value.map { JsonPrimitive(it.toString()) })
        else -> JsonPrimitive(value.toString())
    }
})
