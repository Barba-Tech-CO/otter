package co.barbatech.otter.queryexport.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.queryexport.state.ExportState
import java.nio.file.Path

/**
 * Export dialog: format, CSV options, target file, progress, cancel and the final result.
 * [rerun] supplies "all rows" by re-running the query; [defaultFileName] is the suggested file name.
 */
@Composable
fun ExportDialog(
  state: ExportState,
  onDismiss: () -> Unit,
  rerun: (suspend () -> QueryResult)? = null,
  defaultFileName: String = "result",
) {
  val progress by state.progress.collectAsState()
  val error by state.error.collectAsState()
  val running by state.running.collectAsState()
  val message by state.message.collectAsState()

  var kind by remember { mutableStateOf(ExportKind.Csv) }
  var delimiter by remember { mutableStateOf(',') }
  var includeHeader by remember { mutableStateOf(true) }
  var target by remember { mutableStateOf<Path?>(null) }
  var delimiterMenuOpen by remember { mutableStateOf(false) }

  val suggested = ExportDialogHelper.suggestedPath(defaultFileName, kind)
  val selectedTarget = target ?: suggested

  AlertDialog(
    onDismissRequest = { if (!running) onDismiss() },
    title = { Text("Export result") },
    text = {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Format", modifier = Modifier.width(80.dp))
          RadioButton(selected = kind == ExportKind.Csv, onClick = { kind = ExportKind.Csv })
          Text(
            "CSV",
            modifier = Modifier.selectable(selected = kind == ExportKind.Csv, onClick = { kind = ExportKind.Csv }),
          )
          Spacer(Modifier.width(8.dp))
          RadioButton(selected = kind == ExportKind.Json, onClick = { kind = ExportKind.Json })
          Text(
            "JSON",
            modifier = Modifier.selectable(selected = kind == ExportKind.Json, onClick = { kind = ExportKind.Json }),
          )
        }
        if (kind == ExportKind.Csv) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text("Delimiter", modifier = Modifier.width(80.dp))
            TextButton(onClick = { delimiterMenuOpen = true }) {
              Text(ExportDialogHelper.delimiterLabel(delimiter))
            }
            DropdownMenu(expanded = delimiterMenuOpen, onDismissRequest = { delimiterMenuOpen = false }) {
              ExportDialogHelper.delimiters.forEach { (character, label) ->
                DropdownMenuItem(
                  text = { Text(label) },
                  onClick = {
                    delimiter = character
                    delimiterMenuOpen = false
                  },
                )
              }
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Header row", modifier = Modifier.width(80.dp))
            Switch(checked = includeHeader, onCheckedChange = { includeHeader = it })
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
          Text("File", modifier = Modifier.width(80.dp))
          Text(selectedTarget.toString(), modifier = Modifier.weight(1f))
        }
        TextButton(
          onClick = { ExportDialogHelper.chooseTarget(selectedTarget)?.let { target = it } },
          enabled = !running,
        ) {
          Text("Choose file...")
        }
        if (running) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
          Text("${progress?.rowsWritten ?: 0} rows written")
        }
        message?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
        if (rerun == null) {
          Text("Open this dialog from a result tab to enable export.", color = MaterialTheme.colorScheme.error)
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = rerun != null && !running,
        onClick = {
          val format = ExportDialogHelper.exportFormat(kind, delimiter, includeHeader)
          rerun?.let { state.start(it, format, selectedTarget) }
        },
      ) { Text("Export") }
    },
    dismissButton = {
      if (running) {
        TextButton(onClick = state::cancel) { Text("Cancel export") }
      } else {
        TextButton(onClick = onDismiss) { Text("Close") }
      }
    },
  )
}
