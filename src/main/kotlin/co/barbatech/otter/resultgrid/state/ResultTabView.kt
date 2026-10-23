package co.barbatech.otter.resultgrid.state

import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.ResultCursor
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.database.session.DatabaseDefaults
import co.barbatech.otter.resultgrid.sort.GridSort
import co.barbatech.otter.resultgrid.sort.GridSortingHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loaded rows, sort and paging state of one tab. Rows are fetched a page at a time. */
internal class ResultTabView(
  val tab: ResultTab,
) {
  private val rowSet = tab.result.getOrNull() as? QueryResult.RowSet
  private val cursor: ResultCursor? = rowSet?.cursor
  private val loaded = mutableListOf<Row>()
  private var closed = false

  var started = false

  private val _rows = MutableStateFlow<List<Row>>(emptyList())
  val rows: StateFlow<List<Row>> = _rows.asStateFlow()

  private val _columns = MutableStateFlow<List<ResultColumn>>(rowSet?.cursor?.columns ?: emptyList())
  val columns: StateFlow<List<ResultColumn>> = _columns.asStateFlow()

  private val _hasMore = MutableStateFlow(rowSet != null)
  val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _sort = MutableStateFlow<GridSort?>(null)
  val sort: StateFlow<GridSort?> = _sort.asStateFlow()

  private val _fetchError = MutableStateFlow<String?>(null)
  val fetchError: StateFlow<String?> = _fetchError.asStateFlow()

  fun load(
    scope: CoroutineScope,
    onChanged: () -> Unit,
  ) {
    val cursor = cursor ?: return
    if (closed || _loading.value || !_hasMore.value) return
    _loading.value = true
    scope.launch {
      try {
        val page = cursor.fetch(DatabaseDefaults.defaultFetchSize)
        loaded.addAll(page)
        if (page.size < DatabaseDefaults.defaultFetchSize || !cursor.hasMore) {
          _hasMore.value = false
        }
        publish()
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: Exception) {
        _hasMore.value = false
        _fetchError.value = failure.message ?: "Failed to fetch rows"
      } finally {
        _loading.value = false
        onChanged()
      }
    }
  }

  fun toggleSort(columnIndex: Int) {
    if (columnIndex !in _columns.value.indices) return
    val current = _sort.value
    _sort.value =
      when {
        current == null || current.columnIndex != columnIndex -> GridSort(columnIndex, ascending = true)
        current.ascending -> GridSort(columnIndex, ascending = false)
        else -> null
      }
    publish()
  }

  fun close(scope: CoroutineScope) {
    if (closed) return
    closed = true
    _hasMore.value = false
    val cursor = cursor ?: return
    scope.launch { cursor.close() }
  }

  private fun publish() {
    val sort = _sort.value
    _rows.value =
      if (sort == null) loaded.toList() else GridSortingHelper.sortRows(loaded, sort.columnIndex, sort.ascending)
  }
}
