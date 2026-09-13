package com.zhousl.aether.data.trading

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

const val MetaApiDefaultBaseUrl =
    "https://mt-client-api-v1.agiliumtrade.agiliumtrade.ai"

class MetaApiClient(
    private val token: String,
    private val baseUrl: String = MetaApiDefaultBaseUrl,
    private val engine: HttpClientEngine? = null,
) {
    private val client = createClient(this.engine)

    suspend fun getAccountInformation(accountId: String): Result<MetaApiAccountInformation> =
        safeGet("/users/current/accounts/$accountId/accountInformation", accountId)

    suspend fun getPositions(accountId: String): Result<List<MetaApiPosition>> =
        safeGet("/users/current/accounts/$accountId/positions", accountId).map { response ->
            Json.decodeFromString<MetaApiPositionsResponse>(String(response, Charsets.UTF_8)).positions
        }

    suspend fun getHistoryDeals(
        accountId: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
    ): Result<List<MetaApiDeal>> =
        safeGet(
            "/users/current/accounts/$accountId/history-deals/time/$startTimeMillis/$endTimeMillis",
            accountId,
        ).map { response ->
            Json.decodeFromString<MetaApiHistoryDealsResponse>(String(response, Charsets.UTF_8)).history
        }

    private suspend fun safeGet(
        path: String,
        accountId: String,
    ): Result<ByteArray> = runCatching {
        val response: HttpResponse = client.get("$baseUrl$path") {
            header(HttpHeaders.Authorization, token)
            header("auth-token", token)
        }
        if (response.status.value !in 200..299) {
            error("MetaApi HTTP ${response.status.value} for $accountId")
        }
        response.bodyAsBytes()
    }

    private companion object {
        private val Json = Json { ignoreUnknownKeys = true }

        fun createClient(engine: HttpClientEngine?): HttpClient =
            if (engine == null) {
                HttpClient { configureTimeouts() }
            } else {
                HttpClient(engine) { configureTimeouts() }
            }

        fun io.ktor.client.HttpClientConfig<*>.configureTimeouts() {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
                requestTimeoutMillis = 30_000
            }
        }
    }
}