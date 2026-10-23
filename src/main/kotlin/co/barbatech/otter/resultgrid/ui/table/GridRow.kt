package co.barbatech.otter.resultgrid.ui.table

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.resultgrid.state.GridSelection
import co.barbatech.otter.resultgrid.state.ResultGridState
import co.barbatech.otter.database.result.Row as DataRow

@Composable
internal fun GridRow(
  state: ResultGridState,
  row: DataRow,
  rowIndex: Int,
  columns: List<ResultColumn>,
  widths: List<Dp>,
  selection: GridSelection?,
  onSelect: (Int, Int, Boolean) -> Unit,
  onOpenDetail: (Int, Int) -> Unit,
) {
  Row(Modifier.fillMaxWidth().height(GridMetrics.rowHeight)) {
    columns.forEachIndexed { columnIndex, _ ->
      GridCell(
        state = state,
        value = row.cells.getOrNull(columnIndex),
        rowIndex = rowIndex,
        columnIndex = columnIndex,
        width = widths.getOrElse(columnIndex) { GridMetrics.minColumnWidth },
        selected = selection?.contains(rowIndex, columnIndex) == true,
        onSelect = onSelect,
        onOpenDetail = onOpenDetail,
      )
    }
  }
}
