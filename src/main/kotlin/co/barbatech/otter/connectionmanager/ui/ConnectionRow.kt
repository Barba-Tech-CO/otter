package co.barbatech.otter.connectionmanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.barbatech.otter.connectionstore.ConnectionConfig

@Composable
internal fun ConnectionRow(
  config: ConnectionConfig,
  selected: Boolean,
  isOpen: Boolean,
  isActive: Boolean,
  busy: Boolean,
  onSelect: () -> Unit,
  onOpen: () -> Unit,
  onClose: () -> Unit,
  onEdit: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
) {
  var focused by remember { mutableStateOf(false) }
  val background =
    if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
  val focusBorder =
    if (focused) {
      Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), MaterialTheme.shapes.small)
    } else {
      Modifier
    }
  Column(
    Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.small)
      .background(background)
      .then(focusBorder)
      .onFocusChanged { focused = it.isFocused }
      .clickable(onClick = onSelect)
      .padding(horizontal = 6.dp, vertical = 4.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        config.name,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        config.type.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    val stateText =
      when {
        busy -> "working..."
        isActive -> "connected (active)"
        isOpen -> "connected"
        else -> ""
      }
    if (stateText.isNotEmpty()) {
      Text(
        stateText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (selected) {
      Row {
        CompactButton(if (isOpen) "Activate" else "Open", enabled = !busy, onClick = onOpen)
        if (isOpen) CompactButton("Close", enabled = !busy, onClick = onClose)
        CompactButton("Edit", enabled = !busy, onClick = onEdit)
        CompactButton("Copy", enabled = !busy, onClick = onDuplicate)
        CompactButton("Delete", enabled = !busy, onClick = onDelete)
      }
    }
  }
}
