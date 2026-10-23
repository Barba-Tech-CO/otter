package co.barbatech.otter.sqleditor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp

@Composable
fun SqlEditor(
  state: SqlEditorState,
  modifier: Modifier = Modifier,
) {
  val running by state.running.collectAsState()
  val text by state.text.collectAsState()
  val message by state.message.collectAsState()
  var findVisible by remember { mutableStateOf(false) }
  var searchText by remember { mutableStateOf("") }
  var matchCase by remember { mutableStateOf(false) }
  val editor =
    remember(state) {
      SwingSqlEditorControl(
        onTextChanged = state::setText,
        onRun = state::run,
        onCancel = state::cancel,
        onFind = { findVisible = true },
      )
    }

  LaunchedEffect(text) {
    if (editor.getText() != text) editor.setText(text)
  }
  editor.applyTheme(isSystemInDarkTheme())

  Column(modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(enabled = !running, onClick = { state.run(editor.getSelectedText()) }) { Text("Run") }
      Button(enabled = running, onClick = state::cancel) { Text("Cancel") }
      Text("Ctrl/Cmd+Enter to run · Esc to cancel", Modifier.padding(top = 10.dp))
      if (message.isNotEmpty()) Text(message, Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.error)
    }
    if (findVisible) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = searchText,
          onValueChange = { searchText = it },
          label = { Text("Find") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        Checkbox(checked = matchCase, onCheckedChange = { matchCase = it })
        Text("Match case")
        Button(onClick = { editor.findPrevious(searchText, matchCase) }) { Text("Previous") }
        Button(onClick = { editor.findNext(searchText, matchCase) }) { Text("Next") }
        Button(onClick = { findVisible = false }) { Text("Close") }
      }
    }
    SwingPanel(
      factory = { editor.component },
      modifier = Modifier.fillMaxWidth().weight(1f),
    )
  }
}
