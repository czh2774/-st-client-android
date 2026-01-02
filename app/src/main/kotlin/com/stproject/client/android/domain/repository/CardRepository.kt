package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.CardCreateInput
import com.stproject.client.android.domain.model.CardCreateResult

interface CardRepository {
    suspend fun createCard(input: CardCreateInput): CardCreateResult

    suspend fun createCardFromWrapper(wrapper: Map<String, Any>): CardCreateResult

    suspend fun updateCardFromWrapper(
        id: String,
        wrapper: Map<String, Any>,
    ): CardCreateResult

    suspend fun fetchCardWrapper(id: String): Map<String, Any>

    suspend fun fetchExportPng(id: String): ByteArray

    suspend fun parseCardFile(
        fileName: String,
        bytes: ByteArray,
    ): Map<String, Any>

    suspend fun parseCardText(
        content: String,
        fileName: String?,
    ): Map<String, Any>

    suspend fun fetchTemplate(): Map<String, Any>
}
