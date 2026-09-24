package com.mid.varagh.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mid.varagh.core.designsystem.component.VaraghAccentChip
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghCountStat
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.component.currentLocale
import com.mid.varagh.core.designsystem.component.formatNumber
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.motion.AppearAnimated

/** Phase-1 shell: the stats layout with empty (zero) values. Real data arrives in phase 5. */
@Composable
internal fun HistoryScreen(modifier: Modifier = Modifier) {
    val locale = currentLocale()
    Scaffold(
        modifier = modifier,
        topBar = { VaraghTopAppBar(title = stringResource(R.string.history_title)) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppearAnimated(index = 0) {
                VaraghCard(
                    title = stringResource(R.string.history_this_year),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    VaraghAccentChip(
                        text = stringResource(R.string.history_streak_days, formatNumber(0, locale)),
                        icon = VaraghIcons.Streak,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        VaraghCountStat(target = 0, label = stringResource(R.string.history_stat_books), modifier = Modifier.weight(1f))
                        VaraghCountStat(target = 0, label = stringResource(R.string.history_stat_pages), modifier = Modifier.weight(1f))
                        VaraghCountStat(target = 0, label = stringResource(R.string.history_stat_minutes), modifier = Modifier.weight(1f))
                    }
                }
            }
            AppearAnimated(index = 1) {
                VaraghCard(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.history_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.history_empty_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
