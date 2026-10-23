package co.barbatech.otter.resultgrid.ui.tab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.resultgrid.state.ResultGridState
import co.barbatech.otter.resultgrid.state.ResultTabView
import co.barbatech.otter.resultgrid.ui.table.ResultTable

@Composable
internal fun ResultTabContent(
  state: ResultGridState,
  view: ResultTabView,
  modifier: Modifier,
) {
  val error = view.tab.result.exceptionOrNull()
  if (error != null) {
    ErrorContent(error, modifier)
    return
  }
  when (val result = view.tab.result.getOrNull()) {
    is QueryResult.Affected -> {
      Box(modifier.fillMaxSize().padding(16.dp)) {
        Text("${result.rowCount} rows affected")
      }
    }

    is QueryResult.RowSet -> {
      ResultTable(state, view, modifier)
    }

    null -> {
      Box(modifier.fillMaxSize().padding(16.dp)) {
        Text("No result")
      }
    }
  }
}
