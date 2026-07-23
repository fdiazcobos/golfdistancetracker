package com.example.golfdistancetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.golfdistancetracker.auth.AuthManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(authManager: AuthManager, onLoginSuccess: () -> Unit) {
    val user by authManager.userState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Golf Distance Tracker", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        
        if (user == null) {
            Button(onClick = {
                scope.launch {
                    authManager.signIn(context)
                }
            }) {
                Text("Sign in with Google")
            }
        } else {
            Text("Welcome, ${user?.name}!")
            Button(onClick = onLoginSuccess) {
                Text("Go to App")
            }
        }
    }
}
