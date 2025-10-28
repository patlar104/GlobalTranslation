package com.example.globaltranslation.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for conversation history.
 * 
 * This DAO provides type-safe database operations for the conversations table,
 * following Room best practices and modern Kotlin coroutines patterns.
 * 
 * ## Architecture:
 * - Uses Room's coroutine support for async operations
 * - Flow-based queries for reactive UI updates
 * - Optimized SQL queries with proper indexing
 * - OnConflictStrategy.REPLACE for idempotent insert operations
 * 
 * ## Conflict Strategy Design:
 * We use OnConflictStrategy.REPLACE instead of other strategies because:
 * - **REPLACE**: Allows idempotent operations (safe to retry same insert)
 * - **ABORT** (default): Would throw exception on conflict (not desired)
 * - **IGNORE**: Would silently fail updates (data could become stale)
 * - **FAIL**: Would crash the app (unacceptable UX)
 * 
 * REPLACE is chosen because conversation timestamps are unique, and if a
 * duplicate insert occurs (e.g., from retry logic), we want the latest data
 * to overwrite the old data rather than fail or be ignored.
 * 
 * ## Performance Considerations:
 * - All queries are optimized by Room compiler
 * - Indexes on timestamp (primary key) ensure O(1) lookup
 * - Flow emissions only trigger on actual data changes
 * - Suspend functions ensure non-blocking operations
 * 
 * ## Example Usage:
 * ```kotlin
 * // In Repository (correct usage pattern)
 * @Singleton
 * class RoomConversationRepository @Inject constructor(
 *     private val dao: ConversationDao  // DAO injected into repository
 * ) : ConversationRepository {
 *     
 *     override fun getConversations(): Flow<List<ConversationTurn>> = 
 *         dao.getAllConversations().map { it.map { entity -> entity.toDomainModel() } }
 * }
 * ```
 * 
 * @see ConversationEntity for entity definition
 * @see RoomConversationRepository for repository implementation
 */
@Dao
interface ConversationDao {
    
    /**
     * Gets all conversations ordered by timestamp (newest first).
     * 
     * Returns a Flow that automatically emits new values whenever the
     * conversations table changes. Perfect for reactive UI updates.
     * 
     * ## Query Details:
     * - Table: conversations
     * - Ordering: DESC by timestamp (newest first)
     * - Indexing: Utilizes primary key index on timestamp
     * - Performance: O(n) initial query, O(k) for updates where k is changed rows
     * 
     * ## Flow Behavior:
     * - Emits immediately with current data
     * - Automatically emits on INSERT, UPDATE, DELETE operations
     * - Never completes (unless database closed)
     * - Safe to collect from UI thread with proper StateFlow conversion
     * 
     * @return Flow emitting list of all conversations, newest first
     * 
     * ## Example:
     * ```kotlin
     * dao.getAllConversations()
     *     .map { entities -> entities.map { it.toDomainModel() } }
     *     .collect { conversations ->
     *         // UI updates automatically
     *     }
     * ```
     */
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    /**
     * Inserts a new conversation turn with REPLACE conflict strategy.
     * 
     * Uses REPLACE conflict strategy: if a conversation with the same timestamp
     * already exists, it will be replaced. This makes the operation idempotent,
     * which is important for retry logic and data consistency.
     * 
     * ## Why REPLACE?
     * - Timestamps are unique (primary key) - unlikely to have real conflicts
     * - If conflict occurs (e.g., from retry), we want latest data to win
     * - Alternative strategies like ABORT would throw exceptions (bad UX)
     * - Alternative IGNORE would silently fail updates (stale data risk)
     * 
     * ## Performance:
     * - Single insert: ~1-5ms on modern devices
     * - Transaction overhead: Minimal with suspend functions
     * - Triggers Flow emission for reactive updates
     * 
     * ## Thread Safety:
     * - Suspend function ensures non-blocking
     * - Room handles threading automatically
     * - Safe to call from any coroutine context
     * 
     * @param conversation The conversation entity to insert or replace
     * 
     * ## Example:
     * ```kotlin
     * viewModelScope.launch {
     *     val entity = ConversationEntity(
     *         timestamp = System.currentTimeMillis(),
     *         originalText = "Hello",
     *         translatedText = "Hola",
     *         sourceLang = "en",
     *         targetLang = "es"
     *     )
     *     dao.insertConversation(entity)
     * }
     * ```
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    /**
     * Deletes a conversation by timestamp.
     * 
     * Timestamp is the primary key, ensuring unique identification.
     * If no conversation with the given timestamp exists, this is a no-op.
     * 
     * ## Query Details:
     * - Uses primary key for O(1) deletion
     * - Triggers Flow emission for reactive updates
     * - Transactional (atomic operation)
     * 
     * ## Performance:
     * - Single delete: ~1-2ms
     * - No error if timestamp doesn't exist
     * 
     * @param timestamp Unix epoch milliseconds timestamp of conversation to delete
     * 
     * ## Example:
     * ```kotlin
     * viewModelScope.launch {
     *     dao.deleteConversation(conversation.timestamp)
     *     // Flow subscribers automatically notified
     * }
     * ```
     */
    @Query("DELETE FROM conversations WHERE timestamp = :timestamp")
    suspend fun deleteConversation(timestamp: Long)
    
    /**
     * Clears all conversation history.
     * 
     * Destructive operation that deletes all rows from the conversations table.
     * Cannot be undone. Recommend user confirmation before calling.
     * 
     * ## Query Details:
     * - Deletes all rows in conversations table
     * - Triggers Flow emission with empty list
     * - Transactional operation
     * - Database file size may not decrease immediately (VACUUM needed)
     * 
     * ## Performance:
     * - Time: O(n) where n is number of conversations
     * - Typically 10-100ms for thousands of rows
     * 
     * ## Example:
     * ```kotlin
     * // With confirmation
     * AlertDialog(
     *     onConfirm = {
     *         viewModelScope.launch {
     *             dao.clearAll()
     *         }
     *     }
     * )
     * ```
     */
    @Query("DELETE FROM conversations")
    suspend fun clearAll()
    
    /**
     * Gets conversations for a specific language pair.
     * 
     * Useful for filtering conversation history by languages used.
     * Returns a Flow that updates automatically when matching conversations change.
     * 
     * ## Query Details:
     * - Filters by exact sourceLang AND targetLang match
     * - Ordering: DESC by timestamp (newest first)
     * - Indexing: Benefits from composite index if defined
     * - Performance: O(k) where k is matching conversations
     * 
     * ## Use Cases:
     * - Show history for specific language pair
     * - Language-specific statistics
     * - Export conversations for specific languages
     * 
     * @param sourceLang Source language code (e.g., "en")
     * @param targetLang Target language code (e.g., "es")
     * @return Flow emitting matching conversations, newest first
     * 
     * ## Example:
     * ```kotlin
     * // Show only English to Spanish conversations
     * dao.getConversationsForLanguagePair("en", "es")
     *     .collect { conversations ->
     *         updateLanguagePairHistory(conversations)
     *     }
     * ```
     */
    @Query("SELECT * FROM conversations WHERE sourceLang = :sourceLang AND targetLang = :targetLang ORDER BY timestamp DESC")
    fun getConversationsForLanguagePair(sourceLang: String, targetLang: String): Flow<List<ConversationEntity>>
}

