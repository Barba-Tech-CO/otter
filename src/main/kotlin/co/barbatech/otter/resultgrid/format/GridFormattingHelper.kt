package co.barbatech.otter.resultgrid.format

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.resultgrid.state.ResultTab
import java.time.Duration
import java.util.Locale

/** Text formatting shared by the grid, its tabs and the status bar. */
internal object GridFormattingHelper {
  /** Title of a result tab: result number, affected rows or error. */
  fun tabTitle(
    index: Int,
    tab: ResultTab,
  ): String {
    if (tab.result.isFailure) return "Error ${index + 1}"
    return when (val result = tab.result.getOrNull()) {
      is QueryResult.Affected -> "${result.rowCount} rows affected"
      is QueryResult.RowSet -> "Result ${index + 1}"
      null -> "Result ${index + 1}"
    }
  }

  /** Text shown in a cell; binary values become a short placeholder with their size. */
  fun cellText(value: CellValue?): String =
    when (value) {
      null, CellValue.Null -> "NULL"
      is CellValue.Text -> value.value
      is CellValue.Numeric -> value.value.toPlainString()
      is CellValue.Bool -> value.value.toString()
      is CellValue.Binary -> "<BLOB ${value.bytes.size} bytes>"
    }

  fun isNullCell(value: CellValue?): Boolean = value == null || value is CellValue.Null

  /** Truncates long values in the cell; the detail dialog shows the full text. */
  fun truncateForCell(
    text: String,
    maxChars: Int = 120,
  ): String = if (text.length <= maxChars) text else text.take(maxChars) + "\u2026"

  /** Column widths in characters, taken from the header and a sample of the first rows. */
  fun columnCharWidths(
    columns: List<ResultColumn>,
    rows: List<Row>,
    maxChars: Int = 40,
  ): List<Int> {
    val sample = rows.take(50)
    return columns.mapIndexed { index, column ->
      val content = sample.maxOfOrNull { row -> cellText(row.cells.getOrNull(index)).length } ?: 0
      maxOf(column.name.length, content).coerceIn(4, maxChars)
    }
  }

  /** Status line: loaded rows, elapsed time and whether more rows are available. */
  fun formatStatusText(
    rowsLoaded: Int,
    duration: Duration,
    hasMore: Boolean,
    sortedLoadedOnly: Boolean,
  ): String {
    val parts = mutableListOf("$rowsLoaded rows loaded", formatDuration(duration))
    parts += if (hasMore) "more available" else "end of result"
    if (sortedLoadedOnly) parts += "sorted loaded rows only"
    return parts.joinToString(" - ")
  }

  fun formatDuration(duration: Duration): String {
    val millis = duration.toMillis()
    return if (millis < 1000) "$millis ms" else String.format(Locale.ROOT, "%.2f s", millis / 1000.0)
  }
}
