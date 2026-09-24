package com.mid.varagh.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.core.os.ConfigurationCompat
import com.mid.varagh.core.designsystem.motion.rememberReducedMotion
import java.text.NumberFormat
import java.util.Locale

/** Current UI locale, so numbers render as ۱۲۳ in Persian and 123 in English. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale =
    ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

/** Formats [value] with the UI locale's digits and grouping, e.g. "۳٬۴۸۰" / "3,480". */
fun formatNumber(value: Long, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)

/** A number that counts up from 0 the first time it appears, then animates between values. */
@Composable
fun AnimatedCount(
    target: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    suffix: String = "",
) {
    val reducedMotion = rememberReducedMotion()
    val animatable = remember { Animatable(if (reducedMotion) target.toFloat() else 0f) }
    LaunchedEffect(target) {
        if (reducedMotion) animatable.snapTo(target.toFloat())
        else animatable.animateTo(target.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    val locale = currentLocale()
    Text(text = formatNumber(animatable.value.toLong(), locale) + suffix, style = style, modifier = modifier)
}
