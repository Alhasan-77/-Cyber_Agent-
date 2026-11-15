package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [35])
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
    fun createUpdateAndDeleteCollection_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)
        val projectId = repository.createProject(
            name = "Schema owner project",
            description = "Parent record for the test collection",
            language = "Kotlin",
            htmlCode = "",
            cssCode = "",
            jsCode = "",
            backendCode = ""
        ).getOrThrow()

        val collectionId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCollectionSchema(
                projectId = projectId,
                collectionName = "agent_records",
                fieldsSpec = "name:string, enabled:boolean",
                sampleRecordsJson = """[{"name":"first","enabled":true}]""",
                indexConfig = "name_ascending"
            ).getOrThrow()
        }

        val created = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCollections().first { list -> list.any { it.id == collectionId } }
                .first { it.id == collectionId }
        }
        assertNotNull(created.createdAt)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateCollectionSchema(
                created.copy(sampleRecordsJson = """[{"name":"second","enabled":false}]""")
            )
        }
        assertTrue(updateResult.isSuccess)

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCollections().first { list ->
                list.any { it.id == collectionId && it.sampleRecordsJson.contains("second") }
            }.first { it.id == collectionId }
        }
        assertTrue(updated.sampleRecordsJson.contains("second"))

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteCollectionSchema(collectionId)
        }
        assertTrue(deleteResult.isSuccess)

        val remaining = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCollections().first { list -> list.none { it.id == collectionId } }
        }
        assertTrue(remaining.none { it.id == collectionId })
    }

    @Test
    fun deleteProject_cascadesOnlyItsDependentRecords() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val projectToDelete = repository.createProject(
            name = "Delete cascade test",
            description = "Project whose linked records should be deleted",
            language = "Kotlin",
            htmlCode = "<h1>Delete me</h1>",
            cssCode = "",
            jsCode = "",
            backendCode = ""
        ).getOrThrow()
        val projectToKeep = repository.createProject(
            name = "Keep cascade test",
            description = "Unrelated project should remain",
            language = "Kotlin",
            htmlCode = "<h1>Keep me</h1>",
            cssCode = "",
            jsCode = "",
            backendCode = ""
        ).getOrThrow()

        repository.createSnapshot(
            projectId = projectToDelete,
            commitMessage = "Delete with parent",
            versionTag = "v1.0.0",
            htmlCode = "<h1>Delete me</h1>",
            cssCode = "",
            jsCode = "",
            backendCode = "",
            addedLines = 1L,
            deletedLines = 0L
        ).getOrThrow()
        repository.createCollectionSchema(
            projectId = projectToDelete,
            collectionName = "delete_records",
            fieldsSpec = "id:string",
            sampleRecordsJson = "[]",
            indexConfig = ""
        ).getOrThrow()
        repository.createBuildLog(
            projectId = projectToDelete,
            stage = "TEST",
            status = "WARNING",
            durationMs = 0L,
            reportSummary = "Deletion cascade test."
        ).getOrThrow()
        repository.createSnapshot(
            projectId = projectToKeep,
            commitMessage = "Keep with parent",
            versionTag = "v1.0.0",
            htmlCode = "<h1>Keep me</h1>",
            cssCode = "",
            jsCode = "",
            backendCode = "",
            addedLines = 1L,
            deletedLines = 0L
        ).getOrThrow()

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteProject(projectToDelete)
        }
        assertTrue(deleteResult.isSuccess)
        assertEquals(null, repository.getProjectById(aliceUid, projectToDelete).getOrThrow())

        val snapshots = repository.getSnapshots().getOrThrow()
        assertTrue(snapshots.none { it.projectId == projectToDelete })
        assertTrue(snapshots.any { it.projectId == projectToKeep })

        val schemas = repository.getCollectionSchemas().getOrThrow()
        assertTrue(schemas.none { it.projectId == projectToDelete })

        val logs = repository.getBuildLogs().getOrThrow()
        assertTrue(logs.none { it.projectId == projectToDelete })
    }

    @Test
    fun dependentRecords_rejectUnknownParentProject() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val snapshot = repository.createSnapshot(
            projectId = "missing_parent",
            commitMessage = "Invalid parent",
            versionTag = "v1.0.0",
            htmlCode = "",
            cssCode = "",
            jsCode = "",
            backendCode = "",
            addedLines = 0L,
            deletedLines = 0L
        )
        assertTrue(snapshot.isFailure)

        val schema = repository.createCollectionSchema(
            projectId = "missing_parent",
            collectionName = "invalid_records",
            fieldsSpec = "id:string",
            sampleRecordsJson = "[]",
            indexConfig = ""
        )
        assertTrue(schema.isFailure)

        val log = repository.createBuildLog(
            projectId = "missing_parent",
            stage = "TEST",
            status = "WARNING",
            durationMs = 0L,
            reportSummary = "Invalid parent should be rejected."
        )
        assertTrue(log.isFailure)
    }

    @Test
    fun createAndDeleteCustomCollectionRecord_requiresOwnedProjectAndSchema() = runBlocking {
        val userId = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore)

        val projectId = repository.createProject(
            name = "Custom records project",
            description = "Test real collection records",
            language = "Kotlin",
            htmlCode = "",
            cssCode = "",
            jsCode = "",
            backendCode = ""
        ).getOrThrow()
        val schemaId = repository.createCollectionSchema(
            projectId = projectId,
            collectionName = "orders",
            fieldsSpec = "orderId:string,amount:number",
            sampleRecordsJson = "[]",
            indexConfig = ""
        ).getOrThrow()

        val recordId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCollectionRecord(
                projectId = projectId,
                schemaId = schemaId,
                collectionName = "orders",
                recordJson = """{"orderId":"A-100","amount":12}"""
            ).getOrThrow()
        }
        val recordRef = firestore.collection("users").document(userId)
            .collection("projects").document(projectId)
            .collection("data").document("orders")
            .collection("records").document(recordId)
        val record = withTimeout(DEFAULT_TIMEOUT_MS) { recordRef.get().await() }
        assertTrue(record.exists())
        assertEquals(userId, record.getString("userId"))
        assertEquals(projectId, record.getString("projectId"))
        assertEquals(schemaId, record.getString("schemaId"))
        assertEquals("A-100", record.get("recordData.orderId"))

        val invalidJson = repository.createCollectionRecord(
            projectId = projectId,
            schemaId = schemaId,
            collectionName = "orders",
            recordJson = """["not","an","object"]"""
        )
        assertTrue(invalidJson.isFailure)

        val missingSchema = repository.createCollectionRecord(
            projectId = projectId,
            schemaId = "missing_schema",
            collectionName = "orders",
            recordJson = """{"orderId":"A-101"}"""
        )
        assertTrue(missingSchema.isFailure)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteCollectionRecord(
                projectId = projectId,
                schemaId = schemaId,
                collectionName = "orders",
                recordId = recordId
            )
        }
        assertTrue(deleteResult.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { recordRef.get().await() }.exists().not())
    }

    private companion object {
        const val ALICE_EMAIL = "alice@novaforge.test"
        const val BOB_EMAIL = "bob@novaforge.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
