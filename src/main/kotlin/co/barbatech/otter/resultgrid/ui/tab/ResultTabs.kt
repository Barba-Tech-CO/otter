package co.barbatech.otter.resultgrid.ui.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.barbatech.otter.resultgrid.format.GridFormattingHelper
import co.barbatech.otter.resultgrid.state.ResultGridState
import co.barbatech.otter.resultgrid.state.ResultTab

@Composable
internal fun ResultTabs(
  state: ResultGridState,
  tabs: List<ResultTab>,
  selectedIndex: Int,
  onExport: () -> Unit,
) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    PrimaryScrollableTabRow(
      selectedTabIndex = selectedIndex.coerceIn(0, tabs.lastIndex),
      modifier = Modifier.weight(1f),
      edgePadding = 4.dp,
    ) {
      tabs.forEachIndexed { index, tab ->
        Tab(
          selected = index == selectedIndex,
          onClick = { state.select(index) },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(GridFormattingHelper.tabTitle(index, tab), maxLines = 1)
              Box(Modifier.padding(start = 6.dp).clickable { state.close(index) }) {
                Text("\u00d7")
              }
            }
          },
        )
      }
    }
    TextButton(onClick = onExport) { Text("Export") }
  }
}
