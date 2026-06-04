package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String, // e.g. UUID
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
