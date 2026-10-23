package co.barbatech.otter.connectionmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.barbatech.otter.connectionmanager.helper.ConnectionFileChooserHelper
import co.barbatech.otter.connectionmanager.state.ConnectionFormState
import co.barbatech.otter.connectionmanager.state.ConnectionManagerState
import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionType
import java.util.UUID

/** Create or edit form. [initial] null means a new connection. */
@Composable
internal fun ConnectionFormDialog(
  state: ConnectionManagerState,
  initial: ConnectionConfig?,
  onDismiss: () -> Unit,
) {
  val testResult by state.testResult.collectAsState()
  val busyIds by state.busyIds.collectAsState()
  val form = remember(initial) { ConnectionFormState(initial) }
  val configId = remember(initial) { initial?.id ?: UUID.randomUUID().toString() }
  var showErrors by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { state.clearTestResult() }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "New connection" else "Edit connection") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        TypeField(form)
        FormField("Name", form.name, { form.name = it }, showErrors, form.nameError)
        when (form.type) {
          ConnectionType.Sqlite -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
              FormField(
                label = "File path",
                value = form.filePath,
                onValueChange = { form.filePath = it },
                showErrors = showErrors,
                error = form.filePathError,
                modifier = Modifier.weight(1f),
              )
              TextButton(
                onClick = { ConnectionFileChooserHelper.chooseSqliteFile()?.let { form.filePath = it } },
              ) { Text("Browse...") }
            }
          }

          ConnectionType.Postgres -> {
            Row {
              FormField(
                label = "Host",
                value = form.host,
                onValueChange = { form.host = it },
                showErrors = showErrors,
                error = form.hostError,
                modifier = Modifier.weight(2f),
              )
              FormField(
                label = "Port",
                value = form.port,
                onValueChange = { form.port = it },
                showErrors = showErrors,
                error = form.portError,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
              )
            }
            FormField("Database", form.database, { form.database = it }, showErrors, form.databaseError)
            FormField("User", form.user, { form.user = it }, showErrors, form.userError)
            FormField(
              label = "Password",
              value = form.password,
              onValueChange = { form.password = it },
              showErrors = false,
              error = null,
              password = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = form.savePassword, onCheckedChange = { form.savePassword = it })
              Text("Save password in the OS keychain", style = MaterialTheme.typography.bodySmall)
            }
            if (initial != null) {
              Text(
                "Leave the password blank to keep the saved one.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        testResult?.let { result ->
          Text(
            result.message,
            style = MaterialTheme.typography.bodySmall,
            color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
          )
        }
        if (showErrors && !form.isValid) {
          Text(
            "Fix the highlighted fields before saving.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          showErrors = true
          if (form.isValid) {
            state.save(form.toConfig(configId), form.password.takeIf { it.isNotEmpty() })
            onDismiss()
          }
        },
      ) { Text("Save") }
    },
    dismissButton = {
      Row {
        TextButton(
          onClick = {
            state.test(form.toConfig(configId), form.password.takeIf { it.isNotEmpty() })
          },
          enabled = !busyIds.contains(configId),
        ) { Text("Test connection") }
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    },
  )
}
