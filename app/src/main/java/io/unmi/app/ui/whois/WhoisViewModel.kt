package io.unmi.app.ui.whois

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.unmi.app.R
import io.unmi.app.data.local.db.dao.WhoisQueryLogDao
import io.unmi.app.data.local.db.entity.WhoisQueryLogEntity
import io.unmi.app.data.network.RdapResult
import io.unmi.app.data.network.RdapService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class WhoisUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val rdapResult: RdapResult? = null,
    val rawResult: String? = null,
    val error: String? = null,
    val recentQueries: List<WhoisQueryLogEntity> = emptyList()
)

@HiltViewModel
class WhoisViewModel @Inject constructor(
    private val app: Application,
    private val whoisQueryLogDao: WhoisQueryLogDao,
    private val rdapService: RdapService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhoisUiState())
    val uiState: StateFlow<WhoisUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            whoisQueryLogDao.observeAll().collect { logs ->
                _uiState.update { it.copy(recentQueries = logs) }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun queryWhois(domainName: String? = null) {
        val domain = (domainName ?: _uiState.value.query).trim().lowercase()
        if (domain.isBlank()) {
            _uiState.update { it.copy(error = app.getString(R.string.whois_error_empty)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, rdapResult = null, rawResult = null) }
            try {
                val result = rdapService.queryDomain(domain)
                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                whoisQueryLogDao.insert(
                    WhoisQueryLogEntity(
                        domainName = domain,
                        queryStatus = "success",
                        provider = "RDAP/ICANN",
                        rawText = result.rawJson,
                        parsedResultJson = buildParsedSummary(result),
                        queriedAt = now
                    )
                )
                _uiState.update {
                    it.copy(isLoading = false, rdapResult = result, rawResult = result.rawJson)
                }
            } catch (e: Exception) {
                val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                whoisQueryLogDao.insert(
                    WhoisQueryLogEntity(
                        domainName = domain,
                        queryStatus = "error",
                        provider = "RDAP/ICANN",
                        errorMessage = e.message,
                        queriedAt = now
                    )
                )
                _uiState.update {
                    it.copy(isLoading = false, error = app.getString(R.string.whois_error_query, e.message ?: ""))
                }
            }
        }
    }

    private fun buildParsedSummary(result: RdapResult): String {
        return buildString {
            appendLine("Domain: ${result.domainName}")
            appendLine("Registrar: ${result.registrar ?: "Unknown"}")
            appendLine("Registered: ${result.registrationDate ?: "Unknown"}")
            appendLine("Expires: ${result.expirationDate ?: "Unknown"}")
            appendLine("Status: ${result.status.joinToString(", ")}")
            appendLine("NS: ${result.nameservers.joinToString(", ")}")
            appendLine("DNSSEC: ${if (result.dnssecSigned) "Signed" else "Unsigned"}")
        }
    }
}
