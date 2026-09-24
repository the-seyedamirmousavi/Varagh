package com.mid.varagh

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.mid.varagh.core.designsystem.theme.VaraghTheme
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.ui.VaraghApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity. Extends AppCompatActivity (a ComponentActivity) so per-app language switching
 * via AppCompatDelegate works on API < 33 as well.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var featureFlags: FeatureFlags

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VaraghTheme {
                VaraghApp(featureFlags = featureFlags)
            }
        }
    }
}
