package co.barbatech.otter.resultgrid.ui.table

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import co.barbatech.otter.resultgrid.format.GridFormattingHelper
import co.barbatech.otter.resultgrid.state.ResultGridState
import co.barbatech.otter.resultgrid.state.ResultTabView
import kotlinx.coroutines.launch
import java.awt.datatransfer.StringSelection

/** Virtualized table with sticky header, lazy paging, sorting, selection and copy actions. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ResultTable(
  state: ResultGridState,
  view: ResultTabView,
  modifier: Modifier = Modifier,
) {
  val rows by view.rows.collectAsState()
  val columns by view.columns.collectAsState()
  val hasMore by view.hasMore.collectAsState()
  val loading by view.loading.collectAsState()
  val sort by view.sort.collectAsState()
  val fetchError by view.fetchError.collectAsState()
  val selection by state.selection.collectAsState()
  val clipboard = LocalClipboard.current
  val clipboardScope = rememberCoroutineScope()
  val focusRequester = remember { FocusRequester() }
  var focused by remember { mutableStateOf(false) }
  var detail by remember { mutableStateOf<CellDetail?>(null) }
  val horizontalScroll = rememberScrollState()

  val columnWidths =
    remember(columns, rows) {
      GridFormattingHelper.columnCharWidths(columns, rows).map { GridMetrics.columnWidthDp(it) }
    }
  val totalWidth = columnWidths.fold(0.dp) { total, width -> total + width }

  val onCopy: (String) -> Unit = { text ->
    clipboardScope.launch { clipboard.setClipEntry(ClipEntry(StringSelection(text))) }
  }
  val openDetail: () -> Unit = {
    val position = selection?.focus
    if (position != null) {
      val value = rows.getOrNull(position.row)?.cells?.getOrNull(position.column)
      detail = CellDetail(columns.getOrNull(position.column)?.name ?: "", value)
    }
  }
  val onSelect: (Int, Int, Boolean) -> Unit = { row, column, extend ->
    state.selectCell(row, column, extend)
    focusRequester.requestFocus()
  }

  Box(
    modifier
      .focusRequester(focusRequester)
      .onFocusChanged { focused = it.isFocused }
      .onKeyEvent { event -> GridKeyHandler.handle(event, state, onCopy, openDetail) }
      .focusable()
      .then(if (focused) Modifier.border(1.dp, MaterialTheme.colorScheme.primary) else Modifier),
  ) {
    Box(Modifier.fillMaxSize().horizontalScroll(horizontalScroll)) {
      LazyColumn(Modifier.width(totalWidth).fillMaxHeight()) {
        stickyHeader {
          HeaderRow(columns, columnWidths, sort) { columnIndex -> state.toggleSort(columnIndex) }
        }
        itemsIndexed(rows) { index, row ->
          GridRow(
            state = state,
            row = row,
            rowIndex = index,
            columns = columns,
            widths = columnWidths,
            selection = selection,
            onSelect = onSelect,
            onOpenDetail = { rowIndex, columnIndex ->
              detail = CellDetail(columns[columnIndex].name, rows[rowIndex].cells.getOrNull(columnIndex))
            },
          )
        }
        item {
          LaunchedEffect(rows.size, hasMore) {
            if (hasMore) state.loadMore()
          }
          if (loading || rows.isEmpty()) {
            Box(
              Modifier.width(totalWidth).height(GridMetrics.rowHeight).padding(horizontal = 8.dp),
              contentAlignment = Alignment.CenterStart,
            ) {
              when {
                loading -> Text("Loading\u2026", style = MaterialTheme.typography.bodySmall)
                rows.isEmpty() -> Text("No rows", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }
    fetchError?.let { message ->
      Text(
        message,
        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }

  detail?.let { current ->
    CellDetailDialog(current, onDismiss = { detail = null })
  }
}
