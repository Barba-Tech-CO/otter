package co.barbatech.otter.queryhistory.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.barbatech.otter.queryhistory.model.HistoryEntry

@Composable
internal fun HistoryRow(
  entry: HistoryEntry,
  onReopen: (HistoryEntry) -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        "${entry.connectionName} - ${HistoryEntryFormatHelper.timestamp(entry.executedAt)} - " +
          "${entry.duration.toMillis()} ms - ${if (entry.success) "success" else "failure"}",
        style = MaterialTheme.typography.labelMedium,
      )
      Text(
        HistoryEntryFormatHelper.sqlPreview(entry.sql),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      entry.error?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    TextButton(onClick = { onReopen(entry) }) { Text("Reopen") }
  }
  HorizontalDivider()
}
