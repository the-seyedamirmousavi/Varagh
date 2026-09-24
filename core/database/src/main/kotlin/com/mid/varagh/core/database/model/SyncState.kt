package com.mid.varagh.core.database.model

/**
 * Per-row sync bookkeeping, used by the remote backend's SyncWorker (phase 8).
 *
 * The local-only build writes [SYNCED] and hard-deletes rows. With the remote backend, edits are
 * written as [PENDING] and deletions as [DELETED] (tombstones) until the SyncWorker pushes them.
 * Every "observe" query hides [DELETED] rows.
 */
enum class SyncState {
    SYNCED,
    PENDING,
    DELETED,
}
