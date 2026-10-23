package co.barbatech.otter.connectionstore

import kotlinx.serialization.Serializable

/**
 * A saved connection. Never holds a password: secrets live in the SecretStore under [id].
 * SQLite uses [filePath] only; the other engines use host, port, database and user.
 * [savePassword] false means "prompt each time".
 */
@Serializable
data class ConnectionConfig(
  val id: String,
  val name: String,
  val type: ConnectionType,
  val host: String? = null,
  val port: Int? = null,
  val database: String? = null,
  val user: String? = null,
  val filePath: String? = null,
  val savePassword: Boolean = true,
)
