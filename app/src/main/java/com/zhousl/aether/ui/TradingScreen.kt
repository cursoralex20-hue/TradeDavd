package com.zhousl.aether.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhousl.aether.data.journal.TradeJournalEntryEntity
import com.zhousl.aether.data.trading.Mt5AnalyticsCalculator
import com.zhousl.aether.data.trading.Mt5SyncConfig
import com.zhousl.aether.ui.theme.AetherOnSurface
import com.zhousl.aether.ui.theme.AetherOnSurfaceVariant
import com.zhousl.aether.ui.theme.AetherPrimary
import com.zhousl.aether.ui.theme.AetherSurface
import com.zhousl.aether.ui.theme.AetherSurfaceHigh
import com.zhousl.aether.ui.theme.AetherSurfaceHigher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TradingTab(val label: String) {
    CommandCenter("Comando"),
    Market("Mercado"),
    Events("Eventos"),
    Journal("Bitácora"),
    Analytics("Analytics"),
}

@Composable
fun TradingScreen(
    journalEntries: List<TradeJournalEntryEntity>,
    mt5SyncConfig: Mt5SyncConfig,
    onSaveMt5Config: (Mt5SyncConfig) -> Unit,
    onRunMt5Sync: () -> Unit,
    onExportTradeState: () -> Unit,
    onClose: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(TradingTab.CommandCenter.name) }
    val tab = TradingTab.entries.firstOrNull { it.name == selectedTab } ?: TradingTab.CommandCenter

    BackHandler { onClose() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AetherSurface)
    ) {
        TradingTopBar(onClose = onClose)
        TradingTabRow(
            selected = tab,
            onSelect = { selectedTab = it.name },
        )
        when (tab) {
            TradingTab.CommandCenter -> CommandCenterTab(
                config = mt5SyncConfig,
                entries = journalEntries,
                onSaveMt5Config = onSaveMt5Config,
                onRunMt5Sync = onRunMt5Sync,
            )
            TradingTab.Market -> MarketTab(entries = journalEntries)
            TradingTab.Events -> EventsTab(entries = journalEntries)
            TradingTab.Journal -> JournalTab(
                entries = journalEntries,
                onExportTradeState = onExportTradeState,
            )
            TradingTab.Analytics -> AnalyticsTab(entries = journalEntries)
        }
    }
}

@Composable
private fun TradingTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = AetherOnSurface,
            )
        }
        Text(
            text = "Trading · V1",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = AetherOnSurface,
        )
    }
}

@Composable
private fun TradingTabRow(
    selected: TradingTab,
    onSelect: (TradingTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AetherSurfaceHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TradingTab.entries.forEach { entry ->
            val isSelected = entry == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) AetherPrimary else Color.Transparent)
                    .clickable { onSelect(entry) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else AetherOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CommandCenterTab(
    config: Mt5SyncConfig,
    entries: List<TradeJournalEntryEntity>,
    onSaveMt5Config: (Mt5SyncConfig) -> Unit,
    onRunMt5Sync: () -> Unit,
) {
    var brokerServer by rememberSaveable { mutableStateOf(config.brokerServer) }
    var login by rememberSaveable { mutableStateOf(config.login) }
    var password by rememberSaveable { mutableStateOf(config.password) }
    var metaApiToken by rememberSaveable { mutableStateOf(config.metaApiToken) }
    var accountId by rememberSaveable { mutableStateOf(config.accountId) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Cuenta MT5 (MetaApi · free tier)",
                style = MaterialTheme.typography.titleMedium,
                color = AetherOnSurface,
            )
            Text(
                text = "La bitácora se auto-rellena con sync READ-ONLY de history-deals.",
                style = MaterialTheme.typography.bodySmall,
                color = AetherOnSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                value = brokerServer,
                onValueChange = { brokerServer = it },
                label = { Text("Broker server") },
                placeholder = { Text("ej. broker-demo.mql5.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Login") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña (investor/main)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        item {
            OutlinedTextField(
                value = metaApiToken,
                onValueChange = { metaApiToken = it },
                label = { Text("Token MetaApi") },
                placeholder = { Text("eyJ...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        item {
            OutlinedTextField(
                value = accountId,
                onValueChange = { accountId = it },
                label = { Text("Account ID (MetaApi)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        onSaveMt5Config(
                            Mt5SyncConfig(
                                brokerServer = brokerServer.trim(),
                                login = login.trim(),
                                password = password,
                                metaApiToken = metaApiToken.trim(),
                                accountId = accountId.trim(),
                                syncEnabled = true,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Guardar")
                }
                OutlinedButton(
                    onClick = onRunMt5Sync,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Sync ahora")
                }
            }
        }
        item {
            val closed = entries.filter { it.source == "mt5" }
            MetricTile(label = "Entradas importadas", value = closed.size.toString())
        }
        item {
            Text(
                text = if (config.isConfigured) {
                    "Sync configurado · última sincronización: " +
                        formatMillis(config.lastSyncAtMillis)
                } else {
                    "Config no guardada todavía."
                },
                style = MaterialTheme.typography.bodySmall,
                color = AetherOnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MarketTab(entries: List<TradeJournalEntryEntity>) {
    val assets = entries
        .map { it.asset }
        .filter(String::isNotBlank)
        .distinct()
        .sorted()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionTitle("Mercado")
        }
        if (assets.isEmpty()) {
            item {
                EmptyHint("Sin datos de mercado todavía. Sincroniza MT5 para ver activos operados.")
            }
        }
        items(assets) { asset ->
            val assetEntries = entries.filter { it.asset == asset }
            val net = assetEntries.sumOf { it.resultAmount }
            MetricTile(
                label = asset,
                value = "Trades: ${assetEntries.size} · Neto: ${"%.2f".format(net)}",
            )
        }
        item {
            Text(
                text = "En V1 el mercado se deriva de los trades sincronizados (XAU, BTC, ...).",
                style = MaterialTheme.typography.bodySmall,
                color = AetherOnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EventsTab(entries: List<TradeJournalEntryEntity>) {
    val sessions = entries
        .mapNotNull { entry -> entry.notes.takeIf { it.isNotBlank() } }
        .distinct()
        .take(20)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionTitle("Eventos")
        }
        if (sessions.isEmpty()) {
            item {
                EmptyHint("Sin eventos registrados todavía (comentarios de trades sincronizados).")
            }
        }
        items(sessions) { note ->
            MetricTile(label = "Nota", value = note)
        }
    }
}

@Composable
private fun JournalTab(
    entries: List<TradeJournalEntryEntity>,
    onExportTradeState: () -> Unit,
) {
    val sorted = entries.sortedByDescending { it.dateMillis }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle("Bitácora MT5")
                OutlinedButton(onClick = onExportTradeState) {
                    Text("Exportar trade_state")
                }
            }
        }
        if (sorted.isEmpty()) {
            item {
                EmptyHint("Bitácora vacía. Sincroniza MT5 para importar history-deals.")
            }
        }
        items(sorted.take(200), key = { it.id }) { entry ->
            JournalRow(entry)
        }
    }
}

@Composable
private fun AnalyticsTab(entries: List<TradeJournalEntryEntity>) {
    val analytics = Mt5AnalyticsCalculator.compute(entries)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionTitle("Analytics")
        }
        if (analytics.totalTrades == 0) {
            item { EmptyHint("Sin trades cerrados para analizar.") }
            return@LazyColumn
        }
        item { MetricTile("Total trades", analytics.totalTrades.toString()) }
        item { MetricTile("Wins / Losses / BE", "${analytics.wins} / ${analytics.losses} / ${analytics.breakevens}") }
        item { MetricTile("Win rate", "%.1f%%".format(analytics.winRate * 100)) }
        item { MetricTile("Net profit", "%.2f".format(analytics.netProfit)) }
        item { MetricTile("Profit factor", if (analytics.profitFactor == Double.POSITIVE_INFINITY) "∞" else "%.2f".format(analytics.profitFactor)) }
        item { MetricTile("Expectancy", "%.2f".format(analytics.expectancy)) }
        item { MetricTile("Avg win / avg loss", "%.2f / %.2f".format(analytics.averageWin, analytics.averageLoss)) }
        item { MetricTile("Max drawdown", "%.2f".format(analytics.maxDrawdown)) }
        item { MetricTile("Best / worst trade", "%.2f / %.2f".format(analytics.bestTrade, analytics.worstTrade)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = AetherOnSurface,
    )
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AetherSurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AetherOnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AetherOnSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun JournalRow(entry: TradeJournalEntryEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AetherSurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${entry.asset} · ${entry.side}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AetherOnSurface,
            )
            Text(
                text = formatMillis(entry.dateMillis),
                style = MaterialTheme.typography.labelSmall,
                color = AetherOnSurfaceVariant,
            )
        }
        Text(
            text = "Resultado: ${"%.2f".format(entry.resultAmount)} · Vol: ${"%.2f".format(entry.volume)}",
            style = MaterialTheme.typography.bodySmall,
            color = AetherOnSurfaceVariant,
        )
        val gradeColor = when (entry.grade) {
            "win" -> Color(0xFF2E7D32)
            "loss" -> Color(0xFFC62828)
            else -> AetherOnSurfaceVariant
        }
        Text(
            text = entry.grade,
            style = MaterialTheme.typography.labelSmall,
            color = gradeColor,
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = AetherOnSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .widthIn(max = 320.dp),
    )
}

private fun formatMillis(millis: Long): String {
    if (millis <= 0L) return "nunca"
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))
    }.getOrDefault("")
}