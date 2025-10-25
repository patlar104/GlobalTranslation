package com.example.globaltranslation.data.repository

import com.example.globaltranslation.core.model.ConversationTurn
import com.example.globaltranslation.core.repository.ConversationRepository
import com.example.globaltranslation.data.local.ConversationDao
import com.example.globaltranslation.data.local.toDomainModel
import com.example.globaltranslation.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-based implementation of ConversationRepository.
 * Persists conversation history to local database.
 * 
 * Battery optimizations:
 * - Efficient Flow-based data access (no polling)
 * - Direct DAO delegation to minimize object allocations
 * - Optimized query patterns for better database performance
 */
@Singleton
class RoomConversationRepository @Inject constructor(
    private val dao: ConversationDao
) : ConversationRepository {
    
    override fun getConversations(): Flow<List<ConversationTurn>> = 
        dao.getAllConversations().map { entities ->
            entities.map { it.toDomainModel() }
        }
    
    override suspend fun saveConversation(turn: ConversationTurn) {
        dao.insertConversation(turn.toEntity())
    }
    
    override suspend fun deleteConversation(timestamp: Long) {
        dao.deleteConversation(timestamp)
    }
    
    override suspend fun clearAll() {
        dao.clearAll()
    }
}

