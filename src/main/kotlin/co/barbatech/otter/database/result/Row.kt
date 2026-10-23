package co.barbatech.otter.database.result

/** One result row; [cells] follow the column order of the cursor. */
data class Row(
  val cells: List<CellValue>,
)
