package co.barbatech.otter.queryhistory.state

import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.model.HistorySettings
import co.barbatech.otter.queryhistory.store.HistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state of the history panel. [onReopen] puts an entry's SQL into the editor. */
class HistoryState(
  private val store: HistoryStore,
  private val onReopen: (String) -> Unit,
  private val scope: CoroutineScope,
) {
  private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
  val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

  private val _query = MutableStateFlow("")
  val query: StateFlow<String> = _query.asStateFlow()

  val settings: StateFlow<HistorySettings> = store.settings
  val error: StateFlow<String?> = store.lastError

  init {
    scope.launch {
      store.load()
      _entries.value = store.search("", Int.MAX_VALUE)
    }
  }

  fun search(query: String) {
    _query.value = query
    reload(query)
  }

  /** Reloads the entries for the current query, e.g. after new statements were recorded. */
  fun refresh() {
    reload(_query.value)
  }

  fun setEnabled(enabled: Boolean) {
    scope.launch {
      store.load()
      store.updateSettings(store.settings.value.copy(enabled = enabled))
      _entries.value = store.search(_query.value, store.settings.value.maxEntries)
    }
  }

  fun reopen(entry: HistoryEntry) = onReopen(entry.sql)

  fun clear() {
    scope.launch {
      store.clear()
      _entries.value = emptyList()
    }
  }

  private fun reload(query: String) {
    scope.launch { _entries.value = store.search(query, store.settings.value.maxEntries) }
  }
}
