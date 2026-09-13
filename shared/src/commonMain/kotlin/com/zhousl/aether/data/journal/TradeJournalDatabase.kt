package com.zhousl.aether.data.journal

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        TradeJournalEntryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(TradeJournalDatabaseConstructor::class)
abstract class TradeJournalDatabase : RoomDatabase() {
    abstract fun tradeJournalDao(): TradeJournalDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TradeJournalDatabaseConstructor : RoomDatabaseConstructor<TradeJournalDatabase> {
    override fun initialize(): TradeJournalDatabase
}