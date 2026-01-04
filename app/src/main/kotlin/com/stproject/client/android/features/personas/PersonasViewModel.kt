package com.stproject.client.android.features.personas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stproject.client.android.core.common.rethrowIfCancellation
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.userMessage
import com.stproject.client.android.core.network.ApiException
import com.stproject.client.android.domain.model.Persona
import com.stproject.client.android.domain.repository.PersonaRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonasUiState(
    val items: List<Persona> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PersonasViewModel
    @Inject
    constructor(
        private val repository: PersonaRepository,
        private val resolveContentAccess: ResolveContentAccessUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PersonasUiState())
        val uiState: StateFlow<PersonasUiState> = _uiState

        fun load(force: Boolean = false) {
            val state = _uiState.value
            if (state.isLoading && !force) return
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    val items = repository.listPersonas()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = items.sortedWith(compareByDescending<Persona> { persona -> persona.isDefault }),
                            error = null,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isLoading = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isLoading = false, error = "unexpected error") }
                }
            }
        }

        fun createPersona(
            name: String,
            description: String?,
            avatarUrl: String?,
            isDefault: Boolean,
        ) {
            if (_uiState.value.isSaving) return
            _uiState.update { it.copy(isSaving = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    repository.createPersona(name, description, avatarUrl, isDefault)
                    _uiState.update { it.copy(isSaving = false) }
                    load(force = true)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSaving = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSaving = false, error = "unexpected error") }
                }
            }
        }

        fun updatePersona(
            persona: Persona,
            name: String,
            description: String?,
            avatarUrl: String?,
            isDefault: Boolean,
        ) {
            if (_uiState.value.isSaving) return
            _uiState.update { it.copy(isSaving = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    repository.updatePersona(
                        personaId = persona.id,
                        name = name,
                        description = description,
                        avatarUrl = avatarUrl,
                        isDefault = isDefault,
                    )
                    _uiState.update { it.copy(isSaving = false) }
                    load(force = true)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSaving = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isSaving = false, error = "unexpected error") }
                }
            }
        }

        fun setDefault(persona: Persona) {
            updatePersona(
                persona = persona,
                name = persona.name,
                description = persona.description,
                avatarUrl = persona.avatarUrl,
                isDefault = true,
            )
        }

        fun deletePersona(persona: Persona) {
            if (_uiState.value.isDeleting) return
            _uiState.update { it.copy(isDeleting = true, error = null) }
            viewModelScope.launch {
                try {
                    if (!ensureAccess()) return@launch
                    repository.deletePersona(persona.id)
                    _uiState.update { it.copy(isDeleting = false) }
                    load(force = true)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isDeleting = false, error = e.userMessage ?: e.message) }
                } catch (e: Exception) {
                    e.rethrowIfCancellation()
                    _uiState.update { it.copy(isDeleting = false, error = "unexpected error") }
                }
            }
        }

        private suspend fun ensureAccess(): Boolean {
            val access = resolveContentAccess.execute(memberId = null, isNsfwHint = false)
            if (access is ContentAccessDecision.Blocked) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaving = false,
                        isDeleting = false,
                        items = emptyList(),
                        error = access.userMessage(),
                    )
                }
                return false
            }
            return true
        }
    }
