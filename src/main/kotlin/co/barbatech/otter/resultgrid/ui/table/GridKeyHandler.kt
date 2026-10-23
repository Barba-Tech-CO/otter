package co.barbatech.otter.resultgrid.ui.table

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import co.barbatech.otter.resultgrid.state.ResultGridState

/** Maps grid key presses to selection movement, clipboard copy and cell detail actions. */
internal object GridKeyHandler {
  fun handle(
    event: KeyEvent,
    state: ResultGridState,
    onCopy: (String) -> Unit,
    onOpenDetail: () -> Unit,
  ): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val copyPressed = event.isCtrlPressed || event.isMetaPressed
    return when {
      copyPressed && event.key == Key.C -> {
        onCopy(state.copySelection())
        true
      }

      event.key == Key.DirectionLeft -> {
        state.moveSelection(0, -1)
        true
      }

      event.key == Key.DirectionRight -> {
        state.moveSelection(0, 1)
        true
      }

      event.key == Key.DirectionUp -> {
        state.moveSelection(-1, 0)
        true
      }

      event.key == Key.DirectionDown -> {
        state.moveSelection(1, 0)
        true
      }

      event.key == Key.PageUp -> {
        state.moveSelection(-20, 0)
        true
      }

      event.key == Key.PageDown -> {
        state.moveSelection(20, 0)
        true
      }

      event.key == Key.Enter -> {
        onOpenDetail()
        true
      }

      else -> {
        false
      }
    }
  }
}
