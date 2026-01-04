package com.stproject.client.android.features.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stproject.client.android.R
import com.stproject.client.android.core.flags.PlayFeatureFlags
import com.stproject.client.android.domain.model.FanBadge

@Composable
fun MyBadgesScreen(
    viewModel: MyBadgesViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (!PlayFeatureFlags.badgesEnabled) {
        FeatureUnavailableScreen(
            title = stringResource(R.string.badges_title_my),
            onBack = onBack,
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.common_back))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = stringResource(R.string.badges_title_my), style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = { viewModel.load(force = true) }) {
                    Text(stringResource(R.string.common_refresh))
                }
            }

            if (uiState.error != null) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Text(stringResource(R.string.badges_empty_my))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = uiState.items, key = { it.id }) { badge ->
                        FanBadgeRow(
                            badge = badge,
                            actionLabel =
                                stringResource(
                                    if (badge.equipped) {
                                        R.string.badges_unequip
                                    } else {
                                        R.string.badges_equip
                                    },
                                ),
                            actionEnabled = !uiState.isEquipping,
                            onAction = {
                                viewModel.toggleEquip(badge.id, !badge.equipped)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorBadgesScreen(
    creatorId: String,
    viewModel: CreatorBadgesViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var purchaseTarget by remember { mutableStateOf<FanBadge?>(null) }

    LaunchedEffect(creatorId) {
        viewModel.load(creatorId)
    }

    if (!PlayFeatureFlags.badgesEnabled) {
        FeatureUnavailableScreen(
            title = stringResource(R.string.badges_title_creator),
            onBack = onBack,
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.common_back))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.badges_title_creator),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                TextButton(onClick = { viewModel.load(creatorId, force = true) }) {
                    Text(stringResource(R.string.common_refresh))
                }
            }

            if (uiState.error != null) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Text(stringResource(R.string.badges_empty_creator))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = uiState.items, key = { it.id }) { badge ->
                        val owned = badge.owned
                        FanBadgeRow(
                            badge = badge,
                            actionLabel =
                                if (owned) {
                                    stringResource(R.string.badges_owned)
                                } else {
                                    stringResource(R.string.badges_purchase_price, badge.priceDiamonds)
                                },
                            actionEnabled = !owned && !uiState.isPurchasing,
                            onAction = { purchaseTarget = badge },
                        )
                    }
                }
            }
        }
    }

    if (purchaseTarget != null) {
        val badge = purchaseTarget
        AlertDialog(
            onDismissRequest = { purchaseTarget = null },
            title = { Text(stringResource(R.string.badges_purchase_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.badges_purchase_confirm,
                        badge?.priceDiamonds ?: 0,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = badge
                        if (target != null) {
                            viewModel.purchaseBadge(target.id)
                        }
                        purchaseTarget = null
                    },
                    enabled = !uiState.isPurchasing,
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { purchaseTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun FanBadgeRow(
    badge: FanBadge,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badge.imageUrl != null) {
                    AsyncImage(
                        model = badge.imageUrl,
                        contentDescription = badge.name,
                        modifier = Modifier.size(48.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = badge.name, style = MaterialTheme.typography.titleMedium)
                    if (!badge.creatorName.isNullOrBlank()) {
                        Text(
                            text = badge.creatorName ?: "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.badges_level, badge.level),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (badge.equipped) {
            Text(
                text = stringResource(R.string.badges_equipped),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = onAction, enabled = actionEnabled) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun FeatureUnavailableScreen(
    title: String,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.common_back))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
            Text(stringResource(R.string.feature_unavailable))
        }
    }
}
