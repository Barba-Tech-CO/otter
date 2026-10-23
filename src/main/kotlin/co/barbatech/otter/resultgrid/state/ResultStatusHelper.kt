package co.barbatech.otter.resultgrid.state

import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.resultgrid.format.GridFormattingHelper

/** Status bar text describing the selected result tab. */
internal object ResultStatusHelper {
  fun statusFor(view: ResultTabView): String {
    view.tab.result.exceptionOrNull()?.let { error ->
      return "Error: ${error.message ?: "the statement failed"}"
    }
    return when (val result = view.tab.result.getOrNull()) {
      is QueryResult.Affected -> {
        "${result.rowCount} rows affected"
      }

      is QueryResult.RowSet -> {
        GridFormattingHelper.formatStatusText(
          rowsLoaded = view.rows.value.size,
          duration = view.tab.duration,
          hasMore = view.hasMore.value,
          sortedLoadedOnly = view.sort.value != null && view.hasMore.value,
        )
      }

      null -> {
        ""
      }
    }
  }
}
