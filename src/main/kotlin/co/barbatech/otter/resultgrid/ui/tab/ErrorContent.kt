package co.barbatech.otter.resultgrid.ui.tab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.barbatech.otter.database.error.DatabaseException

@Composable
internal fun ErrorContent(
  error: Throwable,
  modifier: Modifier,
) {
  val databaseError = error as? DatabaseException
  Column(modifier.fillMaxSize().padding(16.dp)) {
    Text(
      "The statement failed.",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.error,
    )
    Text(error.message ?: "Unknown database error")
    databaseError?.position?.let { position -> Text("Position: $position") }
  }
}
