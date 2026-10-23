package co.barbatech.otter.resultgrid.sort

/** Sort applied to the loaded rows; [ascending] false means descending. */
data class GridSort(
  val columnIndex: Int,
  val ascending: Boolean,
)
