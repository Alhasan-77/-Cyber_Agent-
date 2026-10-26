package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BuildExecutionLog
import com.example.data.model.DbCollectionSchema
import com.example.data.model.StudioProject
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
import java.util.UUID

class StudioRepository(private val db: FirebaseFirestore) {

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

    suspend fun getUserProjects(): Result<List<StudioProject>> = runCatching {
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

    suspend fun getProjectById(ownerUid: String, projectId: String): Result<StudioProject?> = runCatching {
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
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val projectId = "proj_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        val payload = mapOf(
            "userId" to uid,
            "name" to name.take(120),
            "description" to description.take(1000),
            "language" to language.take(50),
            "htmlCode" to htmlCode.take(50000),
            "cssCode" to cssCode.take(50000),
            "jsCode" to jsCode.take(50000),
            "backendCode" to backendCode.take(50000),
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

    suspend fun updateProject(project: StudioProject): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(project.id.isNotBlank()) { "Project ID must not be blank for update" }
        requireNotNull(project.createdAt) { "Project createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("projects").document(project.id)
        try {
            docRef.set(
                project.copy(
                    userId = uid,
                    name = project.name.take(120),
                    description = project.description.take(1000),
                    language = project.language.take(50),
                    htmlCode = project.htmlCode.take(50000),
                    cssCode = project.cssCode.take(50000),
                    jsCode = project.jsCode.take(50000),
                    backendCode = project.backendCode.take(50000),
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

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
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
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val snapshotId = "snap_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val docRef = db.collection("users").document(uid).collection("snapshots").document(snapshotId)
        val payload = mapOf(
            "userId" to uid,
            "projectId" to projectId,
            "commitMessage" to commitMessage.take(300),
            "versionTag" to versionTag.take(40),
            "htmlCode" to htmlCode.take(50000),
            "cssCode" to cssCode.take(50000),
            "jsCode" to jsCode.take(50000),
            "backendCode" to backendCode.take(50000),
            "addedLines" to addedLines.coerceIn(0L, 100000L),
            "deletedLines" to deletedLines.coerceIn(0L, 100000L),
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
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val colId = "col_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val docRef = db.collection("users").document(uid).collection("collections").document(colId)
        val payload = mapOf(
            "userId" to uid,
            "projectId" to projectId,
            "collectionName" to collectionName.take(80),
            "fieldsSpec" to fieldsSpec.take(5000),
            "sampleRecordsJson" to sampleRecordsJson.take(20000),
            "indexConfig" to indexConfig.take(500),
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

    suspend fun updateCollectionSchema(schema: DbCollectionSchema): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(schema.id.isNotBlank()) { "Schema ID must not be blank" }
        requireNotNull(schema.createdAt) { "Schema createdAt must not be null" }
        val docRef = db.collection("users").document(uid).collection("collections").document(schema.id)
        try {
            docRef.set(
                schema.copy(
                    userId = uid,
                    collectionName = schema.collectionName.take(80),
                    fieldsSpec = schema.fieldsSpec.take(5000),
                    sampleRecordsJson = schema.sampleRecordsJson.take(20000),
                    indexConfig = schema.indexConfig.take(500),
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

    suspend fun deleteCollectionSchema(collectionId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("collections").document(collectionId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
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
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val logId = "log_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val docRef = db.collection("users").document(uid).collection("logs").document(logId)
        val safeStatus = if (status in listOf("SUCCESS", "WARNING", "ERROR", "RUNNING")) status else "SUCCESS"
        val payload = mapOf(
            "userId" to uid,
            "projectId" to projectId,
            "stage" to stage.take(60),
            "status" to safeStatus,
            "durationMs" to durationMs.coerceIn(0L, 3600000L),
            "reportSummary" to reportSummary.take(10000),
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

    suspend fun deleteSnapshot(snapshotId: String): Result<Unit> = runCatching {
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

    suspend fun deleteBuildLog(logId: String): Result<Unit> = runCatching {
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

    suspend fun checkConnectionLatency(): Result<Long> = runCatching {
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

    suspend fun getCollectionSchemas(): Result<List<DbCollectionSchema>> = runCatching {
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

    suspend fun getCollectionSchemaById(collectionId: String): Result<DbCollectionSchema?> = runCatching {
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

    suspend fun getSnapshots(): Result<List<VersionSnapshot>> = runCatching {
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

    suspend fun getSnapshotById(snapshotId: String): Result<VersionSnapshot?> = runCatching {
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

    suspend fun getBuildLogs(): Result<List<BuildExecutionLog>> = runCatching {
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

    suspend fun getBuildLogById(logId: String): Result<BuildExecutionLog?> = runCatching {
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
}


