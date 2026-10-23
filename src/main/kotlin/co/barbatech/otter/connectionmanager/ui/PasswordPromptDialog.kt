package co.barbatech.otter.connectionmanager.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import co.barbatech.otter.connectionmanager.state.PasswordRequest

/** Small dialog that asks for the password of [request] when none is saved in the keychain. */
@Composable
internal fun PasswordPromptDialog(
  request: PasswordRequest,
  onConfirm: (String) -> Unit,
  onCancel: () -> Unit,
) {
  var password by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onCancel,
    title = { Text("Password required") },
    text = {
      Column {
        Text(
          "Enter the password for '${request.connectionName}'.",
          style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password") },
          modifier = Modifier.padding(top = 8.dp),
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = { if (password.isNotEmpty()) onConfirm(password) }),
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(password) }, enabled = password.isNotEmpty()) { Text("Connect") }
    },
    dismissButton = {
      TextButton(onClick = onCancel) { Text("Cancel") }
    },
  )
}
