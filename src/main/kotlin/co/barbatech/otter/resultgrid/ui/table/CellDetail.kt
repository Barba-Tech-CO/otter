package co.barbatech.otter.resultgrid.ui.table

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.resultgrid.format.GridFormattingHelper

/** Full value of one cell, shown in the detail dialog. */
internal data class CellDetail(
  val columnName: String,
  val value: CellValue?,
) {
  val text: String get() = GridFormattingHelper.cellText(value)

  val isNull: Boolean get() = GridFormattingHelper.isNullCell(value)
}
