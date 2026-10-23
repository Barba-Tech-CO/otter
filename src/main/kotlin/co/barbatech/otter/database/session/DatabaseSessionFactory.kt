package co.barbatech.otter.database.session

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.database.error.DatabaseException

/**
 * Opens sessions. A future SSH tunnel is a wrapper: it starts the tunnel, then calls [open]
 * with a copy of the config whose host and port point to the local loopback port.
 */
interface DatabaseSessionFactory {
  /** [password] is null for SQLite or passwordless logins. Throws [DatabaseException]. */
  suspend fun open(
    config: ConnectionConfig,
    password: String?,
  ): DatabaseSession
}
