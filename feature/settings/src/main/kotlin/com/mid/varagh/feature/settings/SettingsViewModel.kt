package com.mid.varagh.feature.settings

import androidx.lifecycle.ViewModel
import com.mid.varagh.core.model.FeatureFlags
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val featureFlags: FeatureFlags,
) : ViewModel()
