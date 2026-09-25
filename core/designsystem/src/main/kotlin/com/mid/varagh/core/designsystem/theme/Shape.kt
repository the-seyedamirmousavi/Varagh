package com.mid.varagh.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Brand corner radii, shared with FitSho, 504 Daily and Timeboxing:
 * 12dp inputs and icon tiles, 20dp cards and hero banners, 28dp sheets and dialogs.
 * Buttons and chips are pills ([VaraghPillShape]).
 */
val VaraghShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Fully rounded ends, for buttons, chips and the floating bottom bar's selection. */
val VaraghPillShape = RoundedCornerShape(percent = 50)
