package com.stproject.client.android.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R

@Composable
fun ReportDialog(
    state: ModerationUiState,
    onDismiss: () -> Unit,
    onSubmit: (List<String>, String?) -> Unit,
) {
    val selectReasonError = stringResource(R.string.report_error_select_reason)
    val detailRequiredError = stringResource(R.string.report_error_detail_required)
    var selected by remember { mutableStateOf(setOf<String>()) }
    var detail by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.reasons) {
        if (state.reasons.isNotEmpty()) {
            selected = emptySet()
            detail = ""
            localError = null
        }
    }

    val requiresDetail = selected.any { state.requiresDetailReasons.contains(it) }
    val canSubmit = selected.isNotEmpty() && (!requiresDetail || detail.trim().isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isLoadingReasons && state.reasons.isEmpty()) {
                    Text(stringResource(R.string.report_loading_reasons))
                } else {
                    state.reasons.forEach { reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selected.contains(reason),
                                onCheckedChange = { checked ->
                                    selected =
                                        if (checked) {
                                            selected + reason
                                        } else {
                                            selected - reason
                                        }
                                    localError = null
                                },
                            )
                            Text(reason)
                        }
                    }
                }
                if (requiresDetail) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = detail,
                        onValueChange = { value ->
                            detail = value.take(state.maxDetailLength)
                            localError = null
                        },
                        label = { Text(stringResource(R.string.report_detail_label)) },
                    )
                }
                if (localError != null) {
                    Text(
                        text = localError ?: "",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!canSubmit) {
                        localError =
                            if (selected.isEmpty()) {
                                selectReasonError
                            } else {
                                detailRequiredError
                            }
                        return@Button
                    }
                    onSubmit(selected.toList(), detail.takeIf { it.isNotBlank() })
                },
                enabled = canSubmit && !state.isSubmitting,
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(stringResource(R.string.report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
