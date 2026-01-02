package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.DecorationItem

interface DecorationRepository {
    suspend fun listDecorations(): List<DecorationItem>

    suspend fun setDecorationEquipped(
        decorationId: String,
        equip: Boolean,
    )
}
