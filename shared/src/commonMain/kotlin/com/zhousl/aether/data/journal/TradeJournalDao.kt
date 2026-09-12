package com.zhousl.aether.data.journal

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeJournalDao {
    @Query("SELECT * FROM trade_journal_entries ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<TradeJournalEntryEntity>>

    @Query("SELECT * FROM trade_journal_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TradeJournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TradeJournalEntryEntity)

    @Update
    suspend fun update(entry: TradeJournalEntryEntity)

    @Delete
    suspend fun delete(entry: TradeJournalEntryEntity)

    @Query("DELETE FROM trade_journal_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM trade_journal_entries")
    fun observeCount(): Flow<Int>
}