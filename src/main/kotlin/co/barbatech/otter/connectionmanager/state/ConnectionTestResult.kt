package co.barbatech.otter.connectionmanager.state

import co.barbatech.otter.database.error.DatabaseErrorKind

/** Outcome of "Test connection": a success message, or the failure kind and reason. */
data class ConnectionTestResult(
  val success: Boolean,
  val kind: DatabaseErrorKind? = null,
  val message: String,
)
