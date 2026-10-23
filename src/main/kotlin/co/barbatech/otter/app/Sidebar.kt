package co.barbatech.otter.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.barbatech.otter.connectionmanager.ui.ConnectionManager
import co.barbatech.otter.schemabrowser.SchemaBrowser

@Composable
internal fun Sidebar(
  state: AppState,
  modifier: Modifier,
) {
  Column(modifier) {
    ConnectionManager(state.connectionManager, Modifier.weight(1f))
    HorizontalDivider()
    SchemaBrowser(state.schemaBrowser, Modifier.weight(2f))
  }
}
