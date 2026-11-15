package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BioExperimentRecord
import com.example.data.model.BuildExecutionLog
import com.example.data.model.DbCollectionSchema
import com.example.data.model.StudioProject
import com.example.data.model.UserProfile
import com.example.data.model.VersionSnapshot
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.util.UUID

class StudioRepository(private val db: FirebaseFirestore) {

    private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun requireText(value: String, field: String, maxLength: Int, allowBlank: Boolean = false) {
        if (!allowBlank) require(value.isNotBlank()) { "$field must not be blank." }
        require(value.length <= maxLength) {
            "$field exceeds the supported limit of $maxLength characters; no data was truncated."
        }
    }

    private fun requireCollectionName(value: String) {
        require(value.matches(Regex("^[A-Za-z][A-Za-z0-9_]{0,63}$"))) {
            "Collection name must start with a letter and contain only letters, numbers, and underscores (max 64 characters)."
        }
    }

    private fun parseSampleRecordsJson(value: String): JsonArray {
        val parsed = Json.parseToJsonElement(value.ifBlank { "[]" }) as? JsonArray
            ?: throw IllegalArgumentException("Sample records must be a JSON array.")
        require(parsed.all { it is JsonObject }) {
            "Each sample record must be a JSON object."
        }
        return parsed
    }

    private fun JsonElement.toFirestoreValue(): Any? = when (this) {
        is JsonObject -> entries.associate { (key, value) -> key to value.toFirestoreValue() }
        is JsonArray -> map { it.toFirestoreValue() }
        is JsonPrimitive -> {
            if (isString) content
            else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
        }
        JsonNull -> null
    }

    private suspend fun requireOwnedProject(uid: String, projectId: String) {
        requireText(projectId, "Project ID", 128)
        require(projectId.matches(Regex("^[a-zA-Z0-9_-]+$"))) { "Project ID contains unsupported characters." }
        val project = db.collection("users").document(uid)
            .collection("projects").document(projectId).get().await()
        require(project.exists() && project.getString("userId") == uid) {
            "The project does not exist or does not belong to the signed-in user."
        }
    }

    private suspend fun requireOwnedCollectionSchema(
        uid: String,
        projectId: String,
        schemaId: String,
        collectionName: String
    ) {
        requireText(schemaId, "Schema ID", 128)
        val schema = db.collection("users").document(uid)
            .collection("collections").document(schemaId).get().await()
        require(
            schema.exists() &&
                schema.getString("userId") == uid &&
                schema.getString("projectId") == projectId &&
                schema.getString("collectionName") == collectionName
        ) {
            "The collection schema does not exist or does not belong to this project and user."
        }
    }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Projects ---

    fun observeProjects(): Flow<List<StudioProject>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudioProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getUserProjects(): Result<List<StudioProject>> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/projects"
        try {
            val snapshot = db.collection("users").document(uid).collection("projects")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(
                StudioProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getProjectById(ownerUid: String, projectId: String): Result<StudioProject?> = runCatchingCancellable {
        val path = "users/$ownerUid/projects/$projectId"
        try {
            val doc = db.collection("users").document(ownerUid)
                .collection("projects").document(projectId)
                .get()
                .await()
            doc.toObject(
                StudioProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun createProject(
        name: String,
        description: String,
        language: String,
        htmlCode: String,
        cssCode: String,
        jsCode: String,
        backendCode: String
    ): Result<String> = runCatchingCancellable {
        val uid = requireUserId()
        requireText(name, "Project name", 120)
        requireText(description, "Project description", 1000, allowBlank = true)
        requireText(language, "Project language", 50)
        requireText(htmlCode, "HTML source", 50000, allowBlank = true)
        requireText(cssCode, "CSS source", 50000, allowBlank = true)
        requireText(jsCode, "JavaScript source", 50000, allowBlank = true)
        requireText(backendCode, "Backend source", 50000, allowBlank = true)
        val projectId = "proj_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        val payload = mapOf(
            "userId" to uid,
            "name" to name,
            "description" to description,
            "language" to language,
            "htmlCode" to htmlCode,
            "cssCode" to cssCode,
            "jsCode" to jsCode,
            "backendCode" to backendCode,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            projectId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProject(project: StudioProject): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        require(project.id.isNotBlank()) { "Project ID must not be blank for update" }
        requireNotNull(project.createdAt) { "Project createdAt must not be null for update" }
        requireText(project.name, "Project name", 120)
        requireText(project.description, "Project description", 1000, allowBlank = true)
        requireText(project.language, "Project language", 50)
        requireText(project.htmlCode, "HTML source", 50000, allowBlank = true)
        requireText(project.cssCode, "CSS source", 50000, allowBlank = true)
        requireText(project.jsCode, "JavaScript source", 50000, allowBlank = true)
        requireText(project.backendCode, "Backend source", 50000, allowBlank = true)
        val docRef = db.collection("users").document(uid).collection("projects").document(project.id)
        try {
            docRef.set(
                project.copy(
                    userId = uid,
                    name = project.name,
                    description = project.description,
                    language = project.language,
                    htmlCode = project.htmlCode,
                    cssCode = project.cssCode,
                    jsCode = project.jsCode,
                    backendCode = project.backendCode,
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        requireText(projectId, "Project ID", 128)
        val userDoc = db.collection("users").document(uid)
        val projectRef = userDoc.collection("projects").document(projectId)
        val project = try {
            projectRef.get().await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFirestoreError(error, OperationType.GET, projectRef.path)
            throw error
        }
        require(!project.exists() || project.getString("userId") == uid) {
            "The project does not belong to the signed-in user."
        }

        // Remove real custom collection records while their owning schema documents still exist.
        val ownedSchemas = try {
            userDoc.collection("collections")
                .whereEqualTo("userId", uid)
                .whereEqualTo("projectId", projectId)
                .get()
                .await()
                .documents
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFirestoreError(error, OperationType.LIST, userDoc.collection("collections").path)
            throw error
        }
        for (schema in ownedSchemas) {
            val collectionName = schema.getString("collectionName")
                ?: throw IllegalStateException("A linked schema is missing its collection name.")
            requireCollectionName(collectionName)
            val recordsRef = projectRef.collection("data").document(collectionName).collection("records")
            val records = try {
                recordsRef.whereEqualTo("userId", uid)
                    .whereEqualTo("projectId", projectId)
                    .get()
                    .await()
                    .documents
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFirestoreError(error, OperationType.LIST, recordsRef.path)
                throw error
            }
            records.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { document -> batch.delete(document.reference) }
                try {
                    batch.commit().await()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    handleFirestoreError(error, OperationType.DELETE, recordsRef.path)
                    throw error
                }
            }
        }

        // Firestore does not cascade deletes into sibling subcollections. Remove only records
        // explicitly owned by this user and linked to this project before deleting the parent.
        val dependentCollections = listOf("snapshots", "collections", "logs")
        for (collectionName in dependentCollections) {
            val collectionRef = userDoc.collection(collectionName)
            val children = try {
                collectionRef.whereEqualTo("userId", uid)
                    .whereEqualTo("projectId", projectId)
                    .get()
                    .await()
                    .documents
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFirestoreError(error, OperationType.LIST, collectionRef.path)
                throw error
            }

            children.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { document -> batch.delete(document.reference) }
                try {
                    batch.commit().await()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    handleFirestoreError(error, OperationType.DELETE, collectionRef.path)
                    throw error
                }
            }
        }

        try {
            projectRef.delete().await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFirestoreError(error, OperationType.DELETE, projectRef.path)
            throw error
        }
    }

    // --- Version Control Snapshots ---

    fun observeSnapshots(): Flow<List<VersionSnapshot>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/snapshots"
        emitAll(
            db.collection("users").document(uid).collection("snapshots")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VersionSnapshot::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createSnapshot(
        projectId: String,
        commitMessage: String,
        versionTag: String,
        htmlCode: String,
        cssCode: String,
        jsCode: String,
        backendCode: String,
        addedLines: Long,
        deletedLines: Long
    ): Result<String> = runCatchingCancellable {
        val uid = requireUserId()
        requireOwnedProject(uid, projectId)
        requireText(commitMessage, "Snapshot commit message", 300)
        requireText(versionTag, "Snapshot version tag", 40)
        requireText(htmlCode, "HTML snapshot", 50000, allowBlank = true)
        requireText(cssCode, "CSS snapshot", 50000, allowBlank = true)
        requireText(jsCode, "JavaScript snapshot", 50000, allowBlank = true)
        requireText(backendCode, "Backend snapshot", 50000, allowBlank = true)
        require(addedLines in 0L..100000L) { "Added line count is outside the supported range." }
        require(deletedLines in 0L..100000L) { "Deleted line count is outside the supported range." }
        val snapshotId = "snap_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val docRef = db.collection("users").document(uid).collection("snapshots").document(snapshotId)
        val payload = mapOf(
            "userId" to uid,
            "projectId" to projectId,
            "commitMessage" to commitMessage,
            "versionTag" to versionTag,
            "htmlCode" to htmlCode,
            "cssCode" to cssCode,
            "jsCode" to jsCode,
            "backendCode" to backendCode,
            "addedLines" to addedLines,
            "deletedLines" to deletedLines,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            snapshotId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Flexible Database Schemas & Collections ---

    fun observeCollections(): Flow<List<DbCollectionSchema>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/collections"
        emitAll(
            db.collection("users").document(uid).collection("collections")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DbCollectionSchema::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createCollectionSchema(
        projectId: String,
        collectionName: String,
        fieldsSpec: String,
        sampleRecordsJson: String,
        indexConfig: String
    ): Result<String> = runCatchingCancellable {
        val uid = requireUserId()
        requireOwnedProject(uid, projectId)
        requireText(collectionName, "Collection name", 80)
        requireCollectionName(collectionName)
        requireText(fieldsSpec, "Collection fields specification", 5000)
        requireText(sampleRecordsJson, "Sample records JSON", 20000, allowBlank = true)
        val normalizedSamples = parseSampleRecordsJson(sampleRecordsJson).toString()
        requireText(indexConfig, "Index configuration", 500, allowBlank = true)
        val colId = "col_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val docRef = db.collection("users").document(uid).collection("collections").document(colId)
        val payload = mapOf(
            "userId" to uid,
            "projectId" to projectId,
            "collectionName" to collectionName,
            "fieldsSpec" to fieldsSpec,
            "sampleRecordsJson" to normalizedSamples,
            "indexConfig" to indexConfig,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            colId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun createCollectionRecord(
        projectId: String,
        schemaId: String,
        collectionName: String,
        recordJson: String
    ): Result<String> = runCatchingCancellable {
        val uid = requireUserId()
        requireOwnedProject(uid, projectId)
        requireCollectionName(collectionName)
        requireText(recordJson, "Collection record JSON", 20000)
        requireOwnedCollectionSchema(uid, projectId, schemaId, collectionName)

        val parsedRecord = Json.parseToJsonElement(recordJson) as? JsonObject
            ?: throw IllegalArgumentException("A collection record must be a JSON object.")
        val recordId = "rec_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val document = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("data").document(collectionName)
            .collection("records").document(recordId)
        val payload = mapOf(
            "userId" to uid,
            "projectId" to projectId,
            "schemaId" to schemaId,
            "collectionName" to collectionName,
            "recordData" to parsedRecord.toFirestoreValue(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            document.set(payload).await()
            recordId
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFirestoreError(error, OperationType.CREATE, document.path)
            throw error
        }
    }

    suspend fun updateCollectionSchema(schema: DbCollectionSchema): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        require(schema.id.isNotBlank()) { "Schema ID must not be blank" }
        requireNotNull(schema.createdAt) { "Schema createdAt must not be null" }
        requireText(schema.collectionName, "Collection name", 80)
        requireCollectionName(schema.collectionName)
        requireText(schema.fieldsSpec, "Collection fields specification", 5000)
        requireText(schema.sampleRecordsJson, "Sample records JSON", 20000, allowBlank = true)
        val normalizedSamples = parseSampleRecordsJson(schema.sampleRecordsJson).toString()
        requireText(schema.indexConfig, "Index configuration", 500, allowBlank = true)
        val docRef = db.collection("users").document(uid).collection("collections").document(schema.id)
        try {
            docRef.set(
                schema.copy(
                    userId = uid,
                    collectionName = schema.collectionName,
                    fieldsSpec = schema.fieldsSpec,
                    sampleRecordsJson = normalizedSamples,
                    indexConfig = schema.indexConfig,
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCollectionSchema(collectionId: String): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        requireText(collectionId, "Collection schema ID", 128)
        val docRef = db.collection("users").document(uid).collection("collections").document(collectionId)
        val schema = try {
            docRef.get().await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFirestoreError(error, OperationType.GET, docRef.path)
            throw error
        }
        if (!schema.exists()) return@runCatchingCancellable
        require(schema.getString("userId") == uid) { "The collection schema does not belong to the signed-in user." }
        val projectId = schema.getString("projectId")
            ?: throw IllegalStateException("The collection schema has no project reference.")
        val collectionName = schema.getString("collectionName")
            ?: throw IllegalStateException("The collection schema has no collection name.")
        requireOwnedProject(uid, projectId)
        requireCollectionName(collectionName)

        val recordsRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("data").document(collectionName).collection("records")
        val records = try {
            recordsRef.whereEqualTo("userId", uid)
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("schemaId", collectionId)
                .get()
                .await()
                .documents
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFirestoreError(error, OperationType.LIST, recordsRef.path)
            throw error
        }
        records.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { document -> batch.delete(document.reference) }
            try {
                batch.commit().await()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFirestoreError(error, OperationType.DELETE, recordsRef.path)
                throw error
            }
        }

        try {
            docRef.delete().await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFirestoreError(error, OperationType.DELETE, docRef.path)
            throw error
        }
    }

    // --- Real-Time Build & Code Monitoring Logs ---

    fun observeBuildLogs(): Flow<List<BuildExecutionLog>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/logs"
        emitAll(
            db.collection("users").document(uid).collection("logs")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BuildExecutionLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createBuildLog(
        projectId: String,
        stage: String,
        status: String,
        durationMs: Long,
        reportSummary: String
    ): Result<String> = runCatchingCancellable {
        val uid = requireUserId()
        requireOwnedProject(uid, projectId)
        requireText(stage, "Build log stage", 60)
        require(status in listOf("SUCCESS", "WARNING", "ERROR", "RUNNING")) { "Unsupported build log status: $status" }
        require(durationMs in 0L..3600000L) { "Build log duration is outside the supported range." }
        requireText(reportSummary, "Build log summary", 10000)
        val logId = "log_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val docRef = db.collection("users").document(uid).collection("logs").document(logId)
        val payload = mapOf(
            "userId" to uid,
            "projectId" to projectId,
            "stage" to stage,
            "status" to status,
            "durationMs" to durationMs,
            "reportSummary" to reportSummary,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSnapshot(snapshotId: String): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("snapshots").document(snapshotId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBuildLog(logId: String): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("logs").document(logId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun checkConnectionLatency(): Result<Long> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/collections"
        val startMs = System.currentTimeMillis()
        try {
            db.collection("users").document(uid).collection("collections")
                .whereEqualTo("userId", uid)
                .limit(1)
                .get()
                .await()
            (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getCollectionSchemas(): Result<List<DbCollectionSchema>> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/collections"
        try {
            val snapshot = db.collection("users").document(uid).collection("collections")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(
                DbCollectionSchema::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getCollectionSchemaById(collectionId: String): Result<DbCollectionSchema?> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/collections/$collectionId"
        try {
            val doc = db.collection("users").document(uid)
                .collection("collections").document(collectionId)
                .get()
                .await()
            doc.toObject(
                DbCollectionSchema::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun getSnapshots(): Result<List<VersionSnapshot>> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/snapshots"
        try {
            val snapshot = db.collection("users").document(uid).collection("snapshots")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(
                VersionSnapshot::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getSnapshotById(snapshotId: String): Result<VersionSnapshot?> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/snapshots/$snapshotId"
        try {
            val doc = db.collection("users").document(uid)
                .collection("snapshots").document(snapshotId)
                .get()
                .await()
            doc.toObject(
                VersionSnapshot::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun getBuildLogs(): Result<List<BuildExecutionLog>> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/logs"
        try {
            val snapshot = db.collection("users").document(uid).collection("logs")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(
                BuildExecutionLog::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getBuildLogById(logId: String): Result<BuildExecutionLog?> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/logs/$logId"
        try {
            val doc = db.collection("users").document(uid)
                .collection("logs").document(logId)
                .get()
                .await()
            doc.toObject(
                BuildExecutionLog::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    // --- User Profile (/users/{userId}) ---

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { doc ->
                    if (doc.exists()) {
                        doc.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getUserProfile(): Result<UserProfile?> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid"
        try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                doc.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun upsertUserProfile(
        email: String,
        displayName: String,
        photoUrl: String,
        activeBranch: String,
        projectsCount: Long,
        bioExperimentsCount: Long,
        preferredAiModel: String? = null
    ): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeEmail = email.take(200)
        val safeDisplayName = displayName.ifBlank {
            safeEmail.substringBefore("@").ifBlank { "Cloud Developer" }
        }.take(120)
        val safePhotoUrl = photoUrl.take(1000)
        val safeBranch = activeBranch.ifBlank { "main" }.take(60)
        val safeProjects = projectsCount.coerceIn(0L, 100000L)
        val safeBio = bioExperimentsCount.coerceIn(0L, 100000L)
        try {
            val existingDoc = docRef.get().await()
            if (existingDoc.exists()) {
                val existingProfile = existingDoc.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val resolvedModel = preferredAiModel?.takeIf { it.isNotBlank() }?.take(100)
                    ?: existingProfile?.preferredAiModel?.takeIf { it.isNotBlank() }?.take(100)
                    ?: "gemini+chatgpt-dual"
                if (existingProfile?.createdAt != null) {
                    docRef.set(
                        existingProfile.copy(
                            userId = uid,
                            email = safeEmail,
                            displayName = safeDisplayName,
                            photoUrl = safePhotoUrl,
                            activeBranch = safeBranch,
                            projectsCount = safeProjects,
                            bioExperimentsCount = safeBio,
                            preferredAiModel = resolvedModel,
                            updatedAt = null
                        )
                    ).await()
                    return@runCatchingCancellable
                }
            }
            val initialModel = preferredAiModel?.takeIf { it.isNotBlank() }?.take(100) ?: "gemini+chatgpt-dual"
            val payload = mapOf(
                "userId" to uid,
                "email" to safeEmail,
                "displayName" to safeDisplayName,
                "photoUrl" to safePhotoUrl,
                "activeBranch" to safeBranch,
                "projectsCount" to safeProjects,
                "bioExperimentsCount" to safeBio,
                "preferredAiModel" to initialModel,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updatePreferredAiModel(preferredAiModel: String): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeModel = preferredAiModel.ifBlank { "gemini+chatgpt-dual" }.take(100)
        try {
            val existingDoc = docRef.get().await()
            if (existingDoc.exists()) {
                docRef.update(
                    mapOf(
                        "preferredAiModel" to safeModel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val user = auth.currentUser
                val email = (user?.email ?: "developer@novaforge.cloud").take(200)
                val displayName = (user?.displayName ?: email.substringBefore("@").ifBlank { "Cloud Developer" }).take(120)
                val photoUrl = (user?.photoUrl?.toString() ?: "").take(1000)
                val payload = mapOf(
                    "userId" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "activeBranch" to "main",
                    "projectsCount" to 0L,
                    "bioExperimentsCount" to 0L,
                    "preferredAiModel" to safeModel,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Bioscience Lab Experiments (/users/{userId}/bio_experiments/{experimentId}) ---

    fun observeBioExperiments(): Flow<List<BioExperimentRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/bio_experiments"
        emitAll(
            db.collection("users").document(uid).collection("bio_experiments")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BioExperimentRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getBioExperiments(): Result<List<BioExperimentRecord>> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/bio_experiments"
        try {
            val snapshot = db.collection("users").document(uid).collection("bio_experiments")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(
                BioExperimentRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getBioExperimentById(experimentId: String): Result<BioExperimentRecord?> = runCatchingCancellable {
        val uid = requireUserId()
        val path = "users/$uid/bio_experiments/$experimentId"
        try {
            val doc = db.collection("users").document(uid)
                .collection("bio_experiments").document(experimentId)
                .get()
                .await()
            doc.toObject(
                BioExperimentRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun createBioExperiment(
        title: String,
        workflowMode: String,
        fastaSequence: String,
        researchGoal: String,
        plddtScore: Long,
        freeEnergyKcal: Double,
        alphaHelixPct: Long,
        betaSheetPct: Long,
        aiReportSummary: String
    ): Result<String> = runCatchingCancellable {
        val uid = requireUserId()
        requireText(title, "Experiment title", 160)
        require(workflowMode in listOf("PROTEIN_FOLDING", "CRISPR_EDITING", "GENOMIC_ALIGNMENT", "CLINICAL_TRIALS")) {
            "Unsupported bioscience workflow mode: $workflowMode"
        }
        requireText(fastaSequence, "FASTA sequence", 50000)
        requireText(researchGoal, "Research goal", 1000, allowBlank = true)
        require(plddtScore in 0L..100L) { "pLDDT score must be between 0 and 100." }
        require(freeEnergyKcal.isFinite() && freeEnergyKcal in -500.0..500.0) { "Free energy is outside the supported range." }
        require(alphaHelixPct in 0L..100L && betaSheetPct in 0L..100L) { "Structure percentages must be between 0 and 100." }
        requireText(aiReportSummary, "AI report summary", 30000, allowBlank = true)
        val expId = "bio_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val docRef = db.collection("users").document(uid).collection("bio_experiments").document(expId)
        val payload = mapOf(
            "userId" to uid,
            "title" to title,
            "workflowMode" to workflowMode,
            "fastaSequence" to fastaSequence,
            "researchGoal" to researchGoal,
            "plddtScore" to plddtScore,
            "freeEnergyKcal" to freeEnergyKcal,
            "alphaHelixPct" to alphaHelixPct,
            "betaSheetPct" to betaSheetPct,
            "aiReportSummary" to aiReportSummary,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            expId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBioExperiment(experiment: BioExperimentRecord): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        require(experiment.id.isNotBlank()) { "BioExperiment ID must not be blank for update" }
        requireNotNull(experiment.createdAt) { "BioExperiment createdAt must not be null for update" }
        requireText(experiment.title, "Experiment title", 160)
        require(experiment.workflowMode in listOf("PROTEIN_FOLDING", "CRISPR_EDITING", "GENOMIC_ALIGNMENT", "CLINICAL_TRIALS")) {
            "Unsupported bioscience workflow mode: ${experiment.workflowMode}"
        }
        requireText(experiment.fastaSequence, "FASTA sequence", 50000)
        requireText(experiment.researchGoal, "Research goal", 1000, allowBlank = true)
        require(experiment.plddtScore in 0L..100L) { "pLDDT score must be between 0 and 100." }
        require(experiment.freeEnergyKcal.isFinite() && experiment.freeEnergyKcal in -500.0..500.0) { "Free energy is outside the supported range." }
        require(experiment.alphaHelixPct in 0L..100L && experiment.betaSheetPct in 0L..100L) {
            "Structure percentages must be between 0 and 100."
        }
        requireText(experiment.aiReportSummary, "AI report summary", 30000, allowBlank = true)
        val docRef = db.collection("users").document(uid).collection("bio_experiments").document(experiment.id)
        try {
            docRef.set(
                experiment.copy(
                    userId = uid,
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBioExperiment(experimentId: String): Result<Unit> = runCatchingCancellable {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("bio_experiments").document(experimentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}


