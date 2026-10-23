package co.barbatech.otter.resultgrid.sort

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.resultgrid.format.GridFormattingHelper

/** Sorts the loaded rows and compares cell values for the grid. */
internal object GridSortingHelper {
  /** Sorts only the loaded rows; NULL values stay last in both directions. */
  fun sortRows(
    rows: List<Row>,
    columnIndex: Int,
    ascending: Boolean,
  ): List<Row> {
    val byValue =
      Comparator<Row> { first, second ->
        val firstValue = first.cells.getOrNull(columnIndex)
        val secondValue = second.cells.getOrNull(columnIndex)
        val firstIsNull = GridFormattingHelper.isNullCell(firstValue)
        val secondIsNull = GridFormattingHelper.isNullCell(secondValue)
        when {
          firstIsNull && secondIsNull -> 0
          firstIsNull -> 1
          secondIsNull -> -1
          else -> compareCellValues(firstValue!!, secondValue!!)
        }
      }
    if (ascending) return rows.sortedWith(byValue)
    val descendingNullsLast =
      Comparator<Row> { first, second ->
        val comparison = byValue.compare(first, second)
        val touchesNull =
          GridFormattingHelper.isNullCell(first.cells.getOrNull(columnIndex)) ||
            GridFormattingHelper.isNullCell(second.cells.getOrNull(columnIndex))
        if (comparison == 0 || touchesNull) comparison else -comparison
      }
    return rows.sortedWith(descendingNullsLast)
  }

  /** Numeric, boolean and text values compare naturally; mixed types compare by their display text. */
  fun compareCellValues(
    first: CellValue,
    second: CellValue,
  ): Int =
    when {
      first is CellValue.Numeric && second is CellValue.Numeric -> first.value.compareTo(second.value)
      first is CellValue.Bool && second is CellValue.Bool -> first.value.compareTo(second.value)
      first is CellValue.Text && second is CellValue.Text -> first.value.compareTo(second.value)
      else -> GridFormattingHelper.cellText(first).compareTo(GridFormattingHelper.cellText(second))
    }
}
