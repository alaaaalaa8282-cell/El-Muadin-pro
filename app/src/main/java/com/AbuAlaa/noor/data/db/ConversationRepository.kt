package com.AbuAlaa.noor.data.db

import android.content.Context
import com.AbuAlaa.noor.data.Conversation
import com.AbuAlaa.noor.data.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ConversationRepository(context: Context) {
    
    private val database = NoorDatabase.getInstance(context)
    private val conversationDao = database.conversationDao()
    private val converters = Converters() // Reuse single instance
    
    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { it.toConversation() }
        }
    }
    
    suspend fun getAllConversationsSync(): List<Conversation> = withContext(Dispatchers.IO) {
        conversationDao.getAllConversationsSync().map { it.toConversation() }
    }
    
    suspend fun getConversationById(id: String): Conversation? = withContext(Dispatchers.IO) {
        conversationDao.getConversationById(id)?.toConversation()
    }
    
    suspend fun saveConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        val entity = conversation.toEntity()
        conversationDao.insertConversation(entity)
        
        // Auto-delete old conversations beyond 40
        val count = conversationDao.getConversationCount()
        if (count > 40) {
            conversationDao.deleteOldConversations()
        }
    }
    
    suspend fun updateConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        val entity = conversation.toEntity()
        conversationDao.updateConversation(entity)
    }
    
    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        conversationDao.deleteConversation(id)
    }
    
    // Generate title from first user message
    fun generateTitle(firstMessage: String): String {
        val cleaned = firstMessage.trim()
        return when {
            cleaned.isEmpty() -> "محادثة جديدة"
            cleaned.length <= 30 -> cleaned
            else -> {
                // Smart truncation at word boundary
                val truncated = cleaned.take(30)
                val lastSpace = truncated.lastIndexOf(' ')
                if (lastSpace > 20) {
                    truncated.substring(0, lastSpace) + "..."
                } else {
                    truncated + "..."
                }
            }
        }
    }
    
    private fun Conversation.toEntity() = ConversationEntity(
        id = id,
        title = title,
        messagesJson = converters.toMessagesJson(messages),
        timestamp = timestamp,
        model = model
    )
    
    private fun ConversationEntity.toConversation() = Conversation(
        id = id,
        title = title,
        messages = converters.fromMessagesJson(messagesJson),
        timestamp = timestamp,
        model = model
    )
}
