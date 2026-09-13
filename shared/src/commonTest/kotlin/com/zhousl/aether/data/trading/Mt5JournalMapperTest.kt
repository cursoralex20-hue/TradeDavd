package com.zhousl.aether.data.trading

import com.zhousl.aether.data.journal.TradeJournalEntryEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mt5JournalMapperTest {
    @Test
    fun mapsClosingDealsToJournalEntries() {
        val deals = listOf(
            MetaApiDeal(
                id = "d1",
                type = "DEAL_TYPE_SELL",
                symbol = "XAUUSD",
                volume = 0.1,
                price = 2350.0,
                profit = 45.5,
                commission = -2.0,
                swap = -0.5,
                time = 1_700_000_000_000,
                magic = 7,
                entry = "DEAL_ENTRY_IN",
                positionId = "pos1",
            ),
            MetaApiDeal(
                id = "d2",
                type = "DEAL_TYPE_SELL",
                symbol = "XAUUSD",
                volume = 0.1,
                price = 2340.0,
                profit = 12.0,
                commission = 0.0,
                swap = 0.0,
                time = 1_700_000_050_000,
                magic = 7,
                entry = "DEAL_ENTRY_OUT",
                positionId = "pos1",
            ),
        )

        val entries = Mt5JournalMapper.mapHistoryDeals(deals)

        // Only the OUT deal creates a journal entry.
        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("mt5-d2", entry.id)
        assertEquals("XAUUSD", entry.asset)
        assertEquals("SELL", entry.side)
        assertEquals(12.0, entry.resultAmount)
        assertEquals("win", entry.grade)
        assertEquals("mt5", entry.source)
        assertEquals(7L, entry.magic)
    }

    @Test
    fun gradesLossAndBreakevenCorrectly() {
        val loss = Mt5JournalMapper.toJournalEntry(
            MetaApiDeal(
                id = "a",
                type = "DEAL_TYPE_BUY",
                symbol = "BTCUSD",
                volume = 1.0,
                price = 60000.0,
                profit = -100.0,
                time = 1L,
            ),
            nowMillis = 2L,
        )
        assertEquals("loss", loss.grade)

        val be = Mt5JournalMapper.toJournalEntry(
            MetaApiDeal(
                id = "b",
                type = "DEAL_TYPE_BUY",
                symbol = "BTCUSD",
                volume = 1.0,
                price = 60000.0,
                profit = 0.0,
                time = 1L,
            ),
            nowMillis = 2L,
        )
        assertEquals("breakeven", be.grade)
    }

    @Test
    fun analyticsComputesWinRateNetAndExpectancy() {
        val entries = listOf(
            TradeJournalEntryEntity(
                id = "1", dateMillis = 1L, asset = "XAUUSD", side = "BUY",
                grade = "win", setup = "", session = "", marketRegime = "",
                riskAmount = 0.0, plannedPips = 0.0, actualPips = 0.0,
                resultAmount = 100.0, notes = "", planFollowed = false,
                createdAtMillis = 1L, updatedAtMillis = 1L, source = "mt5",
            ),
            TradeJournalEntryEntity(
                id = "2", dateMillis = 2L, asset = "XAUUSD", side = "SELL",
                grade = "loss", setup = "", session = "", marketRegime = "",
                riskAmount = 0.0, plannedPips = 0.0, actualPips = 0.0,
                resultAmount = -50.0, notes = "", planFollowed = false,
                createdAtMillis = 2L, updatedAtMillis = 2L, source = "mt5",
            ),
        )

        val analytics = Mt5AnalyticsCalculator.compute(entries)

        assertEquals(2, analytics.totalTrades)
        assertEquals(1, analytics.wins)
        assertEquals(1, analytics.losses)
        assertEquals(0.5, analytics.winRate)
        assertEquals(50.0, analytics.netProfit)
        assertEquals(2.0, analytics.profitFactor)
        assertEquals(25.0, analytics.expectancy)
    }

    @Test
    fun ignoresManualOrOpenEntriesInAnalytics() {
        val manual = TradeJournalEntryEntity(
            id = "m", dateMillis = 1L, asset = "XAUUSD", side = "BUY",
            grade = "win", setup = "", session = "", marketRegime = "",
            riskAmount = 0.0, plannedPips = 0.0, actualPips = 0.0,
            resultAmount = 999.0, notes = "", planFollowed = false,
            createdAtMillis = 1L, updatedAtMillis = 1L, source = "",
        )
        val open = TradeJournalEntryEntity(
            id = "o", dateMillis = 2L, asset = "BTCUSD", side = "BUY",
            grade = "open", setup = "", session = "", marketRegime = "",
            riskAmount = 0.0, plannedPips = 0.0, actualPips = 0.0,
            resultAmount = 50.0, notes = "", planFollowed = false,
            createdAtMillis = 2L, updatedAtMillis = 2L, source = "mt5",
        )

        val analytics = Mt5AnalyticsCalculator.compute(listOf(manual, open))

        assertEquals(0, analytics.totalTrades)
        assertTrue(analytics.netProfit == 0.0)
    }

    @Test
    fun buildTradeStateJsonContainsSections() {
        val config = Mt5SyncConfig(
            brokerServer = "broker.example",
            login = "12345",
            accountId = "abc123",
        )
        val account = MetaApiAccountInformation(balance = 10_000.0, equity = 10_050.0)
        val position = MetaApiPosition(
            id = "p1", type = "POSITION_TYPE_BUY", symbol = "XAUUSD",
            volume = 0.5, openPrice = 2300.0, currentPrice = 2320.0,
            profit = 100.0, time = 1L,
        )
        val entries = listOf(
            TradeJournalEntryEntity(
                id = "mt5-d2", dateMillis = 1L, asset = "XAUUSD", side = "SELL",
                grade = "win", setup = "", session = "", marketRegime = "",
                riskAmount = 0.0, plannedPips = 0.0, actualPips = 0.0,
                resultAmount = 12.0, notes = "", planFollowed = false,
                createdAtMillis = 1L, updatedAtMillis = 1L, source = "mt5",
            ),
        )
        val analytics = Mt5AnalyticsCalculator.compute(entries)

        val json = Mt5JournalMapper.buildTradeStateJson(
            config = config,
            account = account,
            openPositions = listOf(position),
            entries = entries,
            analytics = analytics,
            generatedAtMillis = 5L,
        )

        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.contains("\"generatedAtMillis\":5"), "generatedAt")
        assertTrue(json.contains("\"balance\":10000.0"), "account balance")
        assertTrue(json.contains("\"equity\":10050.0"), "account equity")
        assertTrue(json.contains("\"openPositions\""), "positions section")
        assertTrue(json.contains("\"journalEntries\""), "journal section")
        assertTrue(json.contains("\"symbol\":\"XAUUSD\""), "symbol present")
        assertTrue(json.contains("\"profitFactor\":null"), "infinite profit factor serialized as null")
    }

    @Test
    fun openPositionMapsWithExpectedGrade() {
        val position = MetaApiPosition(
            id = "p1", type = "POSITION_TYPE_SELL", symbol = "BTCUSD",
            volume = 0.01, openPrice = 60000.0, currentPrice = 59000.0,
            profit = 25.0, time = 1L,
        )
        val entry = Mt5JournalMapper.toOpenPositionEntry(position, nowMillis = 2L)

        assertEquals("mt5-pos-p1", entry.id)
        assertEquals("SELL", entry.side)
        assertEquals("open", entry.grade)
        assertEquals("BTCUSD", entry.asset)
        assertEquals(60000.0, entry.openPrice)
        assertEquals(59000.0, entry.closePrice)
        assertEquals(0.01, entry.volume)
    }
}