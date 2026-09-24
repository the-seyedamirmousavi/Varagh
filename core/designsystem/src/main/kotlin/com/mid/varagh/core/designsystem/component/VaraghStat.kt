package com.mid.varagh.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Bold value over a grey caption, e.g. "188.0 cm" / "Height". Read as one item by TalkBack. */
@Composable
fun VaraghStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.titleLarge,
) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = valueStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Like [VaraghStat] but the number counts up (locale digits), e.g. "۳٬۴۸۰" / "صفحه". */
@Composable
fun VaraghCountStat(
    target: Long,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String = "",
    valueStyle: TextStyle = MaterialTheme.typography.headlineMedium,
) {
    val locale = currentLocale()
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label: ${formatNumber(target, locale)}$suffix" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedCount(target = target, style = valueStyle, suffix = suffix)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Evenly spaced row of [VaraghStat]s. */
@Composable
fun VaraghStatRow(
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        stats.forEach { (value, label) ->
            VaraghStat(value = value, label = label, modifier = Modifier.weight(1f))
        }
    }
}
