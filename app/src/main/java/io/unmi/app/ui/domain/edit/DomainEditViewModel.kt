package io.unmi.app.ui.domain.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.data.local.db.entity.DomainEntity
import io.unmi.app.data.network.DomainPrice
import io.unmi.app.data.network.DomainValuationEngine
import io.unmi.app.data.network.NazhumiPriceService
import io.unmi.app.data.network.RdapService
import io.unmi.app.data.network.ValuationResult
import io.unmi.app.data.repository.DomainRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DomainEditUiState(
    val isNew: Boolean = true,
    val domainName: String = "",
    val tld: String = ".com",
    val registrar: String = "",
    val registerDate: String = "",
    val expireDate: String = "",
    val autoRenew: Boolean = false,
    val status: String = "active",
    val purchasePrice: String = "0",
    val renewPrice: String = "0",
    val holdCost: String = "0",
    val currency: String = "CNY",
    val dnsProvider: String = "",
    val privacyProtection: Boolean = false,
    val usageType: String = "",
    val note: String = "",
    val nameservers: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    // RDAP auto-fill
    val isQuerying: Boolean = false,
    val queryMessage: String? = null,
    // Pricing
    val isFetchingPrice: Boolean = false,
    val priceList: List<DomainPrice> = emptyList(),
    val priceMessage: String? = null,
    // Valuation
    val valuation: ValuationResult? = null
)

@HiltViewModel
class DomainEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domainRepository: DomainRepository,
    private val rdapService: RdapService,
    private val nazhumiPriceService: NazhumiPriceService,
    private val valuationEngine: DomainValuationEngine
) : ViewModel() {

    private val domainId: Long? = savedStateHandle.get<Long>("id")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(DomainEditUiState(isNew = domainId == null))
    val uiState: StateFlow<DomainEditUiState> = _uiState.asStateFlow()

    init {
        if (domainId != null) {
            viewModelScope.launch {
                domainRepository.getById(domainId)?.let { domain ->
                    _uiState.update {
                        it.copy(
                            isNew = false,
                            domainName = domain.domainName,
                            tld = domain.tld,
                            registrar = domain.registrar ?: "",
                            registerDate = domain.registerDate ?: "",
                            expireDate = domain.expireDate,
                            autoRenew = domain.autoRenew,
                            status = domain.status,
                            purchasePrice = domain.purchasePrice.toString(),
                            renewPrice = domain.renewPrice.toString(),
                            holdCost = domain.holdCost.toString(),
                            currency = domain.currency,
                            dnsProvider = domain.dnsProvider ?: "",
                            privacyProtection = domain.privacyProtection,
                            usageType = domain.usageType ?: "",
                            note = domain.note ?: "",
                            nameservers = domain.nameservers ?: ""
                        )
                    }
                    // Auto-evaluate valuation for existing domain
                    evaluateValue()
                }
            }
        }
    }

    fun updateField(field: String, value: String) {
        _uiState.update { state ->
            when (field) {
                "domainName" -> state.copy(domainName = value)
                "tld" -> state.copy(tld = value)
                "registrar" -> state.copy(registrar = value)
                "registerDate" -> state.copy(registerDate = value)
                "expireDate" -> state.copy(expireDate = value)
                "status" -> state.copy(status = value)
                "purchasePrice" -> state.copy(purchasePrice = value)
                "renewPrice" -> state.copy(renewPrice = value)
                "holdCost" -> state.copy(holdCost = value)
                "currency" -> state.copy(currency = value)
                "dnsProvider" -> state.copy(dnsProvider = value)
                "usageType" -> state.copy(usageType = value)
                "note" -> state.copy(note = value)
                "nameservers" -> state.copy(nameservers = value)
                else -> state
            }
        }
    }

    fun updateBoolean(field: String, value: Boolean) {
        _uiState.update { state ->
            when (field) {
                "autoRenew" -> state.copy(autoRenew = value)
                "privacyProtection" -> state.copy(privacyProtection = value)
                else -> state
            }
        }
    }

    /**
     * Single button: auto-identify domain info + pricing in parallel via coroutines.
     * RDAP and price queries run concurrently, UI updates as each completes.
     */
    fun autoIdentify() {
        val domain = _uiState.value.domainName.trim()
        if (domain.isBlank() || !domain.contains(".")) {
            _uiState.update { it.copy(queryMessage = "Please enter a full domain name") }
            return
        }

        val tld = domain.substringAfterLast(".")

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isQuerying = true,
                    isFetchingPrice = true,
                    queryMessage = "Identifying...",
                    priceMessage = "Fetching prices..."
                )
            }

            // Launch RDAP and price queries in parallel
            val rdapDeferred = async {
                try {
                    rdapService.queryDomain(domain)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isQuerying = false, queryMessage = "RDAP failed: ${e.message}")
                    }
                    null
                }
            }

            val priceDeferred = async {
                try {
                    nazhumiPriceService.getTldPrices(tld)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isFetchingPrice = false, priceMessage = "Price query failed: ${e.message}")
                    }
                    null
                }
            }

            // Process RDAP result
            val rdapResult = rdapDeferred.await()
            if (rdapResult != null) {
                _uiState.update { state ->
                    state.copy(
                        isQuerying = false,
                        queryMessage = "RDAP data filled",
                        registrar = rdapResult.registrar ?: state.registrar,
                        registerDate = rdapResult.registrationDate ?: state.registerDate,
                        expireDate = rdapResult.expirationDate ?: state.expireDate,
                        tld = ".$tld",
                        nameservers = rdapResult.nameservers.joinToString(", "),
                        dnsProvider = inferDnsProvider(rdapResult.nameservers),
                        status = mapRdapStatus(rdapResult.status)
                    )
                }
            }

            // Process price result
            val priceResult = priceDeferred.await()
            if (priceResult != null) {
                _uiState.update {
                    it.copy(
                        isFetchingPrice = false,
                        priceList = priceResult.prices,
                        priceMessage = "${priceResult.prices.size} prices (.${tld})"
                    )
                }
            }

            // Evaluate value after both queries complete
            evaluateValue()

            // Build final summary message
            val parts = mutableListOf<String>()
            if (rdapResult != null) parts.add("RDAP OK")
            if (priceResult != null && priceResult.prices.isNotEmpty()) {
                parts.add("${priceResult.prices.size} prices")
            }
            _uiState.update {
                it.copy(
                    queryMessage = if (parts.isNotEmpty()) parts.joinToString(" | ") else "Done",
                    isQuerying = false,
                    isFetchingPrice = false
                )
            }
        }
    }

    /**
     * Fetch pricing only (for edit mode)
     */
    fun fetchPricing() {
        val tld = _uiState.value.tld.removePrefix(".").ifBlank {
            _uiState.value.domainName.substringAfterLast(".", "")
        }
        if (tld.isBlank()) {
            _uiState.update { it.copy(priceMessage = "Please enter domain or TLD") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingPrice = true, priceMessage = "Fetching prices...") }
            try {
                val priceInfo = nazhumiPriceService.getTldPrices(tld)
                _uiState.update {
                    it.copy(
                        isFetchingPrice = false,
                        priceList = priceInfo.prices,
                        priceMessage = "${priceInfo.prices.size} prices (.${tld})"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isFetchingPrice = false, priceMessage = "Price query failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Apply a specific registrar's pricing
     */
    fun applyPrice(price: DomainPrice) {
        _uiState.update {
            it.copy(
                renewPrice = price.renewPrice.toString(),
                currency = price.currency
            )
        }
    }

    /**
     * Evaluate domain value
     */
    fun evaluateValue() {
        val state = _uiState.value
        if (state.domainName.isBlank()) return

        val result = valuationEngine.evaluate(
            domainName = state.domainName,
            tld = state.tld,
            renewPrice = state.renewPrice.toDoubleOrNull() ?: 0.0,
            purchasePrice = state.purchasePrice.toDoubleOrNull() ?: 0.0,
            registrationDateStr = state.registerDate.ifBlank { null },
            hasWhoisPrivacy = state.privacyProtection,
            autoRenew = state.autoRenew
        )
        _uiState.update { it.copy(valuation = result) }
    }

    fun save() {
        val state = _uiState.value

        if (state.domainName.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a domain") }
            return
        }
        if (state.expireDate.isBlank()) {
            _uiState.update { it.copy(error = "Please select expiry date") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Evaluate value before saving
                val valuation = valuationEngine.evaluate(
                    domainName = state.domainName,
                    tld = state.tld,
                    renewPrice = state.renewPrice.toDoubleOrNull() ?: 0.0,
                    purchasePrice = state.purchasePrice.toDoubleOrNull() ?: 0.0,
                    registrationDateStr = state.registerDate.ifBlank { null },
                    hasWhoisPrivacy = state.privacyProtection,
                    autoRenew = state.autoRenew
                )

                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val entity = DomainEntity(
                    id = domainId ?: 0,
                    domainName = state.domainName.trim(),
                    tld = state.tld.trim().ifBlank {
                        val parts = state.domainName.split(".")
                        if (parts.size >= 2) ".${parts.last()}" else ".com"
                    },
                    registrar = state.registrar.trim().ifBlank { null },
                    registerDate = state.registerDate.ifBlank { null },
                    expireDate = state.expireDate.trim(),
                    autoRenew = state.autoRenew,
                    status = state.status,
                    purchasePrice = state.purchasePrice.toDoubleOrNull() ?: 0.0,
                    renewPrice = state.renewPrice.toDoubleOrNull() ?: 0.0,
                    holdCost = state.holdCost.toDoubleOrNull() ?: 0.0,
                    currency = state.currency,
                    dnsProvider = state.dnsProvider.ifBlank { null },
                    privacyProtection = state.privacyProtection,
                    usageType = state.usageType.ifBlank { null },
                    note = state.note.ifBlank { null },
                    nameservers = state.nameservers.ifBlank { null },
                    estimatedValue = valuation.estimatedValue,
                    valueCurrency = valuation.currency,
                    valueGrade = valuation.grade,
                    valueConfidence = valuation.confidence,
                    createdAt = now,
                    updatedAt = now
                )

                if (domainId != null) {
                    domainRepository.update(entity)
                } else {
                    domainRepository.insert(entity)
                }
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (e.message?.contains("UNIQUE") == true) "Domain already exists"
                            else "Save failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun inferDnsProvider(nameservers: List<String>): String {
        val first = nameservers.firstOrNull()?.lowercase() ?: return ""
        return when {
            first.contains("cloudflare") -> "Cloudflare"
            first.contains("dnspod") -> "DNSPod"
            first.contains("alidns") || first.contains("hichina") -> "Aliyun"
            first.contains("awsdns") -> "AWS Route53"
            first.contains("google") -> "Google Cloud DNS"
            first.contains("ns.vercel") -> "Vercel"
            first.contains("dnsv5") -> "DNSPod"
            else -> ""
        }
    }

    private fun mapRdapStatus(statuses: List<String>): String {
        return when {
            statuses.any { it.contains("redemption") } -> "expired"
            statuses.any { it.contains("pending delete") } -> "expired"
            statuses.any { it.contains("transfer") && it.contains("prohibited") } -> "active"
            statuses.any { it.contains("active") } -> "active"
            else -> "active"
        }
    }
}
