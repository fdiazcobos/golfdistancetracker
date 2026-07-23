package com.example.golfdistancetracker.auth

import android.content.Context
import androidx.credentials.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class UserInfo(val email: String, val name: String?)

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)
    private val _userState = MutableStateFlow<UserInfo?>(null)
    val userState = _userState.asStateFlow()

    suspend fun signIn(activityContext: Context) {
        android.util.Log.d("AuthManager", "signIn called")
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("REPLACE_WITH_YOUR_CLIENT_ID.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            android.util.Log.d("AuthManager", "Requesting credentials...")
            val result = credentialManager.getCredential(activityContext, request)
            android.util.Log.d("AuthManager", "Credentials received")
            _userState.value = UserInfo("golf.user@example.com", "Golf Player")
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "SignIn failed, using mock fallback", e)
            android.widget.Toast.makeText(activityContext, "Google Login failed: ${e.message}. Using test account.", android.widget.Toast.LENGTH_LONG).show()
            // Mock fallback so the user can test the app
            _userState.value = UserInfo("test.user@example.com", "Test User")
        }
    }

    fun signOut() {
        _userState.value = null
    }
}
