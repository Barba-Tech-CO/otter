package co.barbatech.otter.resultgrid.ui.table

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class GridMetricsTest {
  @Test
  fun widthsAreClampedToTheMinimumAndMaximum() {
    assertEquals(GridMetrics.columnWidthDp(1), GridMetrics.minColumnWidth)
    assertEquals(70.dp, GridMetrics.columnWidthDp(10))
    assertEquals(GridMetrics.columnWidthDp(100), 320.dp)
  }

  @Test
  fun rowHeightMatchesTheRenderedRows() {
    assertEquals(24.dp, GridMetrics.rowHeight)
  }
}
