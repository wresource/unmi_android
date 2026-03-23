package io.unmi.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class DomainPrice(
    val registrar: String,
    val registrarCode: String,
    val registrarWeb: String,
    val registerPrice: Double,
    val renewPrice: Double,
    val transferPrice: Double,
    val currency: String,
    val currencyName: String
)

data class TldPriceInfo(
    val tld: String,
    val prices: List<DomainPrice>,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    fun cheapestRenew(): DomainPrice? = prices.minByOrNull { it.renewPrice }
    fun cheapestRegister(): DomainPrice? = prices.minByOrNull { it.registerPrice }
}

@Singleton
class NazhumiPriceService @Inject constructor(
    private val apiService: ApiService
) {
    private val rateLimiter = RateLimiter(maxTokens = 3, refillIntervalMs = 3000L)
    private val cache = ConcurrentHashMap<String, TldPriceInfo>()
    private val cacheValidityMs = 24 * 60 * 60 * 1000L

    suspend fun getTldPrices(tld: String): TldPriceInfo = withContext(Dispatchers.IO) {
        val cleanTld = tld.removePrefix(".").lowercase()

        cache[cleanTld]?.let { cached ->
            if (System.currentTimeMillis() - cached.fetchedAt < cacheValidityMs) {
                return@withContext cached
            }
        }

        // Query all three order types in parallel to get more registrars
        val newDeferred = async { fetchPrices(cleanTld, "new") }
        val renewDeferred = async { fetchPrices(cleanTld, "renew") }
        val transferDeferred = async { fetchPrices(cleanTld, "transfer") }

        val newPrices = newDeferred.await()
        val renewPrices = renewDeferred.await()
        val transferPrices = transferDeferred.await()

        // Merge and deduplicate by registrar code
        val merged = mutableMapOf<String, DomainPrice>()
        for (list in listOf(newPrices, renewPrices, transferPrices)) {
            for (p in list) {
                merged.putIfAbsent(p.registrarCode, p)
            }
        }

        val result = TldPriceInfo(tld = cleanTld, prices = merged.values.toList())
        cache[cleanTld] = result
        result
    }

    private suspend fun fetchPrices(tld: String, order: String): List<DomainPrice> {
        rateLimiter.acquire()
        return try {
            val response = apiService.getNazhumiPrices(domain = tld, order = order)
            if (!response.isSuccessful) return emptyList()
            val body = response.body()?.string() ?: return emptyList()
            parseApiResponse(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseApiResponse(jsonStr: String): List<DomainPrice> {
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(jsonStr).jsonObject

        val code = root["code"]?.jsonPrimitive?.intOrNull
        if (code != 100) return emptyList()

        val data = root["data"]?.jsonObject ?: return emptyList()
        val priceArray = data["price"]?.jsonArray ?: return emptyList()

        return priceArray.mapNotNull { element ->
            try {
                val obj = element.jsonObject
                val newPrice = obj["new"]?.let { parsePrice(it) } ?: 0.0
                val renewPrice = obj["renew"]?.let { parsePrice(it) } ?: 0.0
                val transferPrice = obj["transfer"]?.let { parsePrice(it) } ?: 0.0

                DomainPrice(
                    registrar = obj["registrarname"]?.jsonPrimitive?.content ?: "",
                    registrarCode = obj["registrar"]?.jsonPrimitive?.content ?: "",
                    registrarWeb = obj["registrarweb"]?.jsonPrimitive?.content ?: "",
                    registerPrice = newPrice,
                    renewPrice = renewPrice,
                    transferPrice = transferPrice,
                    currency = (obj["currency"]?.jsonPrimitive?.content ?: "usd").uppercase(),
                    currencyName = obj["currencyname"]?.jsonPrimitive?.content ?: ""
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parsePrice(element: JsonElement): Double {
        return when {
            element is JsonPrimitive && element.isString -> {
                if (element.content == "n/a") 0.0
                else element.content.toDoubleOrNull() ?: 0.0
            }
            element is JsonPrimitive -> element.doubleOrNull ?: 0.0
            else -> 0.0
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
