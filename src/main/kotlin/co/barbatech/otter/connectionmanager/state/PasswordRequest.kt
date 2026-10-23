package co.barbatech.otter.connectionmanager.state

/** Asks the UI to collect a password for [connectionName] before opening [connectionId]. */
data class PasswordRequest(
  val connectionId: String,
  val connectionName: String,
)
