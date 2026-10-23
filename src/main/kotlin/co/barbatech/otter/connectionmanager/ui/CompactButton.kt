package co.barbatech.otter.connectionmanager.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CompactButton(
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  TextButton(
    onClick = onClick,
    enabled = enabled,
    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp).height(28.dp),
  ) {
    Text(label, style = MaterialTheme.typography.labelSmall)
  }
}
