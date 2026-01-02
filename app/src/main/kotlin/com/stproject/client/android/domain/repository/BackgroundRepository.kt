package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.BackgroundList

interface BackgroundRepository {
    suspend fun listBackgrounds(): BackgroundList

    suspend fun uploadBackground(
        fileName: String,
        bytes: ByteArray,
    ): String

    suspend fun renameBackground(
        oldName: String,
        newName: String,
    )

    suspend fun deleteBackground(name: String)
}
