package co.barbatech.otter.schemabrowser

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionType
import co.barbatech.otter.database.metadata.ColumnInfo
import co.barbatech.otter.database.metadata.ForeignKeyInfo
import co.barbatech.otter.database.metadata.IndexInfo
import co.barbatech.otter.database.metadata.SchemaInfo
import co.barbatech.otter.database.metadata.TableDetails
import co.barbatech.otter.database.metadata.TableInfo
import co.barbatech.otter.database.metadata.TableKind
import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.database.session.DatabaseSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SchemaBrowserStateTest {
  @Test
  fun loadsLazilyFiltersRefreshesAndOpensTablePreview() {
    runBlocking {
      val session = FakeSession()
      val activeSession = MutableStateFlow<DatabaseSession?>(session)
      val openedQueries = mutableListOf<String>()
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
      val state = SchemaBrowserState(activeSession, openedQueries::add, scope)
      try {
        withTimeout(2.seconds) { state.nodes.first { it.size == 1 } }
        assertEquals(0, session.tableLoads)
        val schema = state.nodes.value.single()
        state.toggle(schema)
        withTimeout(2.seconds) { state.nodes.first { it.size == 2 } }
        assertEquals(1, session.tableLoads)

        val tableNode = state.nodes.value.single { it.table != null }
        state.toggle(tableNode)
        withTimeout(2.seconds) { state.nodes.first { it.size == 5 } }
        assertTrue(state.nodes.value.any { it.label.startsWith("Column id:") })
        state.setFilter("users")
        assertTrue(state.visibleNodes().any { it.table != null })
        state.openTable(requireNotNull(tableNode.table))
        assertEquals(listOf("SELECT * FROM \"main\".\"users\" LIMIT 500"), openedQueries)

        state.select(schema)
        state.refresh()
        withTimeout(2.seconds) { while (session.tableLoads < 2) delay(10.milliseconds) }
        assertEquals(2, session.tableLoads)
      } finally {
        scope.cancel()
        session.close()
      }
    }
  }

  @Test
  fun cancelledLoadIsNotReportedAsAnError() {
    runBlocking {
      val started = CompletableDeferred<Unit>()
      val session = FakeSession(started)
      val activeSession = MutableStateFlow<DatabaseSession?>(session)
      val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
      val state = SchemaBrowserState(activeSession, {}, scope)
      try {
        withTimeout(2.seconds) { started.await() }
        scope.cancel()
        withTimeout(2.seconds) { state.loadingNodeIds.first { it.isEmpty() } }
        assertTrue(state.errors.value.isEmpty())
      } finally {
        session.close()
      }
    }
  }

  private class FakeSession(
    private val startSignal: CompletableDeferred<Unit>? = null,
  ) : DatabaseSession {
    override val config = ConnectionConfig("fake", "Fake", ConnectionType.Sqlite, filePath = ":memory:")
    var tableLoads = 0

    override suspend fun listSchemas(): List<SchemaInfo> {
      if (startSignal != null) {
        startSignal.complete(Unit)
        awaitCancellation()
      }
      return listOf(SchemaInfo("main"))
    }

    override suspend fun listTables(schema: SchemaInfo): List<TableInfo> {
      tableLoads++
      return listOf(TableInfo("main", "users", TableKind.Table))
    }

    override suspend fun describeTable(table: TableInfo) =
      TableDetails(
        table,
        listOf(ColumnInfo("id", "INTEGER", false, null, true)),
        listOf(IndexInfo("users_pk", listOf("id"), true)),
        listOf(ForeignKeyInfo("fk", listOf("id"), "main", "users", listOf("id"))),
      )

    override fun previewQuery(
      table: TableInfo,
      limit: Int,
    ) = "SELECT * FROM \"main\".\"${table.name}\" LIMIT $limit"

    override suspend fun execute(
      sql: String,
      fetchSize: Int,
    ): QueryResult = error("Not used")

    override suspend fun cancel() = Unit

    override fun close() = Unit
  }
}
