package com.zhousl.aether.data.journal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface TradeJournalReadStore {
    fun observeAll(): Flow<List<TradeJournalEntryEntity>>
    suspend fun all(): List<TradeJournalEntryEntity>
}

class RoomTradeJournalReadStore(
    private val database: TradeJournalDatabase,
) : TradeJournalReadStore {
    override fun observeAll(): Flow<List<TradeJournalEntryEntity>> =
        database.tradeJournalDao().observeAll()

    override suspend fun all(): List<TradeJournalEntryEntity> =
        database.tradeJournalDao().observeAll().first()
}