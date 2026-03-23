package io.unmi.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Traditional WHOIS fallback for TLDs without RDAP support.
 * Handles different response formats from various registries and performs data cleaning.
 */
@Singleton
class WhoisFallbackService @Inject constructor() {

    private val rateLimiter = RateLimiter(maxTokens = 2, refillIntervalMs = 5000L)

    // Known WHOIS servers for TLDs without RDAP
    private val whoisServers = mapOf(
        // Asia
        "jp" to "whois.jprs.jp",
        "kr" to "whois.kr",
        "in" to "whois.registry.in",
        "hk" to "whois.hkirc.hk",
        "tw" to "whois.twnic.net.tw",
        "sg" to "whois.sgnic.sg",
        "th" to "whois.thnic.co.th",
        "vn" to "whois.vnnic.vn",
        "my" to "whois.mynic.my",
        "ph" to "whois.dot.ph",
        "id" to "whois.id",
        // Europe
        "uk" to "whois.nic.uk",
        "de" to "whois.denic.de",
        "fr" to "whois.nic.fr",
        "it" to "whois.nic.it",
        "nl" to "whois.sidn.nl",
        "es" to "whois.nic.es",
        "pt" to "whois.dns.pt",
        "pl" to "whois.dns.pl",
        "cz" to "whois.nic.cz",
        "ch" to "whois.nic.ch",
        "at" to "whois.nic.at",
        "be" to "whois.dns.be",
        "se" to "whois.iis.se",
        "no" to "whois.norid.no",
        "fi" to "whois.fi",
        "dk" to "whois.dk-hostmaster.dk",
        "ie" to "whois.iedr.ie",
        "ru" to "whois.tcinet.ru",
        "ua" to "whois.ua",
        "eu" to "whois.eu",
        // Americas
        "us" to "whois.nic.us",
        "ca" to "whois.cira.ca",
        "br" to "whois.registro.br",
        "mx" to "whois.mx",
        "ar" to "nic.ar",
        "cl" to "whois.nic.cl",
        "co" to "whois.nic.co",
        // Africa/Middle East
        "za" to "whois.registry.net.za",
        "ng" to "whois.nic.net.ng",
        "ke" to "whois.kenic.or.ke",
        "il" to "whois.isoc.org.il",
        "ir" to "whois.nic.ir",
        "ae" to "whois.aeda.net.ae",
        "sa" to "whois.nic.net.sa",
        // Oceania
        "au" to "whois.auda.org.au",
        "nz" to "whois.srs.net.nz",
        // Islands / ccTLDs
        "io" to "whois.nic.io",
        "bi" to "whois.nic.bi",
        "me" to "whois.nic.me",
        "tv" to "whois.nic.tv",
        "cc" to "ccwhois.verisign-grs.com",
        "ws" to "whois.website.ws",
        "la" to "whois.nic.la",
        "li" to "whois.nic.li",
        "to" to "whois.tonic.to",
        "so" to "whois.nic.so",
        "fm" to "whois.nic.fm",
        "am" to "whois.amnic.net",
        "gg" to "whois.gg",
        "im" to "whois.nic.im",
        "is" to "whois.isnic.is",
        "cx" to "whois.nic.cx",
        "nu" to "whois.iis.nu",
        "mu" to "whois.nic.mu",
        "re" to "whois.nic.re",
        "pm" to "whois.nic.pm",
        "tf" to "whois.nic.tf",
        "wf" to "whois.nic.wf",
        "yt" to "whois.nic.yt",
        // Generic TLDs fallback
        "com" to "whois.verisign-grs.com",
        "net" to "whois.verisign-grs.com",
        "org" to "whois.pir.org",
        "info" to "whois.afilias.net",
        "biz" to "whois.nic.biz",
        "mobi" to "whois.dotmobiregistry.net",
        "name" to "whois.nic.name",
        "pro" to "whois.registrypro.pro",
        // New gTLDs
        "xyz" to "whois.nic.xyz",
        "top" to "whois.nic.top",
        "club" to "whois.nic.club",
        "vip" to "whois.nic.vip",
        "shop" to "whois.nic.shop",
        "app" to "whois.nic.google",
        "dev" to "whois.nic.google",
        "ai" to "whois.nic.ai",
        "icu" to "whois.nic.icu",
        "site" to "whois.nic.site",
        "online" to "whois.nic.online",
        "store" to "whois.nic.store",
        "fun" to "whois.nic.fun",
        "space" to "whois.nic.space",
        "tech" to "whois.nic.tech",
        "cloud" to "whois.nic.cloud",
        "ink" to "whois.nic.ink",
        "wiki" to "whois.nic.wiki",
        "design" to "whois.nic.design",
        "blog" to "whois.nic.blog",
        "ltd" to "whois.nic.ltd",
        "group" to "whois.nic.group",
        "live" to "whois.nic.live",
        "work" to "whois.nic.work",
        "world" to "whois.nic.world",
        "today" to "whois.nic.today",
        "life" to "whois.nic.life",
        "network" to "whois.nic.network",
        "email" to "whois.nic.email",
        "company" to "whois.nic.company",
        "center" to "whois.nic.center"
    )

    suspend fun queryDomain(domain: String): RdapResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()

        val tld = domain.substringAfterLast(".")
        val server = whoisServers[tld.lowercase()]
            ?: "whois.iana.org" // IANA will tell us the right server

        val rawText = performWhoisQuery(domain, server)

        // If IANA, check for referral
        if (server == "whois.iana.org") {
            val referral = Regex("""refer:\s*(\S+)""", RegexOption.IGNORE_CASE)
                .find(rawText)?.groupValues?.get(1)
            if (referral != null) {
                val referralText = performWhoisQuery(domain, referral)
                return@withContext parseWhoisResponse(domain, referralText)
            }
        }

        // Some servers like .de need special query format
        val queryDomain = when (tld.lowercase()) {
            "de" -> "-T dn,ace $domain"
            "jp" -> "$domain/e" // English output
            else -> domain
        }
        if (queryDomain != domain) {
            val specialText = performWhoisQuery(queryDomain, server)
            return@withContext parseWhoisResponse(domain, specialText)
        }

        parseWhoisResponse(domain, rawText)
    }

    private fun performWhoisQuery(query: String, server: String): String {
        val socket = Socket(server, 43)
        socket.soTimeout = 10000
        socket.getOutputStream().write("$query\r\n".toByteArray())
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val result = reader.readText()
        socket.close()
        return result
    }

    /**
     * Universal WHOIS response parser with data cleaning.
     * Handles multiple formats from different registries.
     */
    fun parseWhoisResponse(domain: String, rawText: String): RdapResult {
        val lines = rawText.lines()

        var registrar: String? = null
        var registrationDate: String? = null
        var expirationDate: String? = null
        var lastChangedDate: String? = null
        val status = mutableListOf<String>()
        val nameservers = mutableListOf<String>()
        var dnssec = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%") || trimmed.startsWith("#")
                || trimmed.startsWith(">>>") || trimmed.startsWith("[")) continue

            val colonIndex = trimmed.indexOf(':')
            if (colonIndex <= 0) continue

            val key = trimmed.substring(0, colonIndex).trim().lowercase()
            val value = trimmed.substring(colonIndex + 1).trim()
            if (value.isEmpty()) continue

            // Registrar
            when {
                key in setOf("registrar", "registrant", "sponsoring registrar",
                    "registrar name", "registrar organization") && registrar == null ->
                    registrar = value

                // Registration date
                key in setOf("creation date", "created", "created date", "registered",
                    "registration date", "domain name commencement date",
                    "registered date", "登録年月日", "[登録年月日]", "created on",
                    "registration time", "record created") ->
                    registrationDate = cleanDate(value)

                // Expiration date
                key in setOf("registry expiry date", "expiration date", "expires",
                    "expiry date", "expire date", "有効期限", "[有効期限]",
                    "registrar registration expiration date", "expired",
                    "expiration time", "paid-till", "free-date", "renewal date",
                    "record expires") ->
                    expirationDate = cleanDate(value)

                // Last updated
                key in setOf("updated date", "last updated", "last modified",
                    "changed", "最終更新", "[最終更新]", "last update",
                    "modified", "record last updated") ->
                    lastChangedDate = cleanDate(value)

                // Status
                key in setOf("domain status", "status", "状態", "[状態]",
                    "state", "domain state") ->
                    status.add(value.substringBefore(" ").trim())

                // Nameservers
                key in setOf("name server", "nameserver", "nserver",
                    "nameservers", "ns", "name servers") ->
                    nameservers.add(value.substringBefore(" ").trim().lowercase())

                // DNSSEC
                key in setOf("dnssec", "signing key") ->
                    dnssec = value.lowercase() !in setOf("unsigned", "no", "inactive", "")
            }
        }

        return RdapResult(
            domainName = domain.lowercase(),
            registrar = registrar,
            registrationDate = registrationDate,
            expirationDate = expirationDate,
            lastChangedDate = lastChangedDate,
            status = status.distinct(),
            nameservers = nameservers.distinct(),
            dnssecSigned = dnssec,
            rawJson = rawText // Store raw whois text
        )
    }

    /**
     * Clean and normalize date strings from various whois formats:
     * "2023-07-19T04:00:00Z" -> "2023-07-19"
     * "2023/07/19" -> "2023-07-19"
     * "19-Jul-2023" -> "2023-07-19"
     * "2023.07.19" -> "2023-07-19"
     * "2023年07月19日" -> "2023-07-19"
     */
    private fun cleanDate(raw: String): String? {
        val trimmed = raw.trim()

        // ISO format: 2023-07-19T04:00:00Z
        Regex("""(\d{4}-\d{2}-\d{2})""").find(trimmed)?.let {
            return it.groupValues[1]
        }

        // Slash format: 2023/07/19
        Regex("""(\d{4})/(\d{1,2})/(\d{1,2})""").find(trimmed)?.let {
            return "${it.groupValues[1]}-${it.groupValues[2].padStart(2, '0')}-${it.groupValues[3].padStart(2, '0')}"
        }

        // Dot format: 2023.07.19
        Regex("""(\d{4})\.(\d{1,2})\.(\d{1,2})""").find(trimmed)?.let {
            return "${it.groupValues[1]}-${it.groupValues[2].padStart(2, '0')}-${it.groupValues[3].padStart(2, '0')}"
        }

        // Japanese: 2023年07月19日
        Regex("""(\d{4})年(\d{1,2})月(\d{1,2})日""").find(trimmed)?.let {
            return "${it.groupValues[1]}-${it.groupValues[2].padStart(2, '0')}-${it.groupValues[3].padStart(2, '0')}"
        }

        // DD-Mon-YYYY: 19-Jul-2023
        val months = mapOf(
            "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
            "may" to "05", "jun" to "06", "jul" to "07", "aug" to "08",
            "sep" to "09", "oct" to "10", "nov" to "11", "dec" to "12"
        )
        Regex("""(\d{1,2})[-\s](jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\w*[-\s](\d{4})""",
            RegexOption.IGNORE_CASE).find(trimmed)?.let {
            val month = months[it.groupValues[2].lowercase().take(3)] ?: return null
            return "${it.groupValues[3]}-$month-${it.groupValues[1].padStart(2, '0')}"
        }

        return null
    }

    fun hasWhoisServer(tld: String): Boolean {
        return whoisServers.containsKey(tld.lowercase())
    }
}
