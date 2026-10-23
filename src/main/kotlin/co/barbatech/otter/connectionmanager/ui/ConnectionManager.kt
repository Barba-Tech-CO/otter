package co.barbatech.otter.connectionmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import co.barbatech.otter.connectionmanager.state.ConnectionManagerState
import co.barbatech.otter.connectionstore.ConnectionConfig

/** Connection list with create, edit, duplicate, delete, test, open and close actions. */
@Composable
fun ConnectionManager(
  state: ConnectionManagerState,
  modifier: Modifier = Modifier,
) {
  val connections by state.connections.collectAsState()
  val activeSession by state.activeSession.collectAsState()
  val openIds by state.openIds.collectAsState()
  val busyIds by state.busyIds.collectAsState()
  val status by state.status.collectAsState()
  val passwordRequest by state.passwordRequest.collectAsState()

  var selectedId by remember { mutableStateOf<String?>(null) }
  var editing by remember { mutableStateOf<ConnectionConfig?>(null) }
  var showNewForm by remember { mutableStateOf(false) }
  var deleting by remember { mutableStateOf<ConnectionConfig?>(null) }

  Column(modifier.padding(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        "Connections",
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.titleSmall,
      )
      TextButton(onClick = { showNewForm = true }) { Text("New") }
    }
    status?.let { message ->
      Text(
        message.text,
        modifier = Modifier.padding(bottom = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (connections.isEmpty()) {
      Text(
        "No saved connections yet.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      items(connections, key = { it.id }) { config ->
        ConnectionRow(
          config = config,
          selected = selectedId == config.id,
          isOpen = config.id in openIds,
          isActive = activeSession?.config?.id == config.id,
          busy = config.id in busyIds,
          onSelect = { selectedId = config.id },
          onOpen = {
            selectedId = config.id
            state.open(config.id)
          },
          onClose = { state.close(config.id) },
          onEdit = { editing = config },
          onDuplicate = { state.duplicate(config.id) },
          onDelete = { deleting = config },
        )
      }
    }
  }

  if (showNewForm) {
    ConnectionFormDialog(state = state, initial = null, onDismiss = { showNewForm = false })
  }
  editing?.let { config ->
    ConnectionFormDialog(state = state, initial = config, onDismiss = { editing = null })
  }
  deleting?.let { config ->
    AlertDialog(
      onDismissRequest = { deleting = null },
      title = { Text("Delete connection") },
      text = {
        Text("Delete '${config.name}'? Its saved password is removed from the OS keychain as well.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            state.delete(config.id)
            deleting = null
          },
        ) { Text("Delete") }
      },
      dismissButton = {
        TextButton(onClick = { deleting = null }) { Text("Cancel") }
      },
    )
  }
  passwordRequest?.let { request ->
    PasswordPromptDialog(
      request = request,
      onConfirm = state::continueWithPassword,
      onCancel = state::cancelPasswordPrompt,
    )
  }
}
