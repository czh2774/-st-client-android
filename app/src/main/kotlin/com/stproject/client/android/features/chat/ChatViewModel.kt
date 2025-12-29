package com.stproject.client.android.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.domain.repository.ChatRepository
import com.stproject.client.android.domain.usecase.SendUserMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sendUserMessage: SendUserMessageUseCase
) : ViewModel() {
    private val input = MutableStateFlow("")
    private val isSending = MutableStateFlow(false)

    val uiState: StateFlow<ChatUiState> =
        combine(chatRepository.messages, input, isSending) { messages, inputText, sending ->
            ChatUiState(
                messages = messages,
                input = inputText,
                isSending = sending
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    fun onInputChanged(value: String) {
        input.value = value
    }

    fun onSendClicked() {
        val content = input.value.trim()
        if (content.isEmpty()) return

        viewModelScope.launch {
            isSending.value = true
            try {
                sendUserMessage(content)
                input.value = ""
            } finally {
                isSending.value = false
            }
        }
    }
}


