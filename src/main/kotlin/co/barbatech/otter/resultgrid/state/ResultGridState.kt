package co.barbatech.otter.resultgrid.state

import co.barbatech.otter.queryexport.state.ExportState
import co.barbatech.otter.resultgrid.clipboard.GridClipboardHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UI state of the result area: one tab per executed statement, with lazy paging and selection. */
class ResultGridState(
  val export: ExportState,
  private val scope: CoroutineScope,
) {
  private val _tabs = MutableStateFlow<List<ResultTab>>(emptyList())
  val tabs: StateFlow<List<ResultTab>> = _tabs.asStateFlow()

  private val _selectedIndex = MutableStateFlow(0)
  val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

  /** Text for the status bar: row count, elapsed time, page position. */
  private val _statusText = MutableStateFlow("")
  val statusText: StateFlow<String> = _statusText.asStateFlow()

  private val _selection = MutableStateFlow<GridSelection?>(null)
  val selection: StateFlow<GridSelection?> = _selection.asStateFlow()

  private var views: List<ResultTabView> = emptyList()

  /** Replaces all tabs with the outcome of a new run and closes the cursors of the old tabs. */
  fun show(newTabs: List<ResultTab>) {
    views.forEach { it.close(scope) }
    views = newTabs.map { ResultTabView(it) }
    _tabs.value = newTabs
    _selection.value = null
    _selectedIndex.value = 0
    if (views.isNotEmpty()) loadFirstPage(views[0])
    updateStatus()
  }

  fun select(index: Int) {
    if (index !in views.indices) return
    _selectedIndex.value = index
    _selection.value = null
    loadFirstPage(views[index])
    updateStatus()
  }

  /** Removes a tab and closes its cursor. Closing the last tab leaves the grid empty. */
  fun close(index: Int) {
    val view = views.getOrNull(index) ?: return
    view.close(scope)
    views = views.toMutableList().apply { removeAt(index) }
    _tabs.value = _tabs.value.toMutableList().apply { removeAt(index) }
    _selection.value = null
    if (views.isEmpty()) {
      _selectedIndex.value = 0
      updateStatus()
      return
    }
    val selected = _selectedIndex.value
    val next = if (index < selected) selected - 1 else selected.coerceAtMost(views.lastIndex)
    _selectedIndex.value = next
    loadFirstPage(views[next])
    updateStatus()
  }

  /** Fetches the next page of the selected tab, off the UI thread, when rows remain. */
  fun loadMore() {
    val view = selectedView() ?: return
    view.load(scope) { updateStatus() }
  }

  /** Cycles the sort of the loaded rows: ascending, descending, then the original order. */
  fun toggleSort(columnIndex: Int) {
    val view = selectedView() ?: return
    view.toggleSort(columnIndex)
    updateStatus()
  }

  fun selectCell(
    row: Int,
    column: Int,
    extend: Boolean,
  ) {
    val view = selectedView() ?: return
    if (row !in view.rows.value.indices || column !in view.columns.value.indices) return
    val position = CellPosition(row, column)
    val anchor = _selection.value?.anchor
    _selection.value =
      if (extend && anchor != null) GridSelection(anchor, position) else GridSelection(position, position)
  }

  /** Moves the cell selection by the given delta, clamped to the loaded rows. */
  fun moveSelection(
    deltaRow: Int,
    deltaColumn: Int,
  ) {
    val view = selectedView() ?: return
    val rowCount = view.rows.value.size
    val columnCount = view.columns.value.size
    if (rowCount == 0 || columnCount == 0) return
    val current = _selection.value?.focus ?: CellPosition(0, 0)
    val next =
      CellPosition(
        (current.row + deltaRow).coerceIn(0, rowCount - 1),
        (current.column + deltaColumn).coerceIn(0, columnCount - 1),
      )
    _selection.value = GridSelection(next, next)
  }

  /** Clipboard text of the selection: tab separated cells, NULL becomes empty text. */
  fun copySelection(includeHeaders: Boolean = false): String {
    val view = selectedView() ?: return ""
    val selection = _selection.value ?: return ""
    return GridClipboardHelper.formatRowsForClipboard(view.rows.value, view.columns.value, selection, includeHeaders)
  }

  fun copyCell(
    row: Int,
    column: Int,
  ): String {
    val view = selectedView() ?: return ""
    val cells =
      view.rows.value
        .getOrNull(row)
        ?.cells ?: return ""
    return GridClipboardHelper.clipboardText(cells.getOrNull(column))
  }

  fun copyRow(row: Int): String {
    val view = selectedView() ?: return ""
    val cells =
      view.rows.value
        .getOrNull(row)
        ?.cells ?: return ""
    return cells.joinToString("\t") { GridClipboardHelper.escapeTsvField(GridClipboardHelper.clipboardText(it)) }
  }

  internal fun selectedView(): ResultTabView? = views.getOrNull(_selectedIndex.value)

  private fun loadFirstPage(view: ResultTabView) {
    if (view.started) return
    view.started = true
    view.load(scope) { updateStatus() }
  }

  private fun updateStatus() {
    val view = selectedView()
    _statusText.value = if (view == null) "" else ResultStatusHelper.statusFor(view)
  }
}
