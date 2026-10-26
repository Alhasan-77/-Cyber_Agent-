package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StudioRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveProject_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProject(
                name = "AI E-Commerce Full-Stack",
                description = "Cloud store with live preview",
                language = "Full-Stack Web",
                htmlCode = "<h1>Store</h1>",
                cssCode = "h1 { color: cyan; }",
                jsCode = "console.log('loaded');",
                backendCode = "fun api() {}"
            )
        }
        assertTrue(createResult.isSuccess)
        val projectId = createResult.getOrThrow()
        assertTrue(projectId.isNotEmpty())

        val emittedProjects = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects().first { list -> list.any { it.id == projectId } }
        }
        assertTrue(emittedProjects.any { it.id == projectId })
    }

    @Test
    fun createSnapshotAndBuildLog_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProject(
                name = "BioTech Pipeline",
                description = "Genomic analysis app",
                language = "Python + FastAPI",
                htmlCode = "<div id='app'></div>",
                cssCode = "",
                jsCode = "",
                backendCode = "import Bio"
            ).getOrThrow()
        }

        val snapResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createSnapshot(
                projectId = projectId,
                commitMessage = "Initial commit v1.0.0",
                versionTag = "v1.0.0",
                htmlCode = "<div id='app'></div>",
                cssCode = "",
                jsCode = "",
                backendCode = "import Bio",
                addedLines = 12L,
                deletedLines = 0L
            )
        }
        assertTrue(snapResult.isSuccess)

        val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBuildLog(
                projectId = projectId,
                stage = "Full-Stack Build & Security Scan",
                status = "SUCCESS",
                durationMs = 420L,
                reportSummary = "All modules compiled cleanly."
            )
        }
        assertTrue(logResult.isSuccess)
    }

    @Test
    fun getProjectById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudioRepository(firestore)
        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createProject(
                name = "Alice Private App",
                description = "Secret architecture",
                language = "Kotlin + Compose",
                htmlCode = "",
                cssCode = "",
                jsCode = "",
                backendCode = ""
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = StudioRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProjectById(aliceUid, projectId)
        }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeProjects_unauthenticatedUser_fails() = runBlocking {
        auth.signOut()
        val repository = StudioRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProjects().first()
            }
            fail("Expected exception when unauthenticated")
        } catch (e: Exception) {
            // Expected IllegalStateException from requireUserId() or PERMISSION_DENIED
            assertNotNull(e.message)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@novaforge.test"
        const val BOB_EMAIL = "bob@novaforge.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
