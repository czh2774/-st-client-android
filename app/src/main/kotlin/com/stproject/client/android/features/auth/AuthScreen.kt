package com.stproject.client.android.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val LOGIN_BUTTON_TEST_TAG = "auth.login"

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineSmall
            )
            if (uiState.error != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.email,
                onValueChange = viewModel::onEmailChanged,
                enabled = !uiState.isSubmitting,
                label = { Text("Email") }
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.password,
                onValueChange = viewModel::onPasswordChanged,
                enabled = !uiState.isSubmitting,
                label = { Text("Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation()
            )
            Button(
                modifier = Modifier.testTag(LOGIN_BUTTON_TEST_TAG),
                onClick = viewModel::onLoginClicked,
                enabled = !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Login")
            }
        }
    }
}
