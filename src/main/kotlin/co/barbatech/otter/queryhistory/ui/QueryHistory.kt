package co.barbatech.otter.queryhistory.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.barbatech.otter.queryhistory.state.HistoryState

/** Searchable, persistent query history with reopen, clear and a recording switch. */
@Composable
fun QueryHistory(
  state: HistoryState,
  modifier: Modifier = Modifier,
) {
  val entries by state.entries.collectAsState()
  val settings by state.settings.collectAsState()
  val query by state.query.collectAsState()
  val error by state.error.collectAsState()
  var confirmClear by remember { mutableStateOf(false) }

  Column(modifier.padding(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = query,
        onValueChange = state::search,
        label = { Text("Search history") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(8.dp))
      Switch(checked = settings.enabled, onCheckedChange = state::setEnabled)
      Text(if (settings.enabled) "Recording on" else "Recording off")
      Spacer(Modifier.width(8.dp))
      TextButton(onClick = { confirmClear = true }, enabled = entries.isNotEmpty()) { Text("Clear") }
    }
    error?.let {
      Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp))
    }
    if (entries.isEmpty()) {
      Text("No history entries.", modifier = Modifier.padding(8.dp))
    } else {
      LazyColumn(Modifier.fillMaxSize()) {
        items(entries) { entry ->
          HistoryRow(entry, onReopen = state::reopen)
        }
      }
    }
  }

  if (confirmClear) {
    AlertDialog(
      onDismissRequest = { confirmClear = false },
      title = { Text("Clear history") },
      text = { Text("Delete all recorded statements? This cannot be undone.") },
      confirmButton = {
        TextButton(
          onClick = {
            state.clear()
            confirmClear = false
          },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text("Clear") }
      },
      dismissButton = {
        TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
      },
    )
  }
}
