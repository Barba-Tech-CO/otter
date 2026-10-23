package co.barbatech.otter.queryhistory.store

import co.barbatech.otter.queryhistory.model.HistoryEntry

/** Filtering and size rules shared by the history stores. */
internal object HistoryEntriesHelper {
  /** Newest first filtering by SQL text or connection name; blank matches all. */
  fun matching(
    entries: Iterable<HistoryEntry>,
    query: String,
    limit: Int,
  ): List<HistoryEntry> =
    entries
      .filter {
        query.isBlank() || it.sql.contains(query, ignoreCase = true) ||
          it.connectionName.contains(query, ignoreCase = true)
      }.take(limit.coerceAtLeast(0))

  fun trim(
    entries: ArrayDeque<HistoryEntry>,
    maxEntries: Int,
  ) {
    while (entries.size > maxEntries) entries.removeLast()
  }
}
