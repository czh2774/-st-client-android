package com.stproject.client.android.domain.usecase

import com.stproject.client.android.domain.repository.ChatRepository
import javax.inject.Inject

class SendUserMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(content: String) {
        chatRepository.sendUserMessage(content)
    }
}


