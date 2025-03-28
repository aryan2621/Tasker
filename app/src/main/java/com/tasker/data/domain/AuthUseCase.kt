package com.tasker.data.domain

import com.google.firebase.auth.AuthCredential
import com.tasker.data.model.User
import com.tasker.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class SignInWithGoogleUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(credential: AuthCredential): Result<User> {
        return authRepository.signInWithGoogle(credential)
    }
}

class SignOutUseCase(private val authRepository: AuthRepository) {
    suspend fun execute() {
        authRepository.signOut()
    }
}

class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(email: String): Result<Unit> {
        return authRepository.resetPassword(email)
    }
}

class GetCurrentUserUseCase(private val authRepository: AuthRepository) {
    fun execute(): Flow<User?> {
        return authRepository.currentUser
    }
}

class UpdateUserProfileUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(displayName: String?, photoUrl: String?): Result<Unit> {
        return authRepository.updateUserProfile(displayName, photoUrl)
    }
}

class DeleteAccountUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(): Result<Unit> {
        return authRepository.deleteAccount()
    }
}

class CheckAuthStateUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(): Boolean {
        return authRepository.isUserAuthenticated()
    }
}

class GetCurrentUserIdUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(): String? {
        return authRepository.getCurrentUserId()
    }
}
class SignInUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(email: String, password: String): Result<User> {
        return authRepository.signIn(email, password)
    }
}

class SignUpUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(email: String, password: String, displayName: String): Result<User> {
        return authRepository.signUp(email, password, displayName)
    }
}
