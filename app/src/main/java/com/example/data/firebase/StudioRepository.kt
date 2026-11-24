package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.model.ProjectStatus
import com.example.model.StudioProject
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeProjects(userId: String): Flow<List<StudioProject>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/projects"
        emitAll(
            db.collection("users").document(userId).collection("projects")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudioProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Authenticated user mismatch." }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Creator")
                    .take(80)
                val safeEmail = (user.email ?: "").take(120)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "planTier" to "FREE",
                    "credits" to 50L,
                    "totalGenerations" to 0L,
                    "preferredLanguage" to "id",
                    "darkTheme" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(userId: String, language: String, darkTheme: Boolean): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user preference update." }
            val validLang = if (language == "en") "en" else "id"
            db.collection("users").document(uid).update(
                mapOf(
                    "preferredLanguage" to validLang,
                    "darkTheme" to darkTheme,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun spendCredits(userId: String, cost: Int): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user credit update." }
            val docRef = db.collection("users").document(uid)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val currentCredits = (snap.getLong("credits") ?: 0L)
                val currentGens = (snap.getLong("totalGenerations") ?: 0L)
                require(currentCredits >= cost) { "Insufficient credits." }
                tx.update(
                    docRef,
                    mapOf(
                        "credits" to (currentCredits - cost).coerceAtLeast(0L),
                        "totalGenerations" to (currentGens + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimDailyCredits(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user credit claim." }
            val docRef = db.collection("users").document(uid)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val currentCredits = (snap.getLong("credits") ?: 0L)
                val currentGens = (snap.getLong("totalGenerations") ?: 0L)
                val nextCredits = (currentCredits + 10L).coerceAtMost(10000L)
                tx.update(
                    docRef,
                    mapOf(
                        "credits" to nextCredits,
                        "totalGenerations" to currentGens,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createProject(project: StudioProject): Result<String> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val docRef = db.collection("users").document(uid).collection("projects").document()
        val path = docRef.path
        return try {
            val verifiedUid = requireUserId()
            val payload = mapOf(
                "userId" to verifiedUid,
                "title" to project.title.ifBlank { "Untitled Project" }.take(120),
                "projectType" to project.projectType,
                "prompt" to project.prompt.ifBlank { "Studio Project" }.take(2000),
                "enhancedPrompt" to project.enhancedPrompt?.take(3000),
                "style" to project.style.take(60),
                "aspectRatio" to project.aspectRatio,
                "resolution" to project.resolution,
                "status" to project.status,
                "creditCost" to project.creditCost.coerceIn(0L, 100L),
                "localMediaPath" to project.localMediaPath?.take(500),
                "interactionId" to project.interactionId?.take(200),
                "scriptJson" to project.scriptJson?.take(15000),
                "subtitlesJson" to project.subtitlesJson?.take(15000),
                "errorMessage" to project.errorMessage?.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun completeProject(
        userId: String,
        projectId: String,
        localMediaPath: String?,
        interactionId: String? = null,
        scriptJson: String? = null,
        subtitlesJson: String? = null
    ): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user project update." }
            require(projectId.isNotBlank()) { "Project ID cannot be blank." }
            val updates = mapOf(
                "status" to ProjectStatus.COMPLETED.name,
                "localMediaPath" to localMediaPath?.take(500),
                "interactionId" to interactionId?.take(200),
                "scriptJson" to scriptJson?.take(15000),
                "subtitlesJson" to subtitlesJson?.take(15000),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("users").document(uid).collection("projects").document(projectId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun failProject(userId: String, projectId: String, errorMessage: String): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user project update." }
            db.collection("users").document(uid).collection("projects").document(projectId)
                .update(
                    mapOf(
                        "status" to ProjectStatus.FAILED.name,
                        "errorMessage" to errorMessage.take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectMediaAndInteraction(
        userId: String,
        projectId: String,
        localMediaPath: String,
        interactionId: String?
    ): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user project update." }
            val updates = mapOf(
                "localMediaPath" to localMediaPath.take(500),
                "interactionId" to interactionId?.take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("users").document(uid).collection("projects").document(projectId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectSubtitles(
        userId: String,
        projectId: String,
        subtitlesJson: String
    ): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user subtitle update." }
            db.collection("users").document(uid).collection("projects").document(projectId)
                .update(
                    mapOf(
                        "subtitlesJson" to subtitlesJson.take(15000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user project delete." }
            db.collection("users").document(uid).collection("projects").document(projectId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
