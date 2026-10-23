package co.barbatech.otter.connectionmanager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.barbatech.otter.connectionmanager.state.ConnectionFormState
import co.barbatech.otter.connectionstore.ConnectionType

@Composable
internal fun TypeField(form: ConnectionFormState) {
  var expanded by remember { mutableStateOf(false) }
  Column {
    Text("Type", style = MaterialTheme.typography.labelSmall)
    Box {
      OutlinedButton(onClick = { expanded = true }) { Text(form.type.displayName) }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        ConnectionType.entries.forEach { type ->
          DropdownMenuItem(
            text = { Text(type.displayName) },
            onClick = {
              form.changeType(type)
              expanded = false
            },
          )
        }
      }
    }
  }
}
