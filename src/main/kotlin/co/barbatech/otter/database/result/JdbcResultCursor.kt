package co.barbatech.otter.database.result

import co.barbatech.otter.database.error.DatabaseErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.util.concurrent.locks.ReentrantLock

/** Incremental JDBC cursor; closing it releases both JDBC resources and its PostgreSQL transaction. */
internal class JdbcResultCursor(
  private val connection: Connection,
  private val resultSet: ResultSet,
  private val statement: Statement,
  private val jdbcLock: ReentrantLock,
  private val onClose: (Boolean) -> Unit,
) : ResultCursor {
  override val columns: List<ResultColumn> =
    resultSet.metaData.let { metadata ->
      (1..metadata.columnCount).map { index ->
        ResultColumn(metadata.getColumnLabel(index), metadata.getColumnTypeName(index) ?: "")
      }
    }

  @Volatile override var hasMore: Boolean = true
    private set
  private var bufferedRow: Row? = null
  private var checkedForNext = false

  @Volatile private var closed = false

  override suspend fun fetch(count: Int): List<Row> =
    withContext(Dispatchers.IO) {
      require(count > 0) { "Fetch count must be positive" }
      jdbcLock.lock()
      try {
        check(!closed) { "Result cursor is closed" }
        val rows = ArrayList<Row>(count)
        while (rows.size < count && hasMore) {
          rows.add(nextRow() ?: break)
        }
        if (rows.size == count) {
          bufferedRow = readNextRow()
          checkedForNext = true
          hasMore = bufferedRow != null
        }
        rows
      } catch (exception: Exception) {
        closeResources(commit = false)
        throw DatabaseErrors.from(exception)
      } finally {
        jdbcLock.unlock()
      }
    }

  override fun close() {
    closeResources(commit = true)
  }

  private fun nextRow(): Row? {
    if (checkedForNext) {
      checkedForNext = false
      return bufferedRow.also { bufferedRow = null }
    }
    return readNextRow()
  }

  private fun readNextRow(): Row? {
    if (!resultSet.next()) {
      hasMore = false
      return null
    }
    val cells =
      (1..columns.size).map { index ->
        when (val value = resultSet.getObject(index)) {
          null -> CellValue.Null
          is ByteArray -> CellValue.Binary(value)
          is BigDecimal -> CellValue.Numeric(value)
          is java.math.BigInteger -> CellValue.Numeric(value.toBigDecimal())
          is Number -> CellValue.Numeric(value.toString().toBigDecimal())
          is Boolean -> CellValue.Bool(value)
          else -> CellValue.Text(value.toString())
        }
      }
    return Row(cells)
  }

  private fun closeResources(commit: Boolean) {
    jdbcLock.lock()
    try {
      releaseResources(commit)
    } finally {
      jdbcLock.unlock()
    }
  }

  private fun releaseResources(commit: Boolean) {
    if (closed) return
    closed = true
    hasMore = false
    var failure: Exception? = null
    try {
      resultSet.close()
    } catch (exception: Exception) {
      failure = exception
    }
    try {
      statement.close()
    } catch (exception: Exception) {
      failure = failure ?: exception
    }
    try {
      onClose(commit && failure == null)
    } catch (exception: Exception) {
      failure = failure ?: exception
    }
    if (failure != null) throw DatabaseErrors.from(failure)
  }
}
