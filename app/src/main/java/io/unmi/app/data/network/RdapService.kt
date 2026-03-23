package io.unmi.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class RdapResult(
    val domainName: String,
    val registrar: String?,
    val registrationDate: String?,
    val expirationDate: String?,
    val lastChangedDate: String?,
    val status: List<String>,
    val nameservers: List<String>,
    val dnssecSigned: Boolean,
    val rawJson: String
)

@Singleton
class RdapService @Inject constructor(
    private val apiService: ApiService,
    private val whoisFallback: WhoisFallbackService
) {
    private val rateLimiter = RateLimiter(maxTokens = 3, refillIntervalMs = 3000L)

    // Dynamic RDAP server map loaded from IANA bootstrap
    private val rdapServerMap = ConcurrentHashMap<String, String>()
    private val bootstrapMutex = Mutex()
    private var bootstrapLoaded = false

    /**
     * Load IANA RDAP bootstrap data (1198+ TLDs).
     * Format: { "services": [ [["tld1","tld2"], ["https://rdap.server/..."]], ... ] }
     */
    private suspend fun ensureBootstrapLoaded() {
        if (bootstrapLoaded) return
        bootstrapMutex.withLock {
            if (bootstrapLoaded) return
            try {
                val response = apiService.getRdapBootstrap()
                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: return
                    val json = Json { ignoreUnknownKeys = true }
                    val root = json.parseToJsonElement(body).jsonObject
                    val services = root["services"]?.jsonArray ?: return

                    for (service in services) {
                        val arr = service.jsonArray
                        val tlds = arr[0].jsonArray
                        val urls = arr[1].jsonArray
                        val serverUrl = urls.firstOrNull {
                            it.jsonPrimitive.content.startsWith("https://")
                        }?.jsonPrimitive?.content
                            ?: urls.firstOrNull()?.jsonPrimitive?.content
                            ?: continue

                        val cleanUrl = serverUrl.trimEnd('/')
                        for (tld in tlds) {
                            rdapServerMap[tld.jsonPrimitive.content.lowercase()] = cleanUrl
                        }
                    }
                    bootstrapLoaded = true
                }
            } catch (_: Exception) {
                // Fallback: will use rdap.org
            }
        }
    }

    private fun getRdapServer(tld: String): String {
        return rdapServerMap[tld.lowercase()]
            ?: "https://rdap.org" // Universal fallback with auto-redirect
    }

    suspend fun queryDomain(domain: String): RdapResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()

        // Ensure we have the bootstrap data
        ensureBootstrapLoaded()

        val tld = domain.substringAfterLast(".")

        // Check if this TLD has an RDAP server; if not, use WHOIS fallback directly
        if (!rdapServerMap.containsKey(tld.lowercase())) {
            return@withContext whoisFallback.queryDomain(domain)
        }
        // Try RDAP first, fallback to WHOIS on any failure
        try {
            val baseUrl = getRdapServer(tld)
            val url = if (baseUrl.contains("/domain")) "$baseUrl/$domain"
                      else "$baseUrl/domain/$domain"

            val response = apiService.queryRdap(url)

            if (!response.isSuccessful) {
                return@withContext whoisFallback.queryDomain(domain)
            }

            val body = response.body()?.string()
            if (body.isNullOrBlank()) {
                return@withContext whoisFallback.queryDomain(domain)
            }

            parseRdapResponse(domain, body)
        } catch (_: Exception) {
            whoisFallback.queryDomain(domain)
        }
    }

    private fun parseRdapResponse(domain: String, jsonStr: String): RdapResult {
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(jsonStr).jsonObject

        // Parse registrar - try multiple approaches
        val registrar = parseRegistrar(root)

        // Parse events (dates)
        var registrationDate: String? = null
        var expirationDate: String? = null
        var lastChangedDate: String? = null

        root["events"]?.jsonArray?.forEach { event ->
            val obj = event.jsonObject
            val action = obj["eventAction"]?.jsonPrimitive?.content
            val date = obj["eventDate"]?.jsonPrimitive?.content
            when (action) {
                "registration" -> registrationDate = date?.take(10)
                "expiration" -> expirationDate = date?.take(10)
                "last changed" -> lastChangedDate = date?.take(10)
            }
        }

        // Parse status
        val status = root["status"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

        // Parse nameservers
        val nameservers = root["nameservers"]?.jsonArray?.mapNotNull {
            it.jsonObject["ldhName"]?.jsonPrimitive?.content
        } ?: emptyList()

        // DNSSEC
        val dnssecSigned = root["secureDNS"]?.jsonObject?.get("delegationSigned")
            ?.jsonPrimitive?.booleanOrNull == true

        return RdapResult(
            domainName = root["ldhName"]?.jsonPrimitive?.content?.lowercase() ?: domain,
            registrar = registrar,
            registrationDate = registrationDate,
            expirationDate = expirationDate,
            lastChangedDate = lastChangedDate,
            status = status,
            nameservers = nameservers,
            dnssecSigned = dnssecSigned,
            rawJson = jsonStr
        )
    }

    private fun parseRegistrar(root: JsonObject): String? {
        val entities = root["entities"]?.jsonArray ?: return null

        for (entity in entities) {
            val obj = entity.jsonObject
            val roles = obj["roles"]?.jsonArray?.map { it.jsonPrimitive.content } ?: continue

            if ("registrar" in roles) {
                // Method 1: vcardArray -> fn
                obj["vcardArray"]?.jsonArray?.getOrNull(1)?.jsonArray?.let { vcard ->
                    for (item in vcard) {
                        try {
                            val arr = item.jsonArray
                            if (arr.getOrNull(0)?.jsonPrimitive?.content == "fn") {
                                return arr.getOrNull(3)?.jsonPrimitive?.content
                            }
                        } catch (_: Exception) { }
                    }
                }

                // Method 2: publicIds
                obj["publicIds"]?.jsonArray?.firstOrNull()?.let { pid ->
                    return pid.jsonObject["identifier"]?.jsonPrimitive?.content
                }

                // Method 3: handle field
                obj["handle"]?.jsonPrimitive?.content?.let { return it }
            }
        }
        return null
    }
}
