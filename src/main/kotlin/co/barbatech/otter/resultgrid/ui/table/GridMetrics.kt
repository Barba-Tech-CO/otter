package co.barbatech.otter.resultgrid.ui.table

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared grid metrics and the character-to-width conversion used by the table. */
internal object GridMetrics {
  val rowHeight: Dp = 24.dp
  val minColumnWidth: Dp = 48.dp

  fun columnWidthDp(chars: Int): Dp = (chars * 7).dp.coerceIn(minColumnWidth, 320.dp)
}
