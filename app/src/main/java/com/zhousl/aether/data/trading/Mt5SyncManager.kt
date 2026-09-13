package com.zhousl.aether.data.trading

import android.content.Context
import com.zhousl.aether.data.SettingsRepository
import com.zhousl.aether.data.journal.RoomTradeJournalReadStore
import com.zhousl.aether.data.journal.TradeJournalDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

sealed interface Mt5SyncState {
    data object Idle : Mt5SyncState
    data object Syncing : Mt5SyncState
    data class Success(
        val recentDeals: Int,
        val openPositions: Int,
        val importedEntries: Int,
        val atMillis: Long,
    ) : Mt5SyncState
    data class Failure(val message: String) : Mt5SyncState
}

class Mt5SyncManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val journalDao: TradeJournalDao,
    private val journalReadStore: RoomTradeJournalReadStore,
) {
    private val Json = Json { ignoreUnknownKeys = true }

    suspend fun runSync(
        config: Mt5SyncConfig,
    ): Mt5SyncState {
        if (!config.isConfigured) {
            return Mt5SyncState.Failure("Config MT5 incompleta (token MetaApi y accountId requeridos).")
        }
        val client = MetaApiClient(token = config.metaApiToken)
        return try {
            val now = System.currentTimeMillis()
            val since = if (config.lastSyncAtMillis > 0L) config.lastSyncAtMillis else now - 90L * 24 * 60 * 60 * 1000
            val dealsResult = client.getHistoryDeals(
                accountId = config.accountId,
                startTimeMillis = since,
                endTimeMillis = now,
            )
            val deals = dealsResult.getOrElse { throw it }
            val positionsResult = client.getPositions(config.accountId)
            val positions = positionsResult.getOrElse { throw it }
            val accountResult = client.getAccountInformation(config.accountId)
            val account = accountResult.getOrNull()

            val entries = Mt5JournalMapper.mapHistoryDeals(deals)
            if (entries.isNotEmpty()) {
                val existing = journalReadStore.all()
                val existingIds = existing.map { it.id }.toSet()
                val fresh = entries.filterNot { it.id in existingIds }
                if (fresh.isNotEmpty()) {
                    journalDao.upsertAll(fresh)
                }
            }

            val allEntries = journalReadStore.all().filter { it.source == TradeJournalSourceMt5 }
            val analytics = Mt5AnalyticsCalculator.compute(allEntries)
            val tradeState = Mt5JournalMapper.buildTradeStateJson(
                config = config.copy(lastSyncAtMillis = now),
                account = account,
                openPositions = positions,
                entries = allEntries.sortedByDescending { it.dateMillis }.take(500),
                analytics = analytics,
            )
            writeTradeStateJson(tradeState)

            settingsRepository.saveMt5SyncConfigJson(
                Json.encodeToString(
                    serializer = Mt5SyncConfig.serializer(),
                    value = config.copy(lastSyncAtMillis = now),
                ),
            )

            Mt5SyncState.Success(
                recentDeals = deals.size,
                openPositions = positions.size,
                importedEntries = entries.size,
                atMillis = now,
            )
        } catch (throwable: Throwable) {
            Mt5SyncState.Failure(throwable.message ?: throwable.javaClass.simpleName)
        }
    }

    fun tradeStateFile(): File = File(context.filesDir, "trade_state.json")

    suspend fun latestConfig(): Mt5SyncConfig {
        val raw = settingsRepository.mt5SyncConfigJson().first()
        if (raw.isBlank()) return Mt5SyncConfig()
        return runCatching {
            Json.decodeFromString(Mt5SyncConfig.serializer(), raw)
        }.getOrDefault(Mt5SyncConfig())
    }

    fun latestTradeStateJson(): String? =
        tradeStateFile().takeIf { it.exists() }?.readText(Charsets.UTF_8)

    private fun writeTradeStateJson(json: String) {
        FileOutputStream(tradeStateFile()).use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
        }
    }
}