package com.AbuAlaa.noor.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT 40")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT 40")
    suspend fun getAllConversationsSync(): List<ConversationEntity>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
    
    @Query("DELETE FROM conversations WHERE id NOT IN (SELECT id FROM conversations ORDER BY timestamp DESC LIMIT 40)")
    suspend fun deleteOldConversations()
    
    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int
}
