package com.mid.varagh.core.designsystem.component

import androidx.annotation.StringRes
import com.mid.varagh.core.designsystem.R
import com.mid.varagh.core.domain.VaraghException

/** User-facing message for any failure. Unknown errors get a generic, non-technical message. */
@StringRes
fun errorMessageRes(error: Throwable?): Int = when (error) {
    is VaraghException.Network -> R.string.error_network
    is VaraghException.Unauthorized -> R.string.error_unauthorized
    is VaraghException.FileUnavailable -> R.string.error_file_unavailable
    is VaraghException.InvalidFile -> R.string.error_invalid_file
    is VaraghException.DifferentFile -> R.string.error_different_file
    is VaraghException.InvalidBackup -> R.string.error_invalid_backup
    else -> R.string.error_generic
}
