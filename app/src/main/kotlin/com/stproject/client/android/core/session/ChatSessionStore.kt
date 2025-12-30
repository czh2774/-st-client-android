package com.stproject.client.android.core.session

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface ChatSessionStore {
    fun getSessionId(): String?
    fun setSessionId(sessionId: String?)
    fun getClientSessionId(): String?
    fun setClientSessionId(clientSessionId: String?)
    fun getSessionUpdatedAtMs(): Long?
    fun setSessionUpdatedAtMs(updatedAtMs: Long?)
    fun clear()
}

@Singleton
class SharedPreferencesChatSessionStore @Inject constructor(
    @Named("chat_session_prefs") private val prefs: SharedPreferences
) : ChatSessionStore {
    override fun getSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)

    override fun setSessionId(sessionId: String?) {
        val clean = sessionId?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit().apply {
            if (clean == null) {
                remove(KEY_SESSION_ID)
            } else {
                putString(KEY_SESSION_ID, clean)
            }
        }.apply()
    }

    override fun getClientSessionId(): String? = prefs.getString(KEY_CLIENT_SESSION_ID, null)

    override fun setClientSessionId(clientSessionId: String?) {
        val clean = clientSessionId?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit().apply {
            if (clean == null) {
                remove(KEY_CLIENT_SESSION_ID)
            } else {
                putString(KEY_CLIENT_SESSION_ID, clean)
            }
        }.apply()
    }

    override fun getSessionUpdatedAtMs(): Long? {
        val raw = prefs.getLong(KEY_SESSION_UPDATED_AT, -1L)
        return raw.takeIf { it > 0 }
    }

    override fun setSessionUpdatedAtMs(updatedAtMs: Long?) {
        prefs.edit().apply {
            if (updatedAtMs == null || updatedAtMs <= 0) {
                remove(KEY_SESSION_UPDATED_AT)
            } else {
                putLong(KEY_SESSION_UPDATED_AT, updatedAtMs)
            }
        }.apply()
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_CLIENT_SESSION_ID)
            .remove(KEY_SESSION_UPDATED_AT)
            .apply()
    }

    private companion object {
        private const val KEY_SESSION_ID = "chat.session_id"
        private const val KEY_CLIENT_SESSION_ID = "chat.client_session_id"
        private const val KEY_SESSION_UPDATED_AT = "chat.session_updated_at_ms"
    }
}
