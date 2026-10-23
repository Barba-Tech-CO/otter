package co.barbatech.otter.database.session

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.database.error.DatabaseException
import co.barbatech.otter.database.metadata.SchemaInfo
import co.barbatech.otter.database.metadata.TableDetails
import co.barbatech.otter.database.metadata.TableInfo
import co.barbatech.otter.database.result.QueryResult

/**
 * An open database connection. All suspend functions run off the UI thread.
 * Execute one statement at a time; several cursors from earlier statements may stay open.
 */
interface DatabaseSession : AutoCloseable {
  val config: ConnectionConfig

  suspend fun listSchemas(): List<SchemaInfo>

  /** Tables and views of [schema]. */
  suspend fun listTables(schema: SchemaInfo): List<TableInfo>

  suspend fun describeTable(table: TableInfo): TableDetails

  /** Dialect specific "SELECT * FROM table" limited to [limit] rows, with correct identifier quoting. */
  fun previewQuery(
    table: TableInfo,
    limit: Int,
  ): String

  /**
   * Runs exactly one statement, unmodified. Rows are streamed with [fetchSize]; nothing is read until
   * the cursor is asked. Throws [DatabaseException].
   */
  suspend fun execute(
    sql: String,
    fetchSize: Int = DatabaseDefaults.defaultFetchSize,
  ): QueryResult

  /** Cancels the statement running now (JDBC Statement.cancel). No effect when idle. */
  suspend fun cancel()

  override fun close()
}
