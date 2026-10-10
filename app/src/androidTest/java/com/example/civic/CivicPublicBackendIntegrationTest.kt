package com.example.civic

import com.example.civic.data.backend.CivicBackend
import com.example.civic.data.backend.flag
import com.example.civic.data.backend.text
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Read-only live smoke test: no accounts, invitations, complaints or projects are created. */
class CivicPublicBackendIntegrationTest {
    @Test fun publicViewsAndPermanentIdLookupUseTheConfiguredServer() = runBlocking {
        withTimeout(45_000) {
            val backend = CivicBackend()
            backend.client.auth.awaitInitialization()
            assumeTrue("Run this public test with a signed-out test installation", backend.client.auth.currentUserOrNull() == null)
            val snapshot = backend.snapshot()
            val rows = snapshot.rows("projects")
            assertTrue("Stage 1 should contain published demonstration projects", rows.isNotEmpty())
            assertTrue(rows.all { it.flag("is_published") })
            assertTrue(snapshot.rows("profiles").isEmpty())
            assertTrue(snapshot.rows("complaints").isEmpty())
            val row = rows.first()
            val fetched = backend.rows("projects", row.text("id")).single()
            assertEquals(row.text("id"), fetched.text("id"))
            val project = snapshot.project(fetched)
            assertEquals(fetched.text("progress_percentage").toInt(), project.progressPercentage)
            assertEquals(project.id, row.text("id"))
            assertTrue(project.estimatedBudgetInLakhs >= 0)
            assertNotNull(project.startDate)
        }
    }
}
