package co.barbatech.otter.sqleditor

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionType
import co.barbatech.otter.database.error.DatabaseErrorKind
import co.barbatech.otter.database.error.DatabaseException
import co.barbatech.otter.database.metadata.SchemaInfo
import co.barbatech.otter.database.metadata.TableDetails
import co.barbatech.otter.database.metadata.TableInfo
import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.database.session.DatabaseSession
import co.barbatech.otter.queryexport.exporter.ExportProgress
import co.barbatech.otter.queryexport.exporter.ResultExporter
import co.barbatech.otter.queryexport.format.ExportFormat
import co.barbatech.otter.queryexport.state.ExportState
import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.model.HistorySettings
import co.barbatech.otter.queryhistory.store.HistoryStore
import co.barbatech.otter.resultgrid.state.ResultGridState
import co.barbatech.otter.sqlsplitter.DefaultSqlStatementSplitter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SqlEditorStateTest {
  @Test
  fun runsSelectionAndRecordsEveryStatement() =
    runTest {
      val session = FakeSession()
      val history = FakeHistoryStore()
      val grid = createGrid()
      val state = createState(MutableStateFlow(session), history, grid, this)
      state.setText("select 8")
      state.run("select 1; select 2")
      advanceUntilIdle()

      assertEquals(listOf("select 1", "select 2"), session.executed)
      assertEquals(2, history.entries.size)
      assertTrue(grid.tabs.value.all { it.result.isSuccess })
    }

  @Test
  fun executesWholeScriptWhenSelectionIsEmpty() =
    runTest {
      val session = FakeSession()
      val state = createState(MutableStateFlow(session), FakeHistoryStore(), createGrid(), this)
      state.setText("select 1; select 2")
      state.run("")
      advanceUntilIdle()
      assertEquals(listOf("select 1", "select 2"), session.executed)
    }

  @Test
  fun stopsAfterErrorAndShowsPosition() =
    runTest {
      val session = FakeSession(failOn = "select 2")
      val history = FakeHistoryStore()
      val grid = createGrid()
      val state = createState(MutableStateFlow(session), history, grid, this)
      state.run("select 1; select 2; select 3")
      advanceUntilIdle()

      assertEquals(listOf("select 1", "select 2"), session.executed)
      assertEquals(2, history.entries.size)
      assertTrue(
        history.entries
          .last()
          .error!!
          .contains("position 4"),
      )
      assertEquals(2, grid.tabs.value.size)
      assertFalse(
        grid.tabs.value
          .last()
          .result.isSuccess,
      )
    }

  @Test
  fun recordsCancellationAndReturnsToIdle() =
    runTest {
      val session = FakeSession(waitForCancel = true)
      val history = FakeHistoryStore()
      val state = createState(MutableStateFlow(session), history, createGrid(), this)
      state.run("select 1; select 2")
      testScheduler.runCurrent()
      assertTrue(state.running.value)
      state.cancel()
      advanceUntilIdle()

      assertFalse(state.running.value)
      assertEquals(1, history.entries.size)
      assertFalse(history.entries.single().success)
      assertTrue(
        history.entries
          .single()
          .error!!
          .contains("cancel", ignoreCase = true),
      )
    }

  @Test
  fun doesNothingWithoutActiveSession() =
    runTest {
      val history = FakeHistoryStore()
      val state = createState(MutableStateFlow(null), history, createGrid(), this)
      state.run("select 1")
      advanceUntilIdle()
      assertTrue(history.entries.isEmpty())
      assertFalse(state.running.value)
    }

  private fun createState(
    session: MutableStateFlow<DatabaseSession?>,
    history: FakeHistoryStore,
    grid: ResultGridState,
    scope: kotlinx.coroutines.CoroutineScope,
  ) = SqlEditorState(session, DefaultSqlStatementSplitter(), history, grid, scope)

  private fun createGrid() =
    ResultGridState(
      ExportState(
        object : ResultExporter {
          override fun export(
            cursor: co.barbatech.otter.database.result.ResultCursor,
            format: ExportFormat,
            target: Path,
          ): Flow<ExportProgress> = emptyFlow()
        },
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
      ),
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
    )

  private class FakeHistoryStore : HistoryStore {
    override val settings = MutableStateFlow(HistorySettings())
    override val lastError = MutableStateFlow<String?>(null)
    val entries = mutableListOf<HistoryEntry>()

    override suspend fun updateSettings(settings: HistorySettings) {
      this.settings.value = settings
    }

    override suspend fun record(entry: HistoryEntry) {
      entries += entry
    }

    override suspend fun search(
      query: String,
      limit: Int,
    ) = entries.take(limit).reversed()

    override suspend fun clear() {
      entries.clear()
    }
  }

  private class FakeSession(
    private val failOn: String? = null,
    private val waitForCancel: Boolean = false,
  ) : DatabaseSession {
    override val config = ConnectionConfig("id", "Test", ConnectionType.Sqlite, filePath = ":memory:")
    val executed = mutableListOf<String>()
    private val cancellation = CompletableDeferred<Unit>()

    override suspend fun listSchemas() = emptyList<SchemaInfo>()

    override suspend fun listTables(schema: SchemaInfo) = emptyList<TableInfo>()

    override suspend fun describeTable(table: TableInfo) = TableDetails(table, emptyList(), emptyList(), emptyList())

    override fun previewQuery(
      table: TableInfo,
      limit: Int,
    ) = ""

    override suspend fun execute(
      sql: String,
      fetchSize: Int,
    ): QueryResult {
      executed += sql
      if (sql == failOn) throw DatabaseException(DatabaseErrorKind.QueryFailed, "bad query", 4)
      if (waitForCancel) {
        cancellation.await()
        throw DatabaseException(DatabaseErrorKind.Cancelled, "Query cancelled")
      }
      return QueryResult.Affected(1)
    }

    override suspend fun cancel() {
      cancellation.complete(Unit)
    }

    override fun close() = Unit
  }
}
