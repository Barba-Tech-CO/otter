package co.barbatech.otter.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Three areas: sidebar (connections and schema), editor over results or history, status bar. */
@Composable
fun App(state: AppState) {
  OtterTheme {
    Surface(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f)) {
          Sidebar(state, Modifier.width(300.dp).fillMaxHeight())
          VerticalDivider()
          Workspace(state, Modifier.weight(1f).fillMaxHeight())
        }
        HorizontalDivider()
        StatusBar(state)
      }
    }
  }
}
