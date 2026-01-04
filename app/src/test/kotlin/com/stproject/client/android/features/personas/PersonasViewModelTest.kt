package com.stproject.client.android.features.personas

import com.stproject.client.android.BaseUnitTest
import com.stproject.client.android.core.compliance.ContentAccessDecision
import com.stproject.client.android.core.compliance.ContentAccessManager
import com.stproject.client.android.core.compliance.ContentGate
import com.stproject.client.android.domain.model.CharacterDetail
import com.stproject.client.android.domain.model.CharacterFollowResult
import com.stproject.client.android.domain.model.CharacterSummary
import com.stproject.client.android.domain.model.Persona
import com.stproject.client.android.domain.model.ShareCodeInfo
import com.stproject.client.android.domain.repository.CharacterRepository
import com.stproject.client.android.domain.repository.PersonaRepository
import com.stproject.client.android.domain.usecase.ResolveContentAccessUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonasViewModelTest : BaseUnitTest() {
    private class FakeAccessManager(gate: ContentGate) : ContentAccessManager {
        private val state = MutableStateFlow(gate)
        override val gate: StateFlow<ContentGate> = state

        override fun updateGate(gate: ContentGate) {
            state.value = gate
        }

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return state.value.decideAccess(isNsfw)
        }
    }

    private class FakeCharacterRepository : CharacterRepository {
        override suspend fun queryCharacters(
            cursor: String?,
            limit: Int?,
            sortBy: String?,
            isNsfw: Boolean?,
        ): List<CharacterSummary> = emptyList()

        override suspend fun getCharacterDetail(characterId: String): CharacterDetail {
            return CharacterDetail(
                id = characterId,
                name = "Test",
                description = "",
                tags = emptyList(),
                creatorName = null,
                isNsfw = false,
                totalFollowers = 0,
                isFollowed = false,
            )
        }

        override suspend fun resolveShareCode(shareCode: String): String? = null

        override suspend fun generateShareCode(characterId: String): ShareCodeInfo? = null

        override suspend fun blockCharacter(
            characterId: String,
            value: Boolean,
        ) = Unit

        override suspend fun followCharacter(
            characterId: String,
            value: Boolean,
        ): CharacterFollowResult {
            return CharacterFollowResult(totalFollowers = 0, isFollowed = false)
        }
    }

    private class FakePersonaRepository(
        private val items: List<Persona>,
    ) : PersonaRepository {
        override suspend fun listPersonas(): List<Persona> = items

        override suspend fun createPersona(
            name: String,
            description: String?,
            avatarUrl: String?,
            isDefault: Boolean,
        ): Persona {
            return Persona(
                id = "new",
                userId = "user-1",
                name = name,
                description = description ?: "",
                avatarUrl = avatarUrl,
                isDefault = isDefault,
                createdAt = "now",
                updatedAt = "now",
            )
        }

        override suspend fun updatePersona(
            personaId: String,
            name: String,
            description: String?,
            avatarUrl: String?,
            isDefault: Boolean,
        ): Persona {
            return Persona(
                id = personaId,
                userId = "user-1",
                name = name,
                description = description ?: "",
                avatarUrl = avatarUrl,
                isDefault = isDefault,
                createdAt = "now",
                updatedAt = "now",
            )
        }

        override suspend fun deletePersona(personaId: String) = Unit
    }

    private fun buildPersona(
        id: String,
        isDefault: Boolean,
    ): Persona {
        return Persona(
            id = id,
            userId = "user-1",
            name = "Persona $id",
            description = "desc",
            avatarUrl = null,
            isDefault = isDefault,
            createdAt = "now",
            updatedAt = "now",
        )
    }

    @Test
    fun `load blocked sets error and clears items`() =
        runTest(mainDispatcherRule.dispatcher) {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = false,
                    allowNsfwPreference = false,
                )
            val resolveContentAccess =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = FakeCharacterRepository(),
                )
            val repo = FakePersonaRepository(listOf(buildPersona("1", isDefault = true)))
            val viewModel = PersonasViewModel(repo, resolveContentAccess)

            viewModel.load()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("age verification required", state.error)
            assertEquals(emptyList<Persona>(), state.items)
        }

    @Test
    fun `load allowed sorts default first`() =
        runTest(mainDispatcherRule.dispatcher) {
            val gate =
                ContentGate(
                    consentLoaded = true,
                    consentRequired = false,
                    ageVerified = true,
                    allowNsfwPreference = false,
                )
            val resolveContentAccess =
                ResolveContentAccessUseCase(
                    accessManager = FakeAccessManager(gate),
                    characterRepository = FakeCharacterRepository(),
                )
            val personaA = buildPersona("1", isDefault = false)
            val personaB = buildPersona("2", isDefault = true)
            val repo = FakePersonaRepository(listOf(personaA, personaB))
            val viewModel = PersonasViewModel(repo, resolveContentAccess)

            viewModel.load()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(personaB, personaA), state.items)
        }
}
