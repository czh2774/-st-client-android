package com.stproject.client.android.features.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.PolicyUrlProvider

@Composable
fun PrivacyConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isSubmitting: Boolean,
) {
    val context = LocalContext.current
    val policyUrls = remember { PolicyUrlProvider() }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.privacy_dialog_title)) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    text = stringResource(R.string.privacy_dialog_body),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(
                    onClick = {
                        openExternalUrl(context, policyUrls.privacyUrl())
                    },
                    enabled = !isSubmitting,
                ) {
                    Text(stringResource(R.string.settings_privacy_policy))
                }
                TextButton(
                    onClick = {
                        openExternalUrl(context, policyUrls.termsUrl())
                    },
                    enabled = !isSubmitting,
                ) {
                    Text(stringResource(R.string.settings_terms))
                }
            }
        },
        confirmButton = {
            Button(onClick = onAccept, enabled = !isSubmitting) {
                Text(stringResource(R.string.privacy_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline, enabled = !isSubmitting) {
                Text(stringResource(R.string.privacy_decline))
            }
        },
    )

    // Policy links are rendered inside the dialog body.
}

private fun openExternalUrl(
    context: android.content.Context,
    url: String,
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
