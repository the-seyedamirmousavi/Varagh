package com.mid.varagh.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Brand spacing on a 4dp grid, shared with FitSho, 504 Daily and Timeboxing. */
object VaraghSpacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 20.dp
    val XXLarge = 24.dp
    val XXXLarge = 32.dp

    /** Space between the screen edge and content. */
    val ScreenGutter = Large

    /** Vertical gap between stacked cards. */
    val CardGap = Medium

    /** Padding inside a card. */
    val CardPadding = XLarge
}

/** Brand component sizes. */
object VaraghDimens {
    /** Height of a primary (pill) button. */
    val ButtonHeight = 56.dp

    /** Minimum height of a filled text field. */
    val FieldMinHeight = 52.dp

    /** Size of the rounded icon tile in feature rows. */
    val IconTile = 52.dp

    /** Soft shadow under the white top bar and the floating bottom bar. */
    val BarElevation = 3.dp
}
