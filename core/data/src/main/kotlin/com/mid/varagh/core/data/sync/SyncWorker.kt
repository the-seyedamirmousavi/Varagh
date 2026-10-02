package com.mid.varagh.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mid.varagh.core.domain.VaraghException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background sync with the server (remote backend only). Network problems and server errors are
 * retried with exponential backoff; an expired session stops until the user signs in again.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val engine: SyncEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        engine.sync()
        Result.success()
    } catch (e: VaraghException.Unauthorized) {
        Result.failure()
    } catch (e: VaraghException.Network) {
        Result.retry()
    } catch (e: VaraghException) {
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
