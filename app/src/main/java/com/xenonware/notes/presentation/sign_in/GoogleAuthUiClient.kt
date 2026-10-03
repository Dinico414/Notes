package com.xenonware.notes.presentation.sign_in

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.xenonware.notes.R
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class GoogleAuthUiClient(
    private val context: Context,
) {
    private val auth = Firebase.auth

    suspend fun signIn(context: Context = this.context): SignInResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = CredentialManager.create(context).getCredential(context, request)
            val credential = result.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken
                firebaseAuthWithGoogle(googleIdToken)
            } else {
                SignInResult(data = null, errorMessage = "Unexpected credential type.")
            }
        } catch (e: GetCredentialException) {
            SignInResult(data = null, errorMessage = e.message)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            SignInResult(data = null, errorMessage = e.message)
        }
    }

    suspend fun signOut(context: Context = this.context) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            auth.signOut()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }
    }

    private suspend fun firebaseAuthWithGoogle(googleIdToken: String?): SignInResult {
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        return try {
            val user = auth.signInWithCredential(googleCredentials).await().user
            SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid,
                        username = displayName,
                        profilePictureUrl = photoUrl?.toString(),
                        email = email.toString()
                    )
                },
                errorMessage = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    fun getSignedInUser(): UserData? = try {
        auth.currentUser?.let { user ->
            UserData(
                userId = user.uid,
                username = user.displayName,
                profilePictureUrl = user.photoUrl?.toString(),
                email = user.email ?: ""
            )
        }?: run {
            null
        }
    } catch (e: Exception) {
        if (e.message?.contains("auth_api_credentials_begin_sign_in") == true) {
            null
        } else {
            throw e
        }
    }
}
