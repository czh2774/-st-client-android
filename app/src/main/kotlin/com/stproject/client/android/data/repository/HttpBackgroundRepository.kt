package com.stproject.client.android.data.repository

import android.net.Uri
import com.stproject.client.android.core.network.ApiClient
import com.stproject.client.android.core.network.BackgroundDeleteRequestDto
import com.stproject.client.android.core.network.BackgroundRenameRequestDto
import com.stproject.client.android.core.network.StBackgroundApi
import com.stproject.client.android.core.network.StBaseUrlProvider
import com.stproject.client.android.domain.model.BackgroundConfig
import com.stproject.client.android.domain.model.BackgroundItem
import com.stproject.client.android.domain.model.BackgroundList
import com.stproject.client.android.domain.repository.BackgroundRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpBackgroundRepository
    @Inject
    constructor(
        private val api: StBackgroundApi,
        private val apiClient: ApiClient,
        private val baseUrlProvider: StBaseUrlProvider,
    ) : BackgroundRepository {
        override suspend fun listBackgrounds(): BackgroundList {
            val response = apiClient.call { api.listBackgrounds() }
            val baseUrl = rootUrl()
            val items =
                response.images
                    ?.mapNotNull { raw ->
                        val name = raw?.trim().orEmpty()
                        if (name.isEmpty()) return@mapNotNull null
                        BackgroundItem(
                            name = name,
                            url = "$baseUrl/backgrounds/${Uri.encode(name)}",
                        )
                    }
                    ?: emptyList()
            val config =
                response.config?.let { cfg ->
                    val width = cfg.width ?: return@let null
                    val height = cfg.height ?: return@let null
                    BackgroundConfig(width = width, height = height)
                }
            return BackgroundList(items = items, config = config)
        }

        override suspend fun uploadBackground(
            fileName: String,
            bytes: ByteArray,
        ): String {
            val cleanName = fileName.trim().ifEmpty { "background.png" }
            val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", cleanName, requestBody)
            return apiClient.call { api.uploadBackground(part) }
        }

        override suspend fun renameBackground(
            oldName: String,
            newName: String,
        ) {
            apiClient.call {
                api.renameBackground(
                    BackgroundRenameRequestDto(
                        oldName = oldName.trim(),
                        newName = newName.trim(),
                    ),
                )
            }
        }

        override suspend fun deleteBackground(name: String) {
            apiClient.call { api.deleteBackground(BackgroundDeleteRequestDto(name = name.trim())) }
        }

        private fun rootUrl(): String {
            val base = baseUrlProvider.baseUrl().trimEnd('/')
            val apiIndex = base.indexOf("/api/")
            return if (apiIndex == -1) base else base.substring(0, apiIndex)
        }
    }
