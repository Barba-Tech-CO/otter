package co.barbatech.otter.resultgrid.ui.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.awt.datatransfer.StringSelection

/** Shows a cell value in full, with a copy action. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CellDetailDialog(
  detail: CellDetail,
  onDismiss: () -> Unit,
) {
  val clipboard = LocalClipboard.current
  val clipboardScope = rememberCoroutineScope()
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(detail.columnName) },
    text = {
      Box(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
        Text(
          text = if (detail.isNull) "NULL" else detail.text,
          fontStyle = if (detail.isNull) FontStyle.Italic else FontStyle.Normal,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          clipboardScope.launch { clipboard.setClipEntry(ClipEntry(StringSelection(detail.text))) }
          onDismiss()
        },
      ) { Text("Copy") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}
