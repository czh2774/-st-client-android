package com.stproject.client.android.domain.model

data class Persona(
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val avatarUrl: String?,
    val isDefault: Boolean,
    val createdAt: String,
    val updatedAt: String,
)
