package com.stproject.client.android.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import com.stproject.client.android.domain.model.DecorationItem

private data class DecorationType(
    val id: String,
    val labelRes: Int,
)

private val DECORATION_TYPES =
    listOf(
        DecorationType("avatar_frame", R.string.decoration_type_avatar_frame),
        DecorationType("title", R.string.decoration_type_title),
        DecorationType("bubble", R.string.decoration_type_bubble),
        DecorationType("background", R.string.decoration_type_background),
        DecorationType("effect", R.string.decoration_type_effect),
    )

@Composable
fun DecorationsScreen(
    viewModel: DecorationsViewModel,
    onBack: () -> Unit,
    onOpenShop: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeType by remember { mutableStateOf(DECORATION_TYPES.first().id) }
    val filtered = uiState.items.filter { it.type == activeType }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.common_back))
                }
                Text(
                    text = stringResource(R.string.decorations_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = viewModel::load, enabled = !uiState.isLoading) {
                    Text(stringResource(R.string.common_refresh))
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.decorations_description), style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DECORATION_TYPES.forEach { type ->
                        DecorationTypeButton(
                            label = stringResource(type.labelRes),
                            selected = activeType == type.id,
                            onClick = { activeType = type.id },
                        )
                    }
                }
            }

            if (uiState.isLoading && uiState.items.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
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

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (filtered.isEmpty() && !uiState.isLoading) {
                    item(key = "empty") {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(stringResource(R.string.decorations_empty))
                            Text(
                                text = stringResource(R.string.decorations_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = onOpenShop) {
                                    Text(stringResource(R.string.shop_title))
                                }
                            }
                        }
                    }
                }

                items(items = filtered, key = { it.id }) { item ->
                    DecorationRow(
                        item = item,
                        isBusy = uiState.isSubmitting,
                        onToggle = {
                            viewModel.setEquipped(item.id, !item.equipped)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DecorationTypeButton(
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

@Composable
private fun DecorationRow(
    item: DecorationItem,
    isBusy: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.imageUrl.isNullOrBlank()) {
                Column(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.decorations_no_preview),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleSmall)
                if (!item.description.isNullOrBlank()) {
                    Text(text = item.description ?: "", style = MaterialTheme.typography.bodySmall)
                }
                if (!item.owned) {
                    Text(
                        text = stringResource(R.string.decorations_not_owned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (item.priceCredits != null) {
                    Text(
                        text = stringResource(R.string.decorations_price_credits, item.priceCredits),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (item.equipped) {
                    Text(
                        text = stringResource(R.string.decorations_equipped),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val actionLabel =
                if (item.equipped) {
                    R.string.decorations_unequip
                } else {
                    R.string.decorations_equip
                }
            Button(
                onClick = onToggle,
                enabled = item.owned && !isBusy,
            ) {
                Text(stringResource(actionLabel))
            }
        }
    }
}
