package co.barbatech.otter.resultgrid.state

/** Rectangular range of cells; [anchor] is where the selection started, [focus] where it ends. */
data class GridSelection(
  val anchor: CellPosition,
  val focus: CellPosition,
) {
  val firstRow: Int get() = minOf(anchor.row, focus.row)

  val lastRow: Int get() = maxOf(anchor.row, focus.row)

  val firstColumn: Int get() = minOf(anchor.column, focus.column)

  val lastColumn: Int get() = maxOf(anchor.column, focus.column)

  fun contains(
    row: Int,
    column: Int,
  ): Boolean = row in firstRow..lastRow && column in firstColumn..lastColumn
}
