package io.unmi.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.unmi.app.data.local.db.entity.DomainEntity
import androidx.compose.ui.res.stringResource
import io.unmi.app.R
import io.unmi.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun DomainListItem(
    domain: DomainEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysRemaining = try {
        val expireDate = LocalDate.parse(domain.expireDate, DateTimeFormatter.ISO_LOCAL_DATE)
        ChronoUnit.DAYS.between(LocalDate.now(), expireDate).toInt()
    } catch (e: Exception) { -1 }

    val statusColor = when {
        daysRemaining < 0 -> DangerRed
        daysRemaining <= 7 -> DangerRed
        daysRemaining <= 30 -> WarningOrange
        else -> SafeGreen
    }

    val statusBgColor = when {
        daysRemaining < 0 -> DangerRedLight
        daysRemaining <= 7 -> DangerRedLight
        daysRemaining <= 30 -> WarningOrangeLight
        else -> SafeGreenLight
    }

    val statusText = when {
        daysRemaining < 0 -> stringResource(R.string.detail_days_expired)
        daysRemaining == 0 -> stringResource(R.string.detail_today_expire)
        else -> stringResource(R.string.detail_days_remaining, daysRemaining)
    }

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Domain info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = domain.domainName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    domain.registrar?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text("·", color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = domain.expireDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (domain.renewPrice > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${domain.currency} ${String.format("%.2f", domain.renewPrice)}${stringResource(R.string.per_year)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusBgColor
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
