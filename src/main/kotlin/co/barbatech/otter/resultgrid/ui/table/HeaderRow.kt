package co.barbatech.otter.resultgrid.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.resultgrid.sort.GridSort

@Composable
internal fun HeaderRow(
  columns: List<ResultColumn>,
  widths: List<Dp>,
  sort: GridSort?,
  onSort: (Int) -> Unit,
) {
  Row(
    Modifier
      .fillMaxWidth()
      .height(28.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant),
  ) {
    columns.forEachIndexed { index, column ->
      val indicator =
        when {
          sort?.columnIndex != index -> ""
          sort.ascending -> " \u25b2"
          else -> " \u25bc"
        }
      Box(
        Modifier
          .width(widths.getOrElse(index) { GridMetrics.minColumnWidth })
          .fillMaxHeight()
          .clickable { onSort(index) }
          .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          column.name + indicator,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}
