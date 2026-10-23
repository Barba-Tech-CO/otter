package co.barbatech.otter.database.result

/** Outcome of one executed statement. */
sealed interface QueryResult {
  /** The statement produced a result set, read lazily from [cursor]. */
  data class RowSet(
    val cursor: ResultCursor,
  ) : QueryResult

  /** The statement produced no result set (INSERT, UPDATE, DDL...). */
  data class Affected(
    val rowCount: Long,
  ) : QueryResult
}
