package co.barbatech.otter.database.session

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionType
import co.barbatech.otter.database.error.DatabaseErrorKind
import co.barbatech.otter.database.error.DatabaseException
import co.barbatech.otter.database.metadata.SchemaInfo
import co.barbatech.otter.database.metadata.TableKind
import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.database.session.DatabaseSession
import co.barbatech.otter.database.session.JdbcDatabaseSessionFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertIs

class JdbcDatabaseSessionTest {
  @Test
  fun readsSqliteMetadataAndQuotedPreview() {
    runBlocking {
      withSession { session ->
        session.execute("PRAGMA foreign_keys = ON")
        session.execute("CREATE TABLE parent (id INTEGER PRIMARY KEY)")
        session.execute(
          """
          CREATE TABLE "odd""name" (
            id INTEGER PRIMARY KEY,
            parent_id INTEGER,
            value TEXT DEFAULT 'x',
            quoted TEXT DEFAULT 'it''s',
            expression TEXT DEFAULT (lower('X')),
            FOREIGN KEY(parent_id) REFERENCES parent(id)
          )
          """.trimIndent(),
        )
        session.execute("CREATE UNIQUE INDEX unique_value ON \"odd\"\"name\"(value)")
        session.execute("CREATE VIEW sample_view AS SELECT id FROM parent")

        assertEquals(listOf("main"), session.listSchemas().map { it.name })
        val tables = session.listTables(SchemaInfo("main"))
        assertTrue(tables.any { it.name == "sample_view" && it.kind == TableKind.View })
        val table = tables.single { it.name == "odd\"name" }
        val details = session.describeTable(table)
        assertTrue(details.columns.single { it.name == "id" }.isPrimaryKey)
        assertEquals("x", details.columns.single { it.name == "value" }.defaultValue)
        assertEquals("it's", details.columns.single { it.name == "quoted" }.defaultValue)
        assertEquals("lower('X')", details.columns.single { it.name == "expression" }.defaultValue)
        assertTrue(details.indexes.any { it.name == "unique_value" && it.unique && it.columns == listOf("value") })
        assertEquals(listOf("parent_id"), details.foreignKeys.single().columns)
        assertEquals(listOf("id"), details.foreignKeys.single().referencedColumns)
        assertEquals("SELECT * FROM \"main\".\"odd\"\"name\" LIMIT 25", session.previewQuery(table, 25))
      }
    }
  }

  @Test
  fun executesUpdatesDdlAndSyntaxErrors() {
    runBlocking {
      withSession { session ->
        assertIs<QueryResult.Affected>(session.execute("CREATE TABLE items (id INTEGER PRIMARY KEY, value TEXT)"))
        session.execute("INSERT INTO items(value) VALUES ('a')")
        val updated = session.execute("UPDATE items SET value = 'b' WHERE id = 1")
        assertEquals(1L, (updated as QueryResult.Affected).rowCount)
        val error = runCatching { session.execute("SELEC invalid") }.exceptionOrNull()
        assertIs<DatabaseException>(error)
        assertEquals(DatabaseErrorKind.QueryFailed, error.kind)
        assertTrue(error.message!!.isNotBlank())
      }
    }
  }

  @Test
  fun fetchesIncrementallyPreservingNullEmptyStringAndLargeResults() {
    runBlocking {
      withSession { session ->
        session.execute("CREATE TABLE values_table (value TEXT)")
        session.execute("INSERT INTO values_table VALUES (NULL), (''), ('text')")
        val query = "SELECT value FROM values_table ORDER BY rowid"
        val result = session.execute(query, fetchSize = 2) as QueryResult.RowSet
        assertTrue(result.cursor.hasMore)
        val first = result.cursor.fetch(1)
        assertEquals(CellValue.Null, first.single().cells.single())
        val second = result.cursor.fetch(1)
        assertEquals(CellValue.Text(""), second.single().cells.single())
        assertTrue(result.cursor.hasMore)
        assertEquals(
          CellValue.Text("text"),
          result.cursor
            .fetch(2)
            .single()
            .cells
            .single(),
        )
        assertTrue(!result.cursor.hasMore)
        result.cursor.close()

        session.execute("CREATE TABLE numbers (n INTEGER)")
        session.execute(
          """
          WITH RECURSIVE seq(n) AS (
            SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1200
          ) INSERT INTO numbers SELECT n FROM seq
          """.trimIndent(),
        )
        val large = session.execute("SELECT n FROM numbers ORDER BY n", fetchSize = 37) as QueryResult.RowSet
        var total = 0
        while (large.cursor.hasMore) total += large.cursor.fetch(37).size
        assertEquals(1200, total)
        large.cursor.close()
      }
    }
  }

  @Test
  fun sessionRemainsUsableAfterIdleCancel() {
    // SQLite cancellation depends on driver interruption support; this checks the idle-cancel fallback.
    runBlocking {
      withSession { session ->
        session.cancel()
        session.execute("CREATE TABLE safe (value INTEGER)")
        val result = session.execute("SELECT 7") as QueryResult.RowSet
        assertEquals(
          7,
          (
            result.cursor
              .fetch(1)
              .single()
              .cells
              .single() as CellValue.Numeric
          ).value.toInt(),
        )
        result.cursor.close()
      }
    }
  }

  private suspend fun withSession(block: suspend (DatabaseSession) -> Unit) {
    val file = Files.createTempFile("otter-db-test", ".sqlite")
    val config =
      ConnectionConfig(
        id = "test",
        name = "SQLite test",
        type = ConnectionType.Sqlite,
        filePath = file.toString(),
      )
    val session = JdbcDatabaseSessionFactory().open(config, null)
    try {
      block(session)
    } finally {
      session.close()
      Files.deleteIfExists(file)
    }
  }
}
