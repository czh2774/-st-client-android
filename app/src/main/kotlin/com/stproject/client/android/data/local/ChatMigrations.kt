package com.stproject.client.android.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ChatMigrations {
    val MIGRATION_1_4 =
        object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateChatSchema(db)
            }
        }

    val MIGRATION_2_4 =
        object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateChatSchema(db)
            }
        }

    val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateChatSchema(db)
            }
        }

    private fun migrateChatSchema(db: SupportSQLiteDatabase) {
        ensureColumn(db, "chat_messages", "serverId", "TEXT")
        ensureColumn(db, "chat_messages", "isStreaming", "INTEGER", "0", true)
        ensureColumn(db, "chat_messages", "swipeId", "INTEGER")
        ensureColumn(db, "chat_messages", "swipesJson", "TEXT")
        ensureColumn(db, "chat_messages", "metadataJson", "TEXT")

        ensureColumn(db, "chat_sessions", "primaryMemberId", "TEXT")
        ensureColumn(db, "chat_sessions", "displayName", "TEXT", "''", true)
        ensureColumn(db, "chat_sessions", "updatedAt", "TEXT")
        ensureColumn(db, "chat_sessions", "updatedAtMs", "INTEGER", "0", true)

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId_createdAt " +
                "ON chat_messages(sessionId, createdAt)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_chat_sessions_updatedAtMs " +
                "ON chat_sessions(updatedAtMs)",
        )
    }

    private fun ensureColumn(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
        type: String,
        defaultValue: String? = null,
        notNull: Boolean = false,
    ) {
        if (hasColumn(db, table, column)) return
        val builder = StringBuilder()
        builder.append("ALTER TABLE ").append(table).append(" ADD COLUMN ")
            .append(column).append(" ").append(type)
        if (notNull) {
            builder.append(" NOT NULL")
        }
        if (defaultValue != null) {
            builder.append(" DEFAULT ").append(defaultValue)
        }
        db.execSQL(builder.toString())
    }

    private fun hasColumn(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
    ): Boolean {
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                if (name == column) return true
            }
        }
        return false
    }
}
