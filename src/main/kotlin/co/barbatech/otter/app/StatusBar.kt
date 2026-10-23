package co.barbatech.otter.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatusBar(
  state: AppState,
  modifier: Modifier = Modifier,
) {
  val session by state.connectionManager.activeSession.collectAsState()
  val resultStatus by state.resultGrid.statusText.collectAsState()
  Row(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
    Text(session?.config?.name ?: "Not connected", style = MaterialTheme.typography.labelSmall)
    Text("   ", style = MaterialTheme.typography.labelSmall)
    Text(resultStatus, style = MaterialTheme.typography.labelSmall)
  }
}
