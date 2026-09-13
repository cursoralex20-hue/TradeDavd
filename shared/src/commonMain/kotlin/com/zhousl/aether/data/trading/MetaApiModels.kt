package com.zhousl.aether.data.trading

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetaApiAccountInformation(
    @SerialName("balance") val balance: Double = 0.0,
    @SerialName("equity") val equity: Double = 0.0,
    @SerialName("margin") val margin: Double = 0.0,
    @SerialName("marginFree") val marginFree: Double = 0.0,
    @SerialName("profit") val profit: Double = 0.0,
    @SerialName("storage") val storage: Double = 0.0,
    @SerialName("currency") val currency: String = "",
    @SerialName("leverage") val leverage: Int = 0,
    @SerialName("server") val server: String = "",
    @SerialName("type") val type: String = "",
)

@Serializable
data class MetaApiPosition(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("volume") val volume: Double,
    @SerialName("openPrice") val openPrice: Double,
    @SerialName("currentPrice") val currentPrice: Double,
    @SerialName("profit") val profit: Double,
    @SerialName("time") val time: Long,
    @SerialName("magic") val magic: Long = 0L,
    @SerialName("comment") val comment: String = "",
    @SerialName("swap") val swap: Double = 0.0,
)

@Serializable
data class MetaApiDeal(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("volume") val volume: Double,
    @SerialName("price") val price: Double,
    @SerialName("profit") val profit: Double,
    @SerialName("commission") val commission: Double = 0.0,
    @SerialName("swap") val swap: Double = 0.0,
    @SerialName("time") val time: Long,
    @SerialName("magic") val magic: Long = 0L,
    @SerialName("entry") val entry: String = "",
    @SerialName("comment") val comment: String = "",
    @SerialName("positionId") val positionId: String = "",
    @SerialName("ticket") val ticket: Long = 0L,
)

@Serializable
data class MetaApiPositionsResponse(
    @SerialName("positions") val positions: List<MetaApiPosition> = emptyList(),
)

@Serializable
data class MetaApiHistoryDealsResponse(
    @SerialName("history") val history: List<MetaApiDeal> = emptyList(),
)

@Serializable
data class Mt5SyncConfig(
    val brokerServer: String = "",
    val login: String = "",
    val password: String = "",
    val metaApiToken: String = "",
    val accountId: String = "",
    val syncEnabled: Boolean = true,
    val lastSyncAtMillis: Long = 0L,
) {
    val isConfigured: Boolean
        get() = metaApiToken.isNotBlank() && accountId.isNotBlank()
}