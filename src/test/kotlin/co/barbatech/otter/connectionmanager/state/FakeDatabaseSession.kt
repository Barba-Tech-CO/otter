package co.barbatech.otter.connectionmanager.state

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.database.metadata.SchemaInfo
import co.barbatech.otter.database.metadata.TableDetails
import co.barbatech.otter.database.metadata.TableInfo
import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.database.session.DatabaseSession

internal class FakeDatabaseSession(
  override val config: ConnectionConfig,
) : DatabaseSession {
  var closed = false

  override suspend fun listSchemas(): List<SchemaInfo> = emptyList()

  override suspend fun listTables(schema: SchemaInfo): List<TableInfo> = emptyList()

  override suspend fun describeTable(table: TableInfo): TableDetails =
    throw UnsupportedOperationException("not used in these tests")

  override fun previewQuery(
    table: TableInfo,
    limit: Int,
  ): String = ""

  override suspend fun execute(
    sql: String,
    fetchSize: Int,
  ): QueryResult = throw UnsupportedOperationException("not used in these tests")

  override suspend fun cancel() = Unit

  override fun close() {
    closed = true
  }
}
