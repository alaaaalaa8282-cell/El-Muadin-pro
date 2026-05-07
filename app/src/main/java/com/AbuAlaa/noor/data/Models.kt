package com.AbuAlaa.noor.data

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val files: List<MessageFile> = emptyList(),
    val model: String = "",
    val alternatives: List<String> = emptyList(),
    val currentAlternativeIndex: Int = 0
) {
    fun getDisplayContent(): String {
        return if (alternatives.isNotEmpty() && currentAlternativeIndex < alternatives.size) {
            alternatives[currentAlternativeIndex]
        } else {
            content
        }
    }
    
    fun hasAlternatives(): Boolean = alternatives.size > 1
    
    fun getAlternativesCount(): Int = maxOf(1, alternatives.size)
}

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<Message>,
    val timestamp: Long = System.currentTimeMillis(),
    val model: String = "omni"
)

data class User(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null
)

data class Model(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String? = null
)
