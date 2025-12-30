package com.stproject.client.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.stproject.client.android.features.auth.AuthScreen
import com.stproject.client.android.features.auth.AuthViewModel
import com.stproject.client.android.features.chat.ChatScreen
import com.stproject.client.android.features.chat.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val authState by authViewModel.uiState.collectAsState()
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (authState.isAuthenticated) {
                        AuthenticatedContent(
                            chatViewModel = chatViewModel,
                            onLogout = authViewModel::onLogout
                        )
                    } else if (authState.isRestoring) {
                        AuthLoadingScreen()
                    } else {
                        AuthScreen(viewModel = authViewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedContent(
    chatViewModel: ChatViewModel,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onLogout) {
                Text("Logout")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            ChatScreen(viewModel = chatViewModel)
        }
    }
}

@Composable
private fun AuthLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
