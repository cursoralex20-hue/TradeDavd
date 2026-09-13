package com.zhousl.aether.data.trading

import com.zhousl.aether.data.journal.TradeJournalEntryEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

const val TradeJournalSourceManual = "manual"
const val TradeJournalSourceMt5 = "mt5"

const val DealEntryIn = "DEAL_ENTRY_IN"
const val DealEntryOut = "DEAL_ENTRY_OUT"
const val DealEntryOutBy = "DEAL_ENTRY_OUT_BY"

data class Mt5Analytics(
    val totalTrades: Int,
    val wins: Int,
    val losses: Int,
    val breakevens: Int,
    val winRate: Double,
    val netProfit: Double,
    val profitFactor: Double,
    val averageWin: Double,
    val averageLoss: Double,
    val expectancy: Double,
    val maxDrawdown: Double,
    val bestTrade: Double,
    val worstTrade: Double,
) {
    companion object {
        val Empty = Mt5Analytics(
            totalTrades = 0,
            wins = 0,
            losses = 0,
            breakevens = 0,
            winRate = 0.0,
            netProfit = 0.0,
            profitFactor = 0.0,
            averageWin = 0.0,
            averageLoss = 0.0,
            expectancy = 0.0,
            maxDrawdown = 0.0,
            bestTrade = 0.0,
            worstTrade = 0.0,
        )
    }
}

object Mt5JournalMapper {
    private val Json = Json { ignoreUnknownKeys = true }

    fun dealProfit(deal: MetaApiDeal): Double = deal.profit + deal.swap + deal.commission

    fun isClosingDeal(deal: MetaApiDeal): Boolean =
        deal.entry == DealEntryOut || deal.entry == DealEntryOutBy || deal.entry.isBlank()

    fun toJournalEntry(
        deal: MetaApiDeal,
        nowMillis: Long = System.currentTimeMillis(),
    ): TradeJournalEntryEntity {
        val profit = dealProfit(deal)
        val grade = when {
            profit > 0.0001 -> "win"
            profit < -0.0001 -> "loss"
            else -> "breakeven"
        }
        return TradeJournalEntryEntity(
            id = "mt5-${deal.id}",
            dateMillis = deal.time,
            asset = deal.symbol,
            side = if (deal.type.contains("BUY")) "BUY" else "SELL",
            grade = grade,
            setup = "",
            session = "",
            marketRegime = "",
            riskAmount = 0.0,
            plannedPips = 0.0,
            actualPips = 0.0,
            resultAmount = profit,
            notes = deal.comment,
            planFollowed = false,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis,
            source = TradeJournalSourceMt5,
            ticket = deal.id,
            magic = deal.magic,
            positionId = deal.positionId,
            volume = deal.volume,
            openPrice = 0.0,
            closePrice = deal.price,
            commission = deal.commission,
            swap = deal.swap,
        )
    }

    fun mapHistoryDeals(deals: List<MetaApiDeal>): List<TradeJournalEntryEntity> =
        deals
            .filter(::isClosingDeal)
            .distinctBy { it.id }
            .map { toJournalEntry(it) }

    fun toOpenPositionEntry(
        position: MetaApiPosition,
        nowMillis: Long = System.currentTimeMillis(),
    ): TradeJournalEntryEntity = TradeJournalEntryEntity(
        id = "mt5-pos-${position.id}",
        dateMillis = position.time,
        asset = position.symbol,
        side = if (position.type.contains("BUY")) "BUY" else "SELL",
        grade = "open",
        setup = "",
        session = "",
        marketRegime = "",
        riskAmount = 0.0,
        plannedPips = 0.0,
        actualPips = 0.0,
        resultAmount = position.profit,
        notes = position.comment,
        planFollowed = false,
        createdAtMillis = nowMillis,
        updatedAtMillis = nowMillis,
        source = TradeJournalSourceMt5,
        ticket = position.id,
        magic = position.magic,
        positionId = position.id,
        volume = position.volume,
        openPrice = position.openPrice,
        closePrice = position.currentPrice,
        commission = 0.0,
        swap = position.swap,
    )

    fun buildTradeStateJson(
        config: Mt5SyncConfig,
        account: MetaApiAccountInformation?,
        openPositions: List<MetaApiPosition>,
        entries: List<TradeJournalEntryEntity>,
        analytics: Mt5Analytics,
        generatedAtMillis: Long = System.currentTimeMillis(),
    ): String {
        val root = buildJsonObject {
            put("schemaVersion", 1)
            put("generatedAtMillis", generatedAtMillis)
            put("config", buildJsonObject {
                put("brokerServer", config.brokerServer)
                put("login", config.login)
                put("accountId", config.accountId.trim().takeLast(4))
            })
            put("account", if (account != null) buildJsonObject {
                put("balance", account.balance)
                put("equity", account.equity)
                put("margin", account.margin)
                put("marginFree", account.marginFree)
                put("profit", account.profit)
                put("currency", account.currency)
                put("leverage", account.leverage)
            } else JsonObject(emptyMap()))
            put("openPositions", positionsJson(openPositions))
            put("journalEntries", entriesJson(entries))
            put("analytics", analyticsJson(analytics))
        }
        return Json.encodeToString(JsonObject.serializer(), root)
    }

    private fun positionsJson(positions: List<MetaApiPosition>): JsonArray = buildJsonArray {
        positions.forEach { p ->
            add(buildJsonObject {
                put("id", p.id)
                put("type", p.type)
                put("symbol", p.symbol)
                put("volume", p.volume)
                put("openPrice", p.openPrice)
                put("currentPrice", p.currentPrice)
                put("profit", p.profit)
                put("timeMillis", p.time)
            })
        }
    }

    private fun entriesJson(entries: List<TradeJournalEntryEntity>): JsonArray = buildJsonArray {
        entries.forEach { e ->
            add(buildJsonObject {
                put("id", e.id)
                put("dateMillis", e.dateMillis)
                put("asset", e.asset)
                put("side", e.side)
                put("grade", e.grade)
                put("resultAmount", e.resultAmount)
                put("ticket", e.ticket)
                put("magic", e.magic)
                put("positionId", e.positionId)
                put("volume", e.volume)
                put("openPrice", e.openPrice)
                put("closePrice", e.closePrice)
            })
        }
    }

    private fun analyticsJson(analytics: Mt5Analytics): JsonObject = buildJsonObject {
        put("totalTrades", analytics.totalTrades)
        put("wins", analytics.wins)
        put("losses", analytics.losses)
        put("winRate", analytics.winRate)
        put("netProfit", analytics.netProfit)
        put("profitFactor", analytics.profitFactor)
        put("expectancy", analytics.expectancy)
        put("maxDrawdown", analytics.maxDrawdown)
    }
}

object Mt5AnalyticsCalculator {
    fun compute(
        entries: List<TradeJournalEntryEntity>,
    ): Mt5Analytics {
        val closed = entries.filter { it.source == TradeJournalSourceMt5 && it.grade != "open" }
        if (closed.isEmpty()) return Mt5Analytics.Empty

        var wins = 0
        var losses = 0
        var breakevens = 0
        var net = 0.0
        var grossWin = 0.0
        var grossLoss = 0.0
        var best = 0.0
        var worst = 0.0
        var runningEquity = 0.0
        var peak = 0.0
        var maxDrawdown = 0.0

        for (entry in closed) {
            val pnl = entry.resultAmount
            net += pnl
            when {
                pnl > 0.0001 -> {
                    wins++
                    grossWin += pnl
                    if (pnl > best) best = pnl
                }
                pnl < -0.0001 -> {
                    losses++
                    grossLoss += pnl
                    if (pnl < worst) worst = pnl
                }
                else -> breakevens++
            }
            runningEquity += pnl
            if (runningEquity > peak) peak = runningEquity
            val drawdown = peak - runningEquity
            if (drawdown > maxDrawdown) maxDrawdown = drawdown
        }

        val profitFactor = if (grossLoss == 0.0) {
            if (grossWin > 0.0) Double.POSITIVE_INFINITY else 0.0
        } else {
            grossWin / -grossLoss
        }
        val averageWin = if (wins > 0) grossWin / wins else 0.0
        val averageLoss = if (losses > 0) grossLoss / losses else 0.0

        return Mt5Analytics(
            totalTrades = closed.size,
            wins = wins,
            losses = losses,
            breakevens = breakevens,
            winRate = if (closed.isEmpty()) 0.0 else wins.toDouble() / closed.size,
            netProfit = net,
            profitFactor = profitFactor,
            averageWin = averageWin,
            averageLoss = averageLoss,
            expectancy = net / closed.size,
            maxDrawdown = maxDrawdown,
            bestTrade = best,
            worstTrade = worst,
        )
    }
}