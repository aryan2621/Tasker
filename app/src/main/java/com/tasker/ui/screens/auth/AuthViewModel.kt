package com.tasker.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.tasker.data.domain.*
import com.tasker.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AuthViewModel : ViewModel(), KoinComponent {

    private val signInUseCase: SignInUseCase by inject()
    private val signUpUseCase: SignUpUseCase by inject()
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase by inject()
    private val signOutUseCase: SignOutUseCase by inject()
    private val resetPasswordUseCase: ResetPasswordUseCase by inject()
    private val getCurrentUserUseCase: GetCurrentUserUseCase by inject()
    private val checkAuthStateUseCase: CheckAuthStateUseCase by inject()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    val currentUser = getCurrentUserUseCase.execute()

    init {
        // Check if user is already logged in
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val isAuthenticated = checkAuthStateUseCase.execute()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = isAuthenticated
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = signInUseCase.execute(email, password)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = result.isSuccess,
                    error = result.exceptionOrNull()?.let { e -> getErrorMessage(e) }
                )
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            signUpUseCase.execute(email, password, displayName)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = getErrorMessage(e)) }
                }
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            signInWithGoogleUseCase.execute(credential)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = getErrorMessage(e)) }
                }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            resetPasswordUseCase.execute(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Password reset email sent. Please check your inbox."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = getErrorMessage(e)) }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase.execute()
            _uiState.update { it.copy(isLoggedIn = false) }
        }
    }


    private fun getErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("no user record") == true ->
                "No account exists with this email."
            exception.message?.contains("password is invalid") == true ->
                "Incorrect password."
            exception.message?.contains("email address is badly formatted") == true ->
                "Invalid email format."
            exception.message?.contains("email address is already in use") == true ->
                "Email already in use."
            exception.message?.contains("password is too weak") == true ->
                "Password is too weak."
            else -> exception.message ?: "An unknown error occurred."
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)
