package com.stproject.client.android.core.compliance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface ContentAccessManager {
    val gate: StateFlow<ContentGate>

    fun updateGate(gate: ContentGate)

    fun decideAccess(isNsfw: Boolean?): ContentAccessDecision
}

@Singleton
class InMemoryContentAccessManager
    @Inject
    constructor() : ContentAccessManager {
        private val _gate =
            MutableStateFlow(
                ContentGate(
                    consentLoaded = false,
                    consentRequired = true,
                    ageVerified = false,
                    allowNsfwPreference = false,
                ),
            )

        override val gate: StateFlow<ContentGate> = _gate

        override fun updateGate(gate: ContentGate) {
            _gate.value = gate
        }

        override fun decideAccess(isNsfw: Boolean?): ContentAccessDecision {
            return _gate.value.decideAccess(isNsfw)
        }
    }
