package co.barbatech.otter.database.result

/**
 * Rows of one result set, read on demand (JDBC fetch size, never LIMIT rewriting).
 * Call [close] when done; it releases the statement.
 */
interface ResultCursor : AutoCloseable {
  val columns: List<ResultColumn>

  /** False once the last row was returned. */
  val hasMore: Boolean

  /** Returns up to [count] rows; fewer (or none) means the end was reached. */
  suspend fun fetch(count: Int): List<Row>

  override fun close()
}
