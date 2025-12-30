package com.stproject.client.android.features.chat

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.usecase.SendUserMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest : BaseUnitTest() {
    private class FakeChatRepository : ChatRepository {
        private val _messages = MutableStateFlow(emptyList<com.stproject.client.android.domain.model.ChatMessage>())
        override val messages: Flow<List<com.stproject.client.android.domain.model.ChatMessage>> = _messages.asStateFlow()

        override suspend fun sendUserMessage(content: String) {
            // no-op: we only test that ViewModel clears input and toggles sending.
        }

        override suspend fun clearLocalSession() = Unit
    }

    @Test
    fun `send clears input`() = runTest(mainDispatcherRule.dispatcher) {
        val repo = FakeChatRepository()
        val vm = ChatViewModel(
            chatRepository = repo,
            sendUserMessage = SendUserMessageUseCase(repo)
        )
        val collectJob = backgroundScope.launch { vm.uiState.collect() }

        vm.onInputChanged(" hello ")
        vm.onSendClicked()
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.input)
        collectJob.cancel()
    }

    @Test
    fun `send toggles isSending while running`() = runTest(mainDispatcherRule.dispatcher) {
        val repo = object : ChatRepository {
            private val _messages = MutableStateFlow(emptyList<com.stproject.client.android.domain.model.ChatMessage>())
            override val messages: Flow<List<com.stproject.client.android.domain.model.ChatMessage>> = _messages.asStateFlow()

            override suspend fun sendUserMessage(content: String) {
                // Suspend until the test advances the dispatcher.
                kotlinx.coroutines.delay(1000)
            }

            override suspend fun clearLocalSession() = Unit
        }

        val vm = ChatViewModel(
            chatRepository = repo,
            sendUserMessage = SendUserMessageUseCase(repo)
        )
        val collectJob = backgroundScope.launch { vm.uiState.collect() }

        vm.onInputChanged("hi")
        vm.onSendClicked()
        runCurrent()

        // Job launched but not completed yet.
        assertTrue(vm.uiState.value.isSending)
        assertNull(vm.uiState.value.error)

        advanceTimeBy(1000)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSending)
        collectJob.cancel()
    }

    @Test
    fun `api exception sets error and does not clear input`() = runTest(mainDispatcherRule.dispatcher) {
        val repo = object : ChatRepository {
            private val _messages = MutableStateFlow(emptyList<com.stproject.client.android.domain.model.ChatMessage>())
            override val messages: Flow<List<com.stproject.client.android.domain.model.ChatMessage>> = _messages.asStateFlow()

            override suspend fun sendUserMessage(content: String) {
                throw ApiException(message = "boom")
            }

            override suspend fun clearLocalSession() = Unit
        }

        val vm = ChatViewModel(
            chatRepository = repo,
            sendUserMessage = SendUserMessageUseCase(repo)
        )
        val collectJob = backgroundScope.launch { vm.uiState.collect() }

        vm.onInputChanged("hello")
        vm.onSendClicked()
        advanceUntilIdle()

        assertEquals("hello", vm.uiState.value.input)
        assertEquals("boom", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSending)
        collectJob.cancel()
    }
}

