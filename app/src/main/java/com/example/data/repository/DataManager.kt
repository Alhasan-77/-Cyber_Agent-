package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BioExperimentRecord
import com.example.data.model.DbCollectionSchema
import com.example.data.model.StudioProject
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Coroutine-based Firestore DataManager providing scoped CRUD operations (add, read, update, delete)
 * and real-time snapshot Flows for user-owned app documents.
 *
 * Explicitly targets the named Firestore database
 * (`ai-studio-android-novaforg-6912163c-aa3c-4903-a89e-6df061c893e0` / `R.string.firestore_database_id`)
 * instead of the `(default)` instance, supports Firebase multi-instance [FirebaseApp] routing,
 * enforces authenticated Google Sign-In ownership under `/users/{uid}/{collection}`, preserves
 * cooperative coroutine cancellation, and logs structured diagnostics via [handleFirestoreError].
 */
class DataManager(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = runCatching { FirebaseAuth.getInstance(db.app) }.getOrElse { Firebase.auth },
    val databaseId: String = NAMED_DATABASE_ID
) {
    init {
        validateDatabaseId(databaseId)
    }

    /**
     * Multi-instance constructor binding explicitly to the specified [FirebaseApp] and named
     * Firestore database ID (never the `(default)` database).
     */
    constructor(
        firebaseApp: FirebaseApp,
        databaseId: String = NAMED_DATABASE_ID
    ) : this(
        db = FirebaseFirestore.getInstance(firebaseApp, validateDatabaseId(databaseId)),
        auth = FirebaseAuth.getInstance(firebaseApp),
        databaseId = validateDatabaseId(databaseId)
    )

    /**
     * Context-based constructor resolving `R.string.firestore_database_id` (falling back to
     * [NAMED_DATABASE_ID]) and attaching to the specified [FirebaseApp] instance.
     */
    constructor(
        context: Context,
        firebaseApp: FirebaseApp = FirebaseApp.getInstance()
    ) : this(
        firebaseApp = firebaseApp,
        databaseId = resolveDatabaseId(context)
    )

    companion object {
        const val NAMED_DATABASE_ID = "ai-studio-android-novaforg-6912163c-aa3c-4903-a89e-6df061c893e0"
        private const val DEFAULT_DATABASE_ID = "(default)"

        const val COLLECTION_PROJECTS = "projects"
        const val COLLECTION_SCHEMAS = "collections"
        const val COLLECTION_BIO_EXPERIMENTS = "bio_experiments"
        const val COLLECTION_SNAPSHOTS = "snapshots"
        const val COLLECTION_LOGS = "logs"

        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

        /**
         * Validates that [databaseId] is non-blank and does not point to the unprovisioned
         * `(default)` Firestore database.
         */
        fun validateDatabaseId(databaseId: String): String {
            val trimmed = databaseId.trim()
            require(trimmed.isNotEmpty()) {
                "Firestore database ID must not be blank; expected '$NAMED_DATABASE_ID'."
            }
            require(!trimmed.equals(DEFAULT_DATABASE_ID, ignoreCase = true)) {
                "Default Firestore database '(default)' is not permitted; use '$NAMED_DATABASE_ID'."
            }
            return trimmed
        }

        /**
         * Resolves the named Firestore database ID from `R.string.firestore_database_id`,
         * defaulting to [NAMED_DATABASE_ID] if the resource is unavailable.
         */
        fun resolveDatabaseId(context: Context): String {
            val fromRes = runCatching {
                context.applicationContext.getString(R.string.firestore_database_id).trim()
            }.getOrNull()
            return validateDatabaseId(
                fromRes?.takeIf { it.isNotEmpty() && !it.equals(DEFAULT_DATABASE_ID, ignoreCase = true) }
                    ?: NAMED_DATABASE_ID
            )
        }

        /**
         * Creates a [DataManager] explicitly targeting [databaseId] on the given [FirebaseApp]
         * instance for Firebase multi-instance support.
         */
        fun forDatabase(
            firebaseApp: FirebaseApp = FirebaseApp.getInstance(),
            databaseId: String = NAMED_DATABASE_ID
        ): DataManager = DataManager(firebaseApp = firebaseApp, databaseId = databaseId)

        /**
         * Creates a [DataManager] from [Context], explicitly targeting `R.string.firestore_database_id`
         * (`ai-studio-android-novaforg-6912163c-aa3c-4903-a89e-6df061c893e0`).
         */
        fun fromContext(
            context: Context,
            firebaseApp: FirebaseApp = FirebaseApp.getInstance()
        ): DataManager = DataManager(context = context, firebaseApp = firebaseApp)

        internal fun sanitizeCreatePayload(
            userId: String,
            data: Map<String, Any?>,
            includeUpdatedAt: Boolean = true
        ): Map<String, Any> {
            val cleaned = LinkedHashMap<String, Any>()
            data.forEach { (key, value) ->
                if (key != "id" && value != null) {
                    cleaned[key] = value
                }
            }
            cleaned["userId"] = userId
            cleaned.putIfAbsent("createdAt", FieldValue.serverTimestamp())
            if (includeUpdatedAt) {
                cleaned["updatedAt"] = FieldValue.serverTimestamp()
            }
            return cleaned
        }

        internal fun sanitizeUpdatePayload(updates: Map<String, Any?>): Map<String, Any> {
            val cleaned = LinkedHashMap<String, Any>()
            updates.forEach { (key, value) ->
                if (key != "id" && key != "userId" && key != "createdAt" && value != null) {
                    cleaned[key] = value
                }
            }
            cleaned["updatedAt"] = FieldValue.serverTimestamp()
            return cleaned
        }

        internal fun validateDocumentId(id: String, label: String) {
            require(id.isNotBlank() && VALID_ID_REGEX.matches(id)) {
                "$label must be 1..128 alphanumeric, underscore, or hyphen characters."
            }
        }
    }

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    private fun userCollectionRef(uid: String, collectionName: String) =
        db.collection("users").document(uid).collection(collectionName)

    // -------------------------------------------------------------------------
    // Generic Coroutine CRUD Operations (Add, Read, Update, Delete)
    // -------------------------------------------------------------------------

    /**
     * Adds a new user-owned document to `/users/{uid}/{collectionName}` and returns its document ID.
     */
    suspend fun addDocument(
        collectionName: String,
        data: Map<String, Any?>,
        documentId: String? = null,
        includeUpdatedAt: Boolean = true
    ): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName"
        return try {
            val uid = requireUserId()
            validateDocumentId(collectionName, "Collection name")
            val resolvedId = documentId?.trim()?.takeIf { it.isNotEmpty() }
                ?: "doc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            validateDocumentId(resolvedId, "Document ID")

            val docRef = userCollectionRef(uid, collectionName).document(resolvedId)
            path = docRef.path
            val payload = sanitizeCreatePayload(
                userId = uid,
                data = data,
                includeUpdatedAt = includeUpdatedAt
            )
            docRef.set(payload).await()
            Result.success(resolvedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Reads a single typed document from `/users/{uid}/{collectionName}/{documentId}`.
     * Uses [DocumentSnapshot.ServerTimestampBehavior.ESTIMATE] for pending server timestamps.
     */
    suspend fun <T : Any> readDocument(
        collectionName: String,
        documentId: String,
        clazz: Class<T>
    ): Result<T?> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName/$documentId"
        return try {
            val uid = requireUserId()
            validateDocumentId(collectionName, "Collection name")
            validateDocumentId(documentId, "Document ID")

            val docRef = userCollectionRef(uid, collectionName).document(documentId)
            path = docRef.path
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                Result.success(null)
            } else {
                Result.success(
                    snapshot.toObject(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Reads a single raw document map from `/users/{uid}/{collectionName}/{documentId}`.
     */
    suspend fun readDocumentData(
        collectionName: String,
        documentId: String
    ): Result<Map<String, Any>?> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName/$documentId"
        return try {
            val uid = requireUserId()
            validateDocumentId(collectionName, "Collection name")
            validateDocumentId(documentId, "Document ID")

            val docRef = userCollectionRef(uid, collectionName).document(documentId)
            path = docRef.path
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                Result.success(null)
            } else {
                Result.success(snapshot.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Reads all user-owned documents in `/users/{uid}/{collectionName}` with mandatory
     * `.whereEqualTo("userId", uid)` query alignment for Firestore security rules.
     */
    suspend fun <T : Any> readCollection(
        collectionName: String,
        clazz: Class<T>,
        limitCount: Long? = null
    ): Result<List<T>> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName"
        return try {
            val uid = requireUserId()
            validateDocumentId(collectionName, "Collection name")

            val colRef = userCollectionRef(uid, collectionName)
            path = colRef.path
            var query = colRef.whereEqualTo("userId", uid)
            if (limitCount != null && limitCount > 0L) {
                query = query.limit(limitCount)
            }
            val snapshot = query.get().await()
            Result.success(
                snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Performs a partial update on `/users/{uid}/{collectionName}/{documentId}`, preserving
     * immutable `userId` and `createdAt` fields and updating `updatedAt` with a server timestamp.
     */
    suspend fun updateDocument(
        collectionName: String,
        documentId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName/$documentId"
        return try {
            val uid = requireUserId()
            validateDocumentId(collectionName, "Collection name")
            validateDocumentId(documentId, "Document ID")

            val docRef = userCollectionRef(uid, collectionName).document(documentId)
            path = docRef.path
            val payload = sanitizeUpdatePayload(updates)
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Deletes a user-owned document at `/users/{uid}/{collectionName}/{documentId}`.
     */
    suspend fun deleteDocument(
        collectionName: String,
        documentId: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName/$documentId"
        return try {
            val uid = requireUserId()
            validateDocumentId(collectionName, "Collection name")
            validateDocumentId(documentId, "Document ID")

            val docRef = userCollectionRef(uid, collectionName).document(documentId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Real-Time Coroutine Flows (Two-Tier Cancellation-Safe Error Handling)
    // -------------------------------------------------------------------------

    /**
     * Observes all user-owned documents in `/users/{uid}/{collectionName}` in real time.
     */
    fun <T : Any> observeCollection(
        collectionName: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        validateDocumentId(collectionName, "Collection name")
        val path = "users/$uid/$collectionName"
        emitAll(
            userCollectionRef(uid, collectionName)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Observes a single user-owned document at `/users/{uid}/{collectionName}/{documentId}`.
     */
    fun <T : Any> observeDocument(
        collectionName: String,
        documentId: String,
        clazz: Class<T>
    ): Flow<T?> = flow {
        val uid = requireUserId()
        validateDocumentId(collectionName, "Collection name")
        validateDocumentId(documentId, "Document ID")
        val path = "users/$uid/$collectionName/$documentId"
        emitAll(
            userCollectionRef(uid, collectionName)
                .document(documentId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) {
                        null
                    } else {
                        snapshot.toObject(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    // -------------------------------------------------------------------------
    // Typed App-Document Convenience CRUD Methods (Rules-Aligned)
    // -------------------------------------------------------------------------

    suspend fun addProject(
        name: String,
        description: String,
        language: String,
        htmlCode: String = "",
        cssCode: String = "",
        jsCode: String = "",
        backendCode: String = ""
    ): Result<String> {
        val safeName = name.trim().ifEmpty { "Untitled Project" }.take(120)
        val safeLanguage = language.trim().ifEmpty { "Full-Stack Web" }.take(50)
        return addDocument(
            collectionName = COLLECTION_PROJECTS,
            documentId = "proj_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            data = mapOf(
                "name" to safeName,
                "description" to description.take(1000),
                "language" to safeLanguage,
                "htmlCode" to htmlCode.take(50000),
                "cssCode" to cssCode.take(50000),
                "jsCode" to jsCode.take(50000),
                "backendCode" to backendCode.take(50000)
            )
        )
    }

    suspend fun readProject(projectId: String): Result<StudioProject?> =
        readDocument(COLLECTION_PROJECTS, projectId, StudioProject::class.java)

    suspend fun readProjects(limitCount: Long? = null): Result<List<StudioProject>> =
        readCollection(COLLECTION_PROJECTS, StudioProject::class.java, limitCount)

    suspend fun updateProjectFields(
        projectId: String,
        name: String? = null,
        description: String? = null,
        language: String? = null,
        htmlCode: String? = null,
        cssCode: String? = null,
        jsCode: String? = null,
        backendCode: String? = null
    ): Result<Unit> {
        val updates = buildMap<String, Any?> {
            name?.trim()?.takeIf { it.isNotEmpty() }?.let { put("name", it.take(120)) }
            description?.let { put("description", it.take(1000)) }
            language?.trim()?.takeIf { it.isNotEmpty() }?.let { put("language", it.take(50)) }
            htmlCode?.let { put("htmlCode", it.take(50000)) }
            cssCode?.let { put("cssCode", it.take(50000)) }
            jsCode?.let { put("jsCode", it.take(50000)) }
            backendCode?.let { put("backendCode", it.take(50000)) }
        }
        return updateDocument(COLLECTION_PROJECTS, projectId, updates)
    }

    suspend fun deleteProject(projectId: String): Result<Unit> =
        deleteDocument(COLLECTION_PROJECTS, projectId)

    suspend fun addSchemaCollection(
        projectId: String,
        collectionName: String,
        fieldsSpec: String,
        sampleRecordsJson: String = "[]",
        indexConfig: String = "userId ASC, updatedAt DESC"
    ): Result<String> {
        return addDocument(
            collectionName = COLLECTION_SCHEMAS,
            documentId = "col_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            data = mapOf(
                "projectId" to projectId.trim().ifEmpty { "default_project" }.take(128),
                "collectionName" to collectionName.trim().ifEmpty { "items" }.take(80),
                "fieldsSpec" to fieldsSpec.trim().ifEmpty { "id:String" }.take(5000),
                "sampleRecordsJson" to sampleRecordsJson.take(20000),
                "indexConfig" to indexConfig.take(500)
            )
        )
    }

    suspend fun readSchemaCollections(limitCount: Long? = null): Result<List<DbCollectionSchema>> =
        readCollection(COLLECTION_SCHEMAS, DbCollectionSchema::class.java, limitCount)

    suspend fun updateSchemaCollectionFields(
        collectionId: String,
        collectionName: String? = null,
        fieldsSpec: String? = null,
        sampleRecordsJson: String? = null,
        indexConfig: String? = null
    ): Result<Unit> {
        val updates = buildMap<String, Any?> {
            collectionName?.trim()?.takeIf { it.isNotEmpty() }?.let { put("collectionName", it.take(80)) }
            fieldsSpec?.trim()?.takeIf { it.isNotEmpty() }?.let { put("fieldsSpec", it.take(5000)) }
            sampleRecordsJson?.let { put("sampleRecordsJson", it.take(20000)) }
            indexConfig?.let { put("indexConfig", it.take(500)) }
        }
        return updateDocument(COLLECTION_SCHEMAS, collectionId, updates)
    }

    suspend fun deleteSchemaCollection(collectionId: String): Result<Unit> =
        deleteDocument(COLLECTION_SCHEMAS, collectionId)

    suspend fun addBioExperiment(
        title: String,
        workflowMode: String,
        fastaSequence: String,
        researchGoal: String,
        plddtScore: Long,
        freeEnergyKcal: Double,
        alphaHelixPct: Long,
        betaSheetPct: Long,
        aiReportSummary: String
    ): Result<String> {
        val allowedModes = setOf("PROTEIN_FOLDING", "CRISPR_EDITING", "GENOMIC_ALIGNMENT", "CLINICAL_TRIALS")
        val safeMode = if (workflowMode in allowedModes) workflowMode else "PROTEIN_FOLDING"
        return addDocument(
            collectionName = COLLECTION_BIO_EXPERIMENTS,
            documentId = "bio_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            data = mapOf(
                "title" to title.trim().ifEmpty { "Bio Experiment" }.take(160),
                "workflowMode" to safeMode,
                "fastaSequence" to fastaSequence.trim().ifEmpty { ">SEQ\nMVLSPADKTNVKAAWGKVGAHAGEYGAEALERMFLSFPTTK" }.take(50000),
                "researchGoal" to researchGoal.take(1000),
                "plddtScore" to plddtScore.coerceIn(0L, 100L),
                "freeEnergyKcal" to freeEnergyKcal.coerceIn(-500.0, 500.0),
                "alphaHelixPct" to alphaHelixPct.coerceIn(0L, 100L),
                "betaSheetPct" to betaSheetPct.coerceIn(0L, 100L),
                "aiReportSummary" to aiReportSummary.take(30000)
            )
        )
    }

    suspend fun readBioExperiments(limitCount: Long? = null): Result<List<BioExperimentRecord>> =
        readCollection(COLLECTION_BIO_EXPERIMENTS, BioExperimentRecord::class.java, limitCount)

    suspend fun deleteBioExperiment(experimentId: String): Result<Unit> =
        deleteDocument(COLLECTION_BIO_EXPERIMENTS, experimentId)
}
