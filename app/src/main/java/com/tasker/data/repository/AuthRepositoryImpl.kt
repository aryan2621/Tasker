package com.tasker.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.tasker.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    override var cachedUserId: String? = auth.currentUser?.uid
        private set

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser?.toUser()
            cachedUserId = auth.currentUser?.uid
            trySend(user)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                cachedUserId = it.uid
                Result.success(it.toUser())
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()

            // Update display name
            result.user?.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
            )?.await()

            result.user?.let {
                cachedUserId = it.uid
                Result.success(it.toUser().copy(displayName = displayName))
            } ?: Result.failure(Exception("Registration failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(credential: AuthCredential): Result<User> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            result.user?.let {
                cachedUserId = it.uid
                Result.success(it.toUser())
            } ?: Result.failure(Exception("Google authentication failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        cachedUserId = null
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(displayName: String?, photoUrl: String?): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not logged in"))

            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
            }.build()

            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
            user.delete().await()
            cachedUserId = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.toUser()?.uid
    }

    private fun FirebaseUser.toUser(): User {
        return User(
            uid = this.uid,
            email = this.email ?: "",
            displayName = this.displayName ?: "",
            photoUrl = this.photoUrl?.toString()
        )
    }
}