package co.barbatech.otter.queryhistory.store

import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.model.HistorySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Non persistent store used until the file backed store exists, and in tests. */
class InMemoryHistoryStore : HistoryStore {
  private val entries = ArrayDeque<HistoryEntry>()
  private val _settings = MutableStateFlow(HistorySettings())
  override val settings: StateFlow<HistorySettings> = _settings.asStateFlow()

  private val _lastError = MutableStateFlow<String?>(null)
  override val lastError: StateFlow<String?> = _lastError.asStateFlow()

  override suspend fun updateSettings(settings: HistorySettings) {
    _settings.value = settings
    trim()
  }

  override suspend fun record(entry: HistoryEntry) {
    if (!_settings.value.enabled) return
    entries.addFirst(entry)
    trim()
  }

  override suspend fun search(
    query: String,
    limit: Int,
  ): List<HistoryEntry> = HistoryEntriesHelper.matching(entries, query, limit)

  override suspend fun clear() {
    entries.clear()
  }

  private fun trim() {
    HistoryEntriesHelper.trim(entries, _settings.value.maxEntries)
  }
}
