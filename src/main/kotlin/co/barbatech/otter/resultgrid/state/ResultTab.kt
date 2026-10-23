package co.barbatech.otter.resultgrid.state

import co.barbatech.otter.database.result.QueryResult
import java.time.Duration

/**
 * One statement's outcome, shown as a result tab. [result] is a failure when the statement errored.
 * [rerun] executes the statement again (used by "export all rows").
 */
data class ResultTab(
  val sql: String,
  val duration: Duration,
  val result: Result<QueryResult>,
  val rerun: suspend () -> QueryResult,
)
