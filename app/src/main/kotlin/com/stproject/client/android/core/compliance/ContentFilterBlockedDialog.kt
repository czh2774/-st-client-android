package com.stproject.client.android.core.compliance

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stproject.client.android.R

@Composable
fun ContentFilterBlockedDialog(
    open: Boolean,
    onDismiss: () -> Unit,
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.content_access_blocked_title)) },
        text = { Text(stringResource(R.string.content_blocked_filters_body)) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        },
    )
}
