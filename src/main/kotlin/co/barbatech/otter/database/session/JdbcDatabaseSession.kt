package co.barbatech.otter.database.session

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionType
import co.barbatech.otter.database.error.DatabaseErrorKind
import co.barbatech.otter.database.error.DatabaseErrors
import co.barbatech.otter.database.error.DatabaseException
import co.barbatech.otter.database.metadata.ColumnInfo
import co.barbatech.otter.database.metadata.ForeignKeyInfo
import co.barbatech.otter.database.metadata.IndexInfo
import co.barbatech.otter.database.metadata.SchemaInfo
import co.barbatech.otter.database.metadata.TableDetails
import co.barbatech.otter.database.metadata.TableInfo
import co.barbatech.otter.database.metadata.TableKind
import co.barbatech.otter.database.result.JdbcResultCursor
import co.barbatech.otter.database.result.QueryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.locks.ReentrantLock

/** JDBC session. PostgreSQL cursors use a transaction and commit or roll back when closed. */
internal class JdbcDatabaseSession(
  override val config: ConnectionConfig,
  private val connection: Connection,
) : DatabaseSession {
  internal val jdbcLock = ReentrantLock()

  @Volatile private var runningStatement: Statement? = null

  @Volatile private var openPostgresCursors = 0

  @Volatile private var closed = false

  override suspend fun listSchemas(): List<SchemaInfo> =
    withJdbc {
      try {
        if (config.type == ConnectionType.Sqlite) {
          listOf(SchemaInfo("main"))
        } else {
          connection.metaData.getSchemas().use { rows ->
            buildList { while (rows.next()) add(SchemaInfo(rows.getString("TABLE_SCHEM"))) }
          }
        }
      } catch (exception: Exception) {
        throw DatabaseErrors.from(exception)
      }
    }

  override suspend fun listTables(schema: SchemaInfo): List<TableInfo> =
    withJdbc {
      try {
        val catalog = if (config.type == ConnectionType.Sqlite) connection.catalog ?: "main" else null
        connection.metaData.getTables(catalog, schema.name, "%", arrayOf("TABLE", "VIEW")).use { rows ->
          buildList {
            while (rows.next()) {
              val kind =
                if (rows.getString("TABLE_TYPE").equals("VIEW", ignoreCase = true)) TableKind.View else TableKind.Table
              add(TableInfo(rows.getString("TABLE_SCHEM") ?: schema.name, rows.getString("TABLE_NAME"), kind))
            }
          }
        }
      } catch (exception: Exception) {
        throw DatabaseErrors.from(exception)
      }
    }

  override suspend fun describeTable(table: TableInfo): TableDetails =
    withJdbc {
      try {
        val catalog = if (config.type == ConnectionType.Sqlite) connection.catalog ?: "main" else null
        val schema = table.schema ?: if (config.type == ConnectionType.Sqlite) "main" else null
        val primaryKeys = mutableSetOf<String>()
        connection.metaData.getPrimaryKeys(catalog, schema, table.name).use { rows ->
          while (rows.next()) primaryKeys.add(rows.getString("COLUMN_NAME"))
        }
        val columns =
          connection.metaData.getColumns(catalog, schema, table.name, "%").use { rows ->
            buildList {
              while (rows.next()) {
                add(
                  ColumnInfo(
                    name = rows.getString("COLUMN_NAME"),
                    typeName = rows.getString("TYPE_NAME") ?: "",
                    nullable = rows.getInt("NULLABLE") != java.sql.DatabaseMetaData.columnNoNulls,
                    defaultValue = rows.getString("COLUMN_DEF")?.let(SqlTextHelper::unquoteSqlStringLiteral),
                    isPrimaryKey = rows.getString("COLUMN_NAME") in primaryKeys,
                  ),
                )
              }
            }
          }
        val indexes =
          connection.metaData.getIndexInfo(catalog, schema, table.name, false, false).use { rows ->
            val grouped = linkedMapOf<String, MutableList<Pair<Int, String>>>()
            val uniqueByName = mutableMapOf<String, Boolean>()
            while (rows.next()) {
              val name = rows.getString("INDEX_NAME") ?: continue
              val column = rows.getString("COLUMN_NAME") ?: continue
              grouped.getOrPut(name) { mutableListOf() }.add(rows.getShort("ORDINAL_POSITION").toInt() to column)
              uniqueByName[name] = !rows.getBoolean("NON_UNIQUE")
            }
            grouped.map { (name, values) ->
              IndexInfo(name, values.sortedBy { it.first }.map { it.second }, uniqueByName.getValue(name))
            }
          }
        val foreignKeys =
          connection.metaData.getImportedKeys(catalog, schema, table.name).use { rows ->
            val grouped = linkedMapOf<String, MutableList<ForeignKeyRow>>()
            while (rows.next()) {
              val key = rows.getString("FK_NAME") ?: rows.getString("PKTABLE_NAME")
              grouped.getOrPut(key) { mutableListOf() }.add(
                ForeignKeyRow(
                  sequence = rows.getShort("KEY_SEQ").toInt(),
                  column = rows.getString("FKCOLUMN_NAME"),
                  referencedColumn = rows.getString("PKCOLUMN_NAME"),
                  referencedSchema = rows.getString("PKTABLE_SCHEM"),
                  referencedTable = rows.getString("PKTABLE_NAME"),
                  name = rows.getString("FK_NAME"),
                ),
              )
            }
            grouped.values.map { values ->
              val ordered = values.sortedBy { it.sequence }
              val first = ordered.first()
              ForeignKeyInfo(
                first.name,
                ordered.map { it.column },
                first.referencedSchema,
                first.referencedTable,
                ordered.map { it.referencedColumn },
              )
            }
          }
        TableDetails(table, columns, indexes, foreignKeys)
      } catch (exception: Exception) {
        throw DatabaseErrors.from(exception)
      }
    }

  override fun previewQuery(
    table: TableInfo,
    limit: Int,
  ): String {
    require(limit > 0) { "Query limit must be positive" }
    val quote = "\""
    val name = SqlTextHelper.quoteIdentifier(table.name, quote)
    val qualifiedName = table.schema?.let { "${SqlTextHelper.quoteIdentifier(it, quote)}.$name" } ?: name
    return "SELECT * FROM $qualifiedName LIMIT $limit"
  }

  override suspend fun execute(
    sql: String,
    fetchSize: Int,
  ): QueryResult =
    withJdbc {
      checkOpen()
      require(fetchSize > 0) { "Fetch size must be positive" }
      var statement: Statement? = null
      try {
        if (config.type == ConnectionType.Postgres) connection.autoCommit = false
        statement = connection.createStatement().apply { setFetchSize(fetchSize) }
        runningStatement = statement
        if (statement.execute(sql)) {
          val resultSet = statement.resultSet
          val cursor =
            JdbcResultCursor(connection, resultSet, statement, jdbcLock) {
              if (config.type == ConnectionType.Postgres) {
                if (!it) {
                  if (!connection.autoCommit) finishPostgresTransaction(commit = false)
                  openPostgresCursors = 0
                } else {
                  openPostgresCursors = (openPostgresCursors - 1).coerceAtLeast(0)
                  if (openPostgresCursors == 0 && !connection.autoCommit) {
                    finishPostgresTransaction(commit = true)
                  }
                }
              }
              if (runningStatement === statement) runningStatement = null
            }
          if (config.type == ConnectionType.Postgres) openPostgresCursors++
          QueryResult.RowSet(cursor)
        } else {
          val count = statement.updateCount.toLong().coerceAtLeast(0)
          if (config.type == ConnectionType.Postgres && openPostgresCursors == 0) {
            finishPostgresTransaction(commit = true)
          }
          runningStatement = null
          QueryResult.Affected(count)
        }
      } catch (exception: Exception) {
        statement?.close()
        runningStatement = null
        if (config.type == ConnectionType.Postgres) {
          if (!connection.autoCommit) finishPostgresTransaction(commit = false)
          openPostgresCursors = 0
        }
        throw DatabaseErrors.from(exception)
      }
    }

  override suspend fun cancel() =
    withContext(Dispatchers.IO) {
      try {
        runningStatement?.cancel()
      } catch (exception: SQLException) {
        throw DatabaseErrors.from(exception)
      }
      Unit
    }

  override fun close() {
    closed = true
    runningStatement?.cancel()
    jdbcLock.lock()
    try {
      connection.close()
    } finally {
      jdbcLock.unlock()
    }
  }

  private fun checkOpen() {
    if (closed || connection.isClosed) {
      throw DatabaseException(DatabaseErrorKind.Other, "Database session is closed")
    }
  }

  private suspend fun <T> withJdbc(action: () -> T): T =
    withContext(Dispatchers.IO) {
      jdbcLock.lock()
      try {
        action()
      } finally {
        jdbcLock.unlock()
      }
    }

  private fun finishPostgresTransaction(commit: Boolean) {
    try {
      if (commit) connection.commit() else connection.rollback()
    } finally {
      connection.autoCommit = true
    }
  }

  private data class ForeignKeyRow(
    val sequence: Int,
    val column: String,
    val referencedColumn: String,
    val referencedSchema: String?,
    val referencedTable: String,
    val name: String?,
  )
}
