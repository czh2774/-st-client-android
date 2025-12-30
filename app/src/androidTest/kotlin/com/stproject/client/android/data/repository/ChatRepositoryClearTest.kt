package com.stproject.client.android.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stproject.client.android.core.session.SharedPreferencesChatSessionStore
import com.stproject.client.android.data.local.ChatDatabase
import com.stproject.client.android.data.local.ChatMessageEntity
import com.stproject.client.android.domain.model.ChatRole
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRepositoryClearTest {
    private var db: ChatDatabase? = null

    @After
    fun tearDown() {
        db?.close()
    }

    @Test
    fun clearLocalSessionClearsStoreAndMessages() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, ChatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = requireNotNull(db).chatMessageDao()
        val prefs = context.getSharedPreferences("test_chat_session_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val store = SharedPreferencesChatSessionStore(prefs)
        store.setSessionId("s1")
        store.setClientSessionId("client-1")
        store.setSessionUpdatedAtMs(1234L)

        dao.upsert(
            ChatMessageEntity(
                id = "local-1",
                sessionId = "s1",
                serverId = null,
                role = ChatRole.User.name,
                content = "hello",
                createdAt = 1,
                isStreaming = false
            )
        )

        val repo = HttpChatRepository(
            api = mockk(relaxed = true),
            apiClient = mockk(relaxed = true),
            okHttpClient = OkHttpClient(),
            baseUrlProvider = mockk(relaxed = true),
            messageDao = dao,
            sessionStore = store
        )

        repo.clearLocalSession()

        assertNull(store.getSessionId())
        assertNull(store.getClientSessionId())
        assertNull(store.getSessionUpdatedAtMs())
        val remaining = dao.observeMessages("s1").first()
        assertTrue(remaining.isEmpty())
    }
}
