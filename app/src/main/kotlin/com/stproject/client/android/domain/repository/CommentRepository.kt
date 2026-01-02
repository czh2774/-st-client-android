package com.stproject.client.android.domain.repository

import com.stproject.client.android.domain.model.Comment
import com.stproject.client.android.domain.model.CommentLikeResult
import com.stproject.client.android.domain.model.CommentListResult
import com.stproject.client.android.domain.model.CommentSort

interface CommentRepository {
    suspend fun listComments(
        characterId: String,
        sort: CommentSort,
        pageNum: Int,
        pageSize: Int,
    ): CommentListResult

    suspend fun createComment(
        characterId: String,
        content: String,
        parentId: String?,
    ): Comment

    suspend fun deleteComment(commentId: String)

    suspend fun likeComment(
        commentId: String,
        value: Boolean,
    ): CommentLikeResult
}
