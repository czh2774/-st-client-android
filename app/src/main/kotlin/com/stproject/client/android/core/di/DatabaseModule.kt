package com.stproject.client.android.core.di

import android.content.Context
import androidx.room.Room
import com.stproject.client.android.data.local.ChatDatabase
import com.stproject.client.android.data.local.ChatMessageDao
import com.stproject.client.android.data.local.ChatMigrations
import com.stproject.client.android.data.local.ChatSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideChatDatabase(
        @ApplicationContext context: Context,
    ): ChatDatabase {
        return Room.databaseBuilder(context, ChatDatabase::class.java, "st_chat.db")
            .addMigrations(
                ChatMigrations.MIGRATION_1_4,
                ChatMigrations.MIGRATION_2_4,
                ChatMigrations.MIGRATION_3_4,
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideChatMessageDao(db: ChatDatabase): ChatMessageDao = db.chatMessageDao()

    @Provides
    fun provideChatSessionDao(db: ChatDatabase): ChatSessionDao = db.chatSessionDao()
}
