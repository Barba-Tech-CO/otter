package co.barbatech.otter.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import co.barbatech.otter.queryhistory.ui.QueryHistory
import co.barbatech.otter.resultgrid.ui.tab.ResultGrid
import co.barbatech.otter.sqleditor.SqlEditor

@Composable
internal fun Workspace(
  state: AppState,
  modifier: Modifier,
) {
  var selectedTab by remember { mutableStateOf(0) }
  val session by state.connectionManager.activeSession.collectAsState()
  Column(modifier) {
    SqlEditor(state.sqlEditor, Modifier.weight(1f))
    HorizontalDivider()
    PrimaryTabRow(selectedTabIndex = selectedTab) {
      listOf("Results", "History").forEachIndexed { index, title ->
        Tab(
          selected = selectedTab == index,
          onClick = {
            selectedTab = index
            if (index == 1) state.queryHistory.refresh()
          },
          text = { Text(title) },
        )
      }
    }
    if (selectedTab == 0) {
      ResultGrid(state.resultGrid, Modifier.weight(1f), session?.config?.name ?: "result")
    } else {
      QueryHistory(state.queryHistory, Modifier.weight(1f))
    }
  }
}
