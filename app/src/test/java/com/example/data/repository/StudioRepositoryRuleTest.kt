package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FirestoreConnectionStatus
import com.example.data.model.StudioProject
import com.example.data.model.UiState
import com.example.data.remote.GeminiServiceClient
import com.example.ui.viewmodel.DataViewModel
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [36])
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

    @Test
    fun upsertAndObserveUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val upsertResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertUserProfile(
                email = ALICE_EMAIL,
                displayName = "Alice Researcher",
                photoUrl = "https://example.com/alice.png",
                activeBranch = "main",
                projectsCount = 3L,
                bioExperimentsCount = 2L,
                preferredAiModel = "gpt-4o"
            )
        }
        assertTrue(upsertResult.isSuccess)

        val emittedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile().first { profile -> profile?.userId == aliceUid }
        }
        assertNotNull(emittedProfile)
        assertEquals("Alice Researcher", emittedProfile?.displayName)
        assertEquals(3L, emittedProfile?.projectsCount)
        assertEquals("gpt-4o", emittedProfile?.preferredAiModel)

        val updateModelResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updatePreferredAiModel("gemini-3.8-flash")
        }
        assertTrue(updateModelResult.isSuccess)

        val updatedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile().first { profile ->
                profile?.userId == aliceUid && profile.preferredAiModel == "gemini-3.8-flash"
            }
        }
        assertEquals("gemini-3.8-flash", updatedProfile?.preferredAiModel)
    }

    @Test
    fun createAndObserveBioExperiment_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBioExperiment(
                title = "Human Hemoglobin Folding",
                workflowMode = "PROTEIN_FOLDING",
                fastaSequence = ">HBB\nMVHLTPEEKSAVTALWGKVNVDEVGGEALGRLLVVYPWTQRFFESFGDLS",
                researchGoal = "3D structural stability analysis",
                plddtScore = 94L,
                freeEnergyKcal = -11.4,
                alphaHelixPct = 68L,
                betaSheetPct = 14L,
                aiReportSummary = "Stable alpha-helical bundle confirmed."
            )
        }
        assertTrue(createResult.isSuccess)
        val expId = createResult.getOrThrow()

        val emittedExperiments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBioExperiments().first { list -> list.any { it.id == expId } }
        }
        assertTrue(emittedExperiments.any { it.id == expId })
    }

    @Test
    fun dataViewModel_updateMissingDocument_transitionsToErrorInsteadOfHanging() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)
        val dataViewModel = DataViewModel(
            repository = repository,
            databaseId = firestore.app.options.projectId ?: "test-db",
            currentUserId = aliceUid
        )

        dataViewModel.updateDocumentInFirestore(
            collectionId = DataViewModel.COLLECTION_PROJECTS,
            documentId = "non_existent_doc_123",
            updatedPrimary = "Test",
            updatedSecondary = "Desc",
            updatedJsonOrThird = "Kotlin"
        )

        val terminalState = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataViewModel.mutationState.first { it is UiState.Error }
        }
        assertTrue(terminalState is UiState.Error)
        assertEquals(FirestoreConnectionStatus.ERROR, dataViewModel.connectionStatus.value)
    }

    @Test
    fun dataManager_fullCrudAndRealtimeObservation_succeedsForAuthenticatedOwner() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val dataManager = DataManager(firestore)

        // 1. Add
        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.addProject(
                name = "DataManager CRUD App",
                description = "Initial description",
                language = "Kotlin + Compose",
                htmlCode = "<main>Hello</main>"
            )
        }
        assertTrue(addResult.isSuccess)
        val projectId = addResult.getOrThrow()

        // 2. Read single + Read collection
        val readDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.readProject(projectId).getOrThrow()
        }
        assertNotNull(readDoc)
        assertEquals("DataManager CRUD App", readDoc?.name)

        val collectionDocs = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.readProjects().getOrThrow()
        }
        assertTrue(collectionDocs.any { it.id == projectId })

        // 3. Update partial fields
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.updateProjectFields(
                projectId = projectId,
                name = "DataManager Updated App",
                description = "Updated description"
            )
        }
        assertTrue(updateResult.isSuccess)

        val observedUpdated = withTimeout(FLOW_TIMEOUT_MS) {
            dataManager.observeDocument(
                collectionName = DataManager.COLLECTION_PROJECTS,
                documentId = projectId,
                clazz = StudioProject::class.java
            ).first { it?.name == "DataManager Updated App" }
        }
        assertEquals("Updated description", observedUpdated?.description)

        // 4. Delete
        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.deleteProject(projectId)
        }
        assertTrue(deleteResult.isSuccess)

        val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.readProject(projectId).getOrThrow()
        }
        assertNull(afterDelete)
    }

    @Test
    fun dataManager_unauthenticatedWrite_returnsFailureWithoutCrashing() = runBlocking {
        auth.signOut()
        val dataManager = DataManager(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            dataManager.addProject(
                name = "Unauthorized",
                description = "Should fail fast",
                language = "Kotlin"
            )
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun geminiServiceClient_buildsAndParsesGenkitEnvelopesCorrectly() {
        val envelope = GeminiServiceClient.buildGenkitEnvelopeJson(
            JSONObject().put("prompt", "Summarize architecture")
        )
        val parsedEnvelope = JSONObject(envelope)
        assertEquals("Summarize architecture", parsedEnvelope.getJSONObject("data").getString("prompt"))

        assertEquals(
            "Generated output text",
            GeminiServiceClient.parseGenkitResultText("""{"result":"Generated output text"}""")
        )
        assertEquals(
            "Nested output text",
            GeminiServiceClient.parseGenkitResultText("""{"result":{"text":"Nested output text"}}""")
        )
    }

    private companion object {
        const val ALICE_EMAIL = "alice@novaforge.test"
        const val BOB_EMAIL = "bob@novaforge.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
