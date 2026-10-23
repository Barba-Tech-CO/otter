package co.barbatech.otter.resultgrid.ui.tab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.barbatech.otter.queryexport.ui.ExportDialog
import co.barbatech.otter.resultgrid.state.ResultGridState

/** Result tabs plus the grid of the selected tab. */
@Composable
fun ResultGrid(
  state: ResultGridState,
  modifier: Modifier = Modifier,
  defaultFileName: String = "result",
) {
  val tabs by state.tabs.collectAsState()
  val selectedIndex by state.selectedIndex.collectAsState()
  var exportOpen by remember { mutableStateOf(false) }

  Column(modifier) {
    if (tabs.isEmpty()) {
      Box(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Run a query to see results.")
      }
    } else {
      ResultTabs(state, tabs, selectedIndex, onExport = { exportOpen = true })
      HorizontalDivider()
      val view = state.selectedView()
      if (view != null) {
        ResultTabContent(state, view, Modifier.weight(1f))
      }
    }
  }

  if (exportOpen) {
    ExportDialog(
      state = state.export,
      onDismiss = { exportOpen = false },
      rerun = state.selectedView()?.tab?.rerun,
      defaultFileName = defaultFileName,
    )
  }
}
