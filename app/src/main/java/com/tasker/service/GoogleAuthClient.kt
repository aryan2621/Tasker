package com.tasker.service

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthClient(private val context: Context) {
    private val webClientId = "270744937543-5ugk3pan373fujh8egclfoik6n7b0sjv.apps.googleusercontent.com"

    private val client: GoogleSignInClient = GoogleSignIn.getClient(context, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build())

    fun getSignInIntent(): Intent {
        return client.signInIntent
    }

    suspend fun getCredential(intent: Intent): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            Result.success(task.await())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            client.signOut().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFirebaseCredential(account: GoogleSignInAccount) =
        GoogleAuthProvider.getCredential(account.idToken!!, null)
}