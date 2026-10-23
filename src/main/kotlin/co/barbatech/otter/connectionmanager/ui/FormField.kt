package co.barbatech.otter.connectionmanager.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
internal fun FormField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  showErrors: Boolean,
  error: String?,
  modifier: Modifier = Modifier,
  password: Boolean = false,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    modifier = modifier.fillMaxWidth(),
    singleLine = true,
    isError = showErrors && error != null,
    supportingText =
      if (showErrors && error != null) {
        { Text(error) }
      } else {
        null
      },
    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
  )
}
