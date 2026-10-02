package com.mid.varagh

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * WorkManager is initialised on demand with Hilt's worker factory (the default initializer is
 * removed in the manifest), so the SyncWorker gets its dependencies injected. Nothing is scheduled
 * unless the remote backend is on.
 */
@HiltAndroidApp
class VaraghApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
