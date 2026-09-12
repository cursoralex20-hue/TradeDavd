package com.zhousl.aether.data.journal

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

object AndroidTradeJournalDatabaseFactory {
    @Volatile
    private var instance: TradeJournalDatabase? = null

    fun getInstance(context: Context): TradeJournalDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            TradeJournalDatabase::class.java,
            "tradedavd_journal.db",
        ).setDriver(BundledSQLiteDriver())
            .build()
            .also { instance = it }
    }
}