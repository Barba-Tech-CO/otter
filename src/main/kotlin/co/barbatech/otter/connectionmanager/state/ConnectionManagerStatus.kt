package co.barbatech.otter.connectionmanager.state

/** A short status line for the connection list; [isError] picks the color in the UI. */
data class ConnectionManagerStatus(
  val text: String,
  val isError: Boolean = false,
)
