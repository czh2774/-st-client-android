package com.stproject.client.android.features.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.core.compliance.PolicyUrlProvider
import com.stproject.client.android.core.theme.ThemeMode

@Composable
fun SettingsScreen(
    uiState: ComplianceUiState,
    onBack: () -> Unit,
    onVerifyAge: (String) -> Unit,
    onDeleteAccount: () -> Unit,
    onAllowNsfwChanged: (Boolean) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onLanguageTagChanged: (String?) -> Unit,
    onOpenModelPresets: () -> Unit,
    onOpenBackgrounds: () -> Unit,
    onOpenDecorations: () -> Unit,
) {
    val context = LocalContext.current
    val policyUrls = remember { PolicyUrlProvider() }
    var showAgeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.common_back))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_privacy_title), style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = {
                        openExternalUrl(context, policyUrls.privacyUrl())
                    },
                    enabled = !uiState.isSubmitting,
                ) {
                    Text(stringResource(R.string.settings_privacy_policy))
                }
                Button(
                    onClick = {
                        openExternalUrl(context, policyUrls.termsUrl())
                    },
                    enabled = !uiState.isSubmitting,
                ) {
                    Text(stringResource(R.string.settings_terms))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_age_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text =
                        stringResource(
                            if (uiState.ageVerified) {
                                R.string.settings_age_verified
                            } else {
                                R.string.settings_age_not_verified
                            },
                        ),
                    color =
                        if (uiState.ageVerified) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
                if (!uiState.ageVerified) {
                    Button(
                        onClick = { showAgeDialog = true },
                        enabled = !uiState.isSubmitting,
                    ) {
                        Text(stringResource(R.string.settings_age_verify_button))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_content_title), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column {
                        Text(stringResource(R.string.settings_content_allow_mature))
                        if (!uiState.ageVerified) {
                            Text(
                                text = stringResource(R.string.settings_content_verify_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Switch(
                        checked = uiState.allowNsfw,
                        onCheckedChange = onAllowNsfwChanged,
                        enabled = uiState.ageVerified && !uiState.isSubmitting,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeModeButton(
                        label = stringResource(R.string.settings_theme_system),
                        selected = uiState.themeMode == ThemeMode.System,
                        onClick = { onThemeModeChanged(ThemeMode.System) },
                    )
                    ThemeModeButton(
                        label = stringResource(R.string.settings_theme_light),
                        selected = uiState.themeMode == ThemeMode.Light,
                        onClick = { onThemeModeChanged(ThemeMode.Light) },
                    )
                    ThemeModeButton(
                        label = stringResource(R.string.settings_theme_dark),
                        selected = uiState.themeMode == ThemeMode.Dark,
                        onClick = { onThemeModeChanged(ThemeMode.Dark) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeModeButton(
                        label = stringResource(R.string.settings_language_system),
                        selected = uiState.languageTag.isNullOrBlank(),
                        onClick = { onLanguageTagChanged(null) },
                    )
                    ThemeModeButton(
                        label = stringResource(R.string.settings_language_english),
                        selected = uiState.languageTag == "en",
                        onClick = { onLanguageTagChanged("en") },
                    )
                    ThemeModeButton(
                        label = stringResource(R.string.settings_language_zh_cn),
                        selected = uiState.languageTag == "zh-CN",
                        onClick = { onLanguageTagChanged("zh-CN") },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_model_presets_title), style = MaterialTheme.typography.titleMedium)
                Button(onClick = onOpenModelPresets, enabled = !uiState.isSubmitting) {
                    Text(stringResource(R.string.model_presets_title))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_customize_title), style = MaterialTheme.typography.titleMedium)
                Button(onClick = onOpenBackgrounds, enabled = !uiState.isSubmitting) {
                    Text(stringResource(R.string.settings_backgrounds_title))
                }
                Button(onClick = onOpenDecorations, enabled = !uiState.isSubmitting) {
                    Text(stringResource(R.string.settings_decorations_title))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_account_title), style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { showDeleteConfirm = true },
                    enabled = !uiState.isSubmitting,
                ) {
                    Text(stringResource(R.string.settings_account_delete))
                }
            }
        }
    }

    AgeVerificationDialog(
        open = showAgeDialog,
        isSubmitting = uiState.isSubmitting,
        allowDismiss = true,
        onDismiss = { showAgeDialog = false },
        onVerified = { birthDate ->
            showAgeDialog = false
            onVerifyAge(birthDate)
        },
        onUnderage = {
            showAgeDialog = false
        },
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_title)) },
            text = { Text(stringResource(R.string.settings_delete_body)) },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    onDeleteAccount()
                }) {
                    Text(stringResource(R.string.settings_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ThemeModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label)
        }
    } else {
        TextButton(onClick = onClick) {
            Text(label)
        }
    }
}

private fun openExternalUrl(
    context: android.content.Context,
    url: String,
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
