package co.barbatech.otter.resultgrid.clipboard

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.resultgrid.state.GridSelection

/** Tab separated text for the grid clipboard actions. */
internal object GridClipboardHelper {
  /** Clipboard text of one value; SQL NULL becomes empty text so it never looks like the string "NULL". */
  fun clipboardText(value: CellValue?): String = value?.textOrNull() ?: ""

  /** Escapes a tab separated field; tabs, newlines and quotes make the RFC 4180 quoting kick in. */
  fun escapeTsvField(value: String): String =
    if (value.any { it == '\t' || it == '\n' || it == '\r' || it == '"' }) {
      "\"" + value.replace("\"", "\"\"") + "\""
    } else {
      value
    }

  /** Tab separated text of the selected rectangle; rows are joined by newlines. */
  fun formatRowsForClipboard(
    rows: List<Row>,
    columns: List<ResultColumn>,
    selection: GridSelection,
    includeHeaders: Boolean,
  ): String {
    if (rows.isEmpty() || columns.isEmpty()) return ""
    val firstRow = selection.firstRow.coerceIn(0, rows.lastIndex)
    val lastRow = selection.lastRow.coerceIn(0, rows.lastIndex)
    val firstColumn = selection.firstColumn.coerceIn(0, columns.lastIndex)
    val lastColumn = selection.lastColumn.coerceIn(0, columns.lastIndex)
    val lines = mutableListOf<String>()
    if (includeHeaders) {
      lines += (firstColumn..lastColumn).joinToString("\t") { escapeTsvField(columns[it].name) }
    }
    for (rowIndex in firstRow..lastRow) {
      val cells = rows[rowIndex].cells
      lines +=
        (firstColumn..lastColumn).joinToString("\t") { column ->
          escapeTsvField(clipboardText(cells.getOrNull(column)))
        }
    }
    return lines.joinToString("\n")
  }
}
