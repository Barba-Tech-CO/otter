package co.barbatech.otter.resultgrid.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.resultgrid.format.GridFormattingHelper
import co.barbatech.otter.resultgrid.state.ResultGridState
import kotlinx.coroutines.launch
import java.awt.datatransfer.StringSelection
import androidx.compose.ui.input.pointer.isCtrlPressed as pointerCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed as pointerMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed as pointerShiftPressed

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun GridCell(
  state: ResultGridState,
  value: CellValue?,
  rowIndex: Int,
  columnIndex: Int,
  width: Dp,
  selected: Boolean,
  onSelect: (Int, Int, Boolean) -> Unit,
  onOpenDetail: (Int, Int) -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }
  val clipboard = LocalClipboard.current
  val clipboardScope = rememberCoroutineScope()
  val copyToClipboard: (String) -> Unit = { text ->
    clipboardScope.launch { clipboard.setClipEntry(ClipEntry(StringSelection(text))) }
  }
  val isNull = GridFormattingHelper.isNullCell(value)
  val numeric = value is CellValue.Numeric

  Box(
    Modifier
      .width(width)
      .fillMaxHeight()
      .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
      .pointerInput(rowIndex, columnIndex) {
        var lastClickMillis = 0L
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()
            if (event.type != PointerEventType.Press) continue
            val modifiers = event.keyboardModifiers
            val extend =
              modifiers.pointerShiftPressed || modifiers.pointerCtrlPressed || modifiers.pointerMetaPressed
            if (event.button == PointerButton.Secondary) {
              if (!selected) onSelect(rowIndex, columnIndex, false)
              menuOpen = true
              continue
            }
            val now = System.currentTimeMillis()
            val doubleClick = now - lastClickMillis < 400L
            lastClickMillis = now
            onSelect(rowIndex, columnIndex, extend)
            if (doubleClick) onOpenDetail(rowIndex, columnIndex)
          }
        }
      }.padding(horizontal = 6.dp),
    contentAlignment = if (numeric) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    Text(
      text = GridFormattingHelper.truncateForCell(GridFormattingHelper.cellText(value)),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = if (numeric) TextAlign.End else TextAlign.Start,
      fontStyle = if (isNull) FontStyle.Italic else FontStyle.Normal,
      color = if (isNull) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
      style = MaterialTheme.typography.bodySmall,
    )
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
      DropdownMenuItem(
        text = { Text("Copy cell") },
        onClick = {
          copyToClipboard(state.copyCell(rowIndex, columnIndex))
          menuOpen = false
        },
      )
      DropdownMenuItem(
        text = { Text("Copy row") },
        onClick = {
          copyToClipboard(state.copyRow(rowIndex))
          menuOpen = false
        },
      )
      DropdownMenuItem(
        text = { Text("Copy selection") },
        onClick = {
          copyToClipboard(state.copySelection())
          menuOpen = false
        },
      )
      DropdownMenuItem(
        text = { Text("Copy with headers") },
        onClick = {
          copyToClipboard(state.copySelection(includeHeaders = true))
          menuOpen = false
        },
      )
    }
  }
}
