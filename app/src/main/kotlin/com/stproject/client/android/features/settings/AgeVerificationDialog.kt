package com.stproject.client.android.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun AgeVerificationDialog(
    open: Boolean,
    isSubmitting: Boolean,
    allowDismiss: Boolean,
    onDismiss: () -> Unit,
    onVerified: (String) -> Unit,
    onUnderage: () -> Unit,
) {
    if (!open) return

    var birthDate by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showUnderage by remember { mutableStateOf(false) }
    val invalidBirthDate = stringResource(R.string.age_verify_invalid)

    AlertDialog(
        onDismissRequest = { if (allowDismiss) onDismiss() },
        title = { Text(stringResource(R.string.age_verify_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = birthDate,
                    onValueChange = { value ->
                        birthDate = value
                        error = null
                    },
                    label = { Text(stringResource(R.string.age_verify_birth_label)) },
                )
                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val age = calculateAge(birthDate)
                    when {
                        age == null -> error = invalidBirthDate
                        age < 18 -> {
                            error = null
                            showUnderage = true
                        }
                        else -> onVerified(birthDate)
                    }
                },
                enabled = !isSubmitting,
            ) {
                Text(stringResource(R.string.age_verify_confirm))
            }
        },
        dismissButton = {
            if (allowDismiss) {
                TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        },
    )

    if (showUnderage) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.age_verify_restricted_title)) },
            text = { Text(stringResource(R.string.age_verify_restricted_body)) },
            confirmButton = {
                Button(onClick = {
                    showUnderage = false
                    onUnderage()
                }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }
}

private fun calculateAge(rawDate: String): Int? {
    val trimmed = rawDate.trim()
    if (trimmed.isEmpty()) return null
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val birth =
        try {
            LocalDate.parse(trimmed, formatter)
        } catch (_: DateTimeParseException) {
            return null
        }
    val now = LocalDate.now()
    var age = now.year - birth.year
    if (now.dayOfYear < birth.dayOfYear) {
        age--
    }
    return age
}
