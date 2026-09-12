package com.zhousl.aether.data.journal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_journal_entries")
data class TradeJournalEntryEntity(
    @PrimaryKey
    val id: String,
    val dateMillis: Long,
    val asset: String,
    val side: String,
    val grade: String,
    val setup: String,
    val session: String,
    val marketRegime: String,
    val riskAmount: Double,
    val plannedPips: Double,
    val actualPips: Double,
    val resultAmount: Double,
    val notes: String,
    val planFollowed: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)