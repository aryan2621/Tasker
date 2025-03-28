package com.tasker.data.repository

import com.google.firebase.auth.AuthCredential
import com.tasker.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val cachedUserId: String? // Add this property
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, displayName: String): Result<User>
    suspend fun signInWithGoogle(credential: AuthCredential): Result<User>
    suspend fun signOut()
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateUserProfile(displayName: String?, photoUrl: String?): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun isUserAuthenticated(): Boolean
    suspend fun getCurrentUserId() : String?
}