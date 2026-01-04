package com.stproject.client.android.features.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stproject.client.android.R
import com.stproject.client.android.core.network.userMessage
import com.stproject.client.android.domain.model.WalletTransaction

@Composable
fun WalletScreen(viewModel: WalletViewModel) {
    val uiState by viewModel.uiState.collectAsState()

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
                    Text(text = stringResource(R.string.wallet_title), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::load, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.common_refresh))
                    }
                }
                val balance = uiState.balance
                if (balance != null) {
                    Text(
                        text = stringResource(R.string.wallet_balance_credits, balance.balanceCredits),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.wallet_balance_currency, balance.currency),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = stringResource(R.string.wallet_balance_diamonds, balance.diamonds),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (uiState.isLoading && uiState.transactions.isEmpty()) {
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
                    text = uiState.error?.userMessage() ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                text = stringResource(R.string.wallet_transactions_title),
                style = MaterialTheme.typography.titleSmall,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = uiState.transactions, key = { it.id }) { item ->
                    WalletTransactionRow(item = item)
                }
                if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                    item(key = "empty") {
                        Text(text = stringResource(R.string.wallet_transactions_empty))
                    }
                }
                if (uiState.hasMore) {
                    item(key = "load-more") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(onClick = viewModel::loadMore, enabled = !uiState.isLoading) {
                                Text(
                                    stringResource(
                                        if (uiState.isLoading) {
                                            R.string.common_loading
                                        } else {
                                            R.string.common_load_more
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletTransactionRow(item: WalletTransaction) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.wallet_transaction_type, item.type.ifBlank { "-" }),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.wallet_transaction_amount, item.amountCredits),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!item.reason.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.wallet_transaction_reason, item.reason),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (!item.createdAt.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.wallet_transaction_created, item.createdAt),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
