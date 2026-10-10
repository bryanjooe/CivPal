package com.example.civic.data.backend

import com.example.civic.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions
import io.ktor.client.engine.okhttp.OkHttp

/** Mobile access always uses RLS and the signed-in user's session. */
object CivicSupabase {
    val configured get() = BuildConfig.SUPABASE_URL.startsWith("https://") &&
        BuildConfig.SUPABASE_PUBLISHABLE_KEY.startsWith("sb_publishable_")
    val client by lazy {
        check(configured) { "Configure SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in local.properties." }
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            httpEngine = OkHttp.create()
            install(Auth) { scheme = "civic"; host = "auth-callback" }
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }
}
