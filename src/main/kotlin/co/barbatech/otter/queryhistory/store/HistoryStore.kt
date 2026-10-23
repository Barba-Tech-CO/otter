package co.barbatech.otter.queryhistory.store

import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.model.HistorySettings
import kotlinx.coroutines.flow.StateFlow

/** Local only query history. [record] is a no-op while recording is disabled. */
interface HistoryStore {
  val settings: StateFlow<HistorySettings>

  /** Readable problem from the last load or save, or null. */
  val lastError: StateFlow<String?>

  /** Loads persisted data if needed; idempotent. */
  suspend fun load() = Unit

  suspend fun updateSettings(settings: HistorySettings)

  suspend fun record(entry: HistoryEntry)

  /** Newest first; [query] matches SQL text or connection name (blank matches all). */
  suspend fun search(
    query: String,
    limit: Int,
  ): List<HistoryEntry>

  suspend fun clear()
}
