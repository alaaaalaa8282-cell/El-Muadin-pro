package com.AbuAlaa.noor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "conversations")
@TypeConverters(Converters::class)
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val messagesJson: String, // JSON serialized list of messages
    val timestamp: Long,
    val model: String
)
