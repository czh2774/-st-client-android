package com.stproject.client.android.features.shop

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R

@Composable
fun ShopScreen(viewModel: ShopViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(R.string.shop_title), style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = viewModel::restorePurchases,
                        enabled = uiState.purchaseEnabled && !uiState.isRestoring,
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isRestoring) {
                                    R.string.shop_restoring
                                } else {
                                    R.string.shop_restore_purchases
                                },
                            ),
                        )
                    }
                }
                if (!uiState.purchaseDisabledReason.isNullOrBlank()) {
                    Text(
                        text = uiState.purchaseDisabledReason ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (uiState.isLoading && uiState.products.isEmpty()) {
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
                items(items = uiState.products, key = { it.productId }) { item ->
                    ShopProductRow(
                        item = item,
                        canPurchase = uiState.purchaseEnabled && activity != null,
                        onPurchase = {
                            val host = activity ?: return@ShopProductRow
                            viewModel.launchPurchase(host, item.productId)
                        },
                    )
                }
                if (uiState.products.isEmpty() && !uiState.isLoading) {
                    item(key = "empty") {
                        TextButton(onClick = viewModel::load) {
                            Text(stringResource(R.string.common_reload))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopProductRow(
    item: ShopProduct,
    canPurchase: Boolean,
    onPurchase: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = item.displayName, style = MaterialTheme.typography.titleSmall)
        if (!item.description.isNullOrBlank()) {
            Text(text = item.description ?: "", style = MaterialTheme.typography.bodyMedium)
        }
        val priceLabel =
            item.price?.let {
                stringResource(R.string.shop_price_label, it)
            } ?: stringResource(R.string.shop_price_unknown)
        Text(text = priceLabel, style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onPurchase, enabled = item.enabled && canPurchase) {
                Text(stringResource(R.string.shop_buy))
            }
        }
    }
}
