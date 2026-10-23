package co.barbatech.otter.queryhistory.model

import java.time.Duration
import java.time.Instant

/** One executed statement. [error] is set when [success] is false. */
data class HistoryEntry(
  val connectionName: String,
  val sql: String,
  val executedAt: Instant,
  val duration: Duration,
  val success: Boolean,
  val error: String? = null,
)
