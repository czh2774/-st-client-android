package com.stproject.client.android.features.social

import com.stproject.client.android.domain.model.SocialUserSummary

data class SocialUiState(
    val isLoading: Boolean = false,
    val items: List<SocialUserSummary> = emptyList(),
    val error: String? = null,
    val activeTab: SocialTab = SocialTab.Followers,
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    val actionUserId: String = "",
    val targetUserId: String = "",
)
