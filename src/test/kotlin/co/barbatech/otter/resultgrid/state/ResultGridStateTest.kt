package co.barbatech.otter.resultgrid.state

import co.barbatech.otter.database.error.DatabaseErrorKind
import co.barbatech.otter.database.error.DatabaseException
import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.ResultCursor
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.queryexport.ResultExporterFactory
import co.barbatech.otter.queryexport.state.ExportState
import co.barbatech.otter.resultgrid.format.GridFormattingHelper
import co.barbatech.otter.resultgrid.state.CellPosition
import co.barbatech.otter.resultgrid.state.ResultGridState
import co.barbatech.otter.resultgrid.state.ResultTab
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResultGridStateTest {
  private val columns = listOf(ResultColumn("id", "INTEGER"), ResultColumn("name", "TEXT"))

  private class FakeCursor(
    private val rows: List<Row>,
    override val columns: List<ResultColumn>,
  ) : ResultCursor {
    var closed = false
    private var position = 0

    override val hasMore: Boolean get() = position < rows.size

    override suspend fun fetch(count: Int): List<Row> {
      val end = minOf(position + count, rows.size)
      val page = rows.subList(position, end).toList()
      position = end
      return page
    }

    override fun close() {
      closed = true
    }
  }

  private fun rowSetTab(cursor: ResultCursor): ResultTab {
    val result = QueryResult.RowSet(cursor)
    return ResultTab("select 1", Duration.ofMillis(12), Result.success(result)) { result }
  }

  private fun state(scope: TestScope): ResultGridState =
    ResultGridState(ExportState(ResultExporterFactory.streamingExporter(), scope), scope)

  private fun rows(count: Int): List<Row> =
    (0 until count).map { index ->
      Row(listOf(CellValue.Numeric(BigDecimal.valueOf(index.toLong())), CellValue.Text("name $index")))
    }

  @Test
  fun loadsOnePageAtATimeOffTheCaller() =
    runTest {
      val cursor = FakeCursor(rows(1200), columns)
      val state = state(this)
      state.show(listOf(rowSetTab(cursor)))
      advanceUntilIdle()

      assertEquals(
        500,
        state
          .selectedView()!!
          .rows.value.size,
      )
      assertTrue(state.selectedView()!!.hasMore.value)
      assertTrue(state.statusText.value.contains("500 rows loaded"))
      assertTrue(state.statusText.value.contains("more available"))

      state.loadMore()
      advanceUntilIdle()
      assertEquals(
        1000,
        state
          .selectedView()!!
          .rows.value.size,
      )

      state.loadMore()
      advanceUntilIdle()
      assertEquals(
        1200,
        state
          .selectedView()!!
          .rows.value.size,
      )
      assertFalse(state.selectedView()!!.hasMore.value)
      assertTrue(state.statusText.value.contains("end of result"))
    }

  @Test
  fun shortResultStopsAfterTheFirstPage() =
    runTest {
      val state = state(this)
      state.show(listOf(rowSetTab(FakeCursor(rows(3), columns))))
      advanceUntilIdle()

      assertEquals(
        3,
        state
          .selectedView()!!
          .rows.value.size,
      )
      assertFalse(state.selectedView()!!.hasMore.value)

      state.loadMore()
      advanceUntilIdle()
      assertEquals(
        3,
        state
          .selectedView()!!
          .rows.value.size,
      )
    }

  @Test
  fun replacingResultsClosesOldCursors() =
    runTest {
      val first = FakeCursor(rows(3), columns)
      val second = FakeCursor(rows(2), columns)
      val state = state(this)
      state.show(listOf(rowSetTab(first)))
      advanceUntilIdle()

      state.show(listOf(rowSetTab(second)))
      advanceUntilIdle()

      assertTrue(first.closed)
      assertFalse(second.closed)
    }

  @Test
  fun closingATabClosesItsCursorAndSelectsTheNeighbour() =
    runTest {
      val first = FakeCursor(rows(2), columns)
      val second = FakeCursor(rows(2), columns)
      val state = state(this)
      state.show(listOf(rowSetTab(first), rowSetTab(second)))
      advanceUntilIdle()
      state.select(1)

      state.close(1)
      advanceUntilIdle()

      assertTrue(second.closed)
      assertFalse(first.closed)
      assertEquals(1, state.tabs.value.size)
      assertEquals(0, state.selectedIndex.value)
    }

  @Test
  fun closingTheLastTabLeavesTheGridEmpty() =
    runTest {
      val cursor = FakeCursor(rows(2), columns)
      val state = state(this)
      state.show(listOf(rowSetTab(cursor)))
      advanceUntilIdle()

      state.close(0)
      advanceUntilIdle()

      assertTrue(cursor.closed)
      assertTrue(state.tabs.value.isEmpty())
      assertEquals("", state.statusText.value)
    }

  @Test
  fun switchingTabsChangesTheStatus() =
    runTest {
      val state = state(this)
      val affected =
        ResultTab("update", Duration.ofMillis(4), Result.success(QueryResult.Affected(7))) {
          QueryResult.Affected(7)
        }
      state.show(listOf(rowSetTab(FakeCursor(rows(2), columns)), affected))
      advanceUntilIdle()
      assertEquals("2 rows loaded", state.statusText.value.substringBefore(" - "))

      state.select(1)
      assertEquals("7 rows affected", state.statusText.value)
    }

  @Test
  fun errorTabReportsTheDatabaseMessageInTheStatus() =
    runTest {
      val failure = DatabaseException(DatabaseErrorKind.QueryFailed, "relation does not exist", position = 8)
      val tab = ResultTab("select nope", Duration.ofMillis(1), Result.failure(failure)) { throw failure }
      val state = state(this)
      state.show(listOf(tab))
      advanceUntilIdle()

      assertEquals("Error: relation does not exist", state.statusText.value)
    }

  @Test
  fun sortCyclesLoadedRowsThenRestoresTheOriginalOrder() =
    runTest {
      val state = state(this)
      val original = rows(3).let { listOf(it[1], it[2], it[0]) }
      state.show(listOf(rowSetTab(FakeCursor(original, columns))))
      advanceUntilIdle()
      assertEquals(listOf("1", "2", "0"), ids(state))

      state.toggleSort(0)
      assertEquals(listOf("0", "1", "2"), ids(state))

      state.toggleSort(0)
      assertEquals(listOf("2", "1", "0"), ids(state))

      state.toggleSort(0)
      assertEquals(listOf("1", "2", "0"), ids(state))
      assertNull(state.selectedView()!!.sort.value)
    }

  @Test
  fun sortedLoadedRowsCaveatAppearsOnlyWhileMoreRowsExist() =
    runTest {
      val state = state(this)
      state.show(listOf(rowSetTab(FakeCursor(rows(600), columns))))
      advanceUntilIdle()

      state.toggleSort(0)
      assertTrue(state.statusText.value.contains("sorted loaded rows only"))

      state.loadMore()
      advanceUntilIdle()
      assertFalse(state.selectedView()!!.hasMore.value)
      assertFalse(state.statusText.value.contains("sorted loaded rows only"))
    }

  @Test
  fun selectionExtendsToARangeAndCopiesTabSeparatedText() =
    runTest {
      val state = state(this)
      state.show(listOf(rowSetTab(FakeCursor(rows(3), columns))))
      advanceUntilIdle()

      state.selectCell(0, 0, extend = false)
      state.selectCell(1, 1, extend = true)

      assertEquals("0\tname 0\n1\tname 1", state.copySelection())
      assertEquals("id\tname\n0\tname 0\n1\tname 1", state.copySelection(includeHeaders = true))
      assertEquals("1", state.copyCell(1, 0))
      assertEquals("1\tname 1", state.copyRow(1))
    }

  @Test
  fun arrowKeysMoveTheSelectionWithinLoadedRows() =
    runTest {
      val state = state(this)
      state.show(listOf(rowSetTab(FakeCursor(rows(3), columns))))
      advanceUntilIdle()

      state.moveSelection(0, 1)
      assertEquals(CellPosition(0, 1), state.selection.value?.focus)

      state.moveSelection(-1, 0)
      assertEquals(CellPosition(0, 1), state.selection.value?.focus)

      state.moveSelection(5, 0)
      assertEquals(CellPosition(2, 1), state.selection.value?.focus)
    }

  private fun ids(state: ResultGridState): List<String> =
    state
      .selectedView()!!
      .rows.value
      .map { GridFormattingHelper.cellText(it.cells.first()) }
}
