package com.stproject.client.android.features.chat

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.usecase.SendUserMessageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatViewModelTest : BaseUnitTest() {
    private class FakeChatRepository : ChatRepository {
        private val _messages = MutableStateFlow(emptyList<com.stproject.client.android.domain.model.ChatMessage>())
        override val messages: Flow<List<com.stproject.client.android.domain.model.ChatMessage>> = _messages.asStateFlow()

        override suspend fun sendUserMessage(content: String) {
            // no-op: we only test that ViewModel clears input and toggles sending.
        }
    }

    @Test
    fun `send clears input`() = runTest {
        val repo = FakeChatRepository()
        val vm = ChatViewModel(
            chatRepository = repo,
            sendUserMessage = SendUserMessageUseCase(repo)
        )

        vm.onInputChanged(" hello ")
        vm.onSendClicked()
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.input)
    }
}


