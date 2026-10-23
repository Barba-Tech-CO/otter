package co.barbatech.otter.connectionmanager.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionType

internal class ConnectionFormState(
  initial: ConnectionConfig?,
) {
  var type by mutableStateOf(initial?.type ?: ConnectionType.Postgres)
  var name by mutableStateOf(initial?.name ?: "")
  var host by mutableStateOf(initial?.host ?: "localhost")
  var port by mutableStateOf((initial?.port ?: initial?.type?.defaultPort)?.toString() ?: "")
  var database by mutableStateOf(initial?.database ?: "")
  var user by mutableStateOf(initial?.user ?: "")
  var filePath by mutableStateOf(initial?.filePath ?: "")
  var password by mutableStateOf("")
  var savePassword by mutableStateOf(initial?.savePassword ?: true)

  fun changeType(newType: ConnectionType) {
    if (type == newType) return
    val wasDefault = port == type.defaultPort?.toString()
    type = newType
    if (port.isBlank() || wasDefault) port = newType.defaultPort?.toString() ?: ""
  }

  val nameError: String?
    get() = if (name.isBlank()) "Name is required." else null

  val hostError: String?
    get() = if (host.isBlank()) "Host is required." else null

  val portError: String?
    get() {
      val number = port.toIntOrNull() ?: return "Port must be a number."
      return if (number in 1..65535) null else "Port must be between 1 and 65535."
    }

  val databaseError: String?
    get() = if (database.isBlank()) "Database is required." else null

  val userError: String?
    get() = if (user.isBlank()) "User is required." else null

  val filePathError: String?
    get() = if (filePath.isBlank()) "File path is required." else null

  val isValid: Boolean
    get() =
      when (type) {
        ConnectionType.Sqlite -> {
          nameError == null && filePathError == null
        }

        ConnectionType.Postgres -> {
          nameError == null && hostError == null && portError == null && databaseError == null && userError == null
        }
      }

  fun toConfig(id: String): ConnectionConfig =
    when (type) {
      ConnectionType.Sqlite -> {
        ConnectionConfig(
          id = id,
          name = name.trim(),
          type = type,
          filePath = filePath.trim(),
          savePassword = false,
        )
      }

      ConnectionType.Postgres -> {
        ConnectionConfig(
          id = id,
          name = name.trim(),
          type = type,
          host = host.trim(),
          port = port.toIntOrNull(),
          database = database.trim(),
          user = user.trim(),
          savePassword = savePassword,
        )
      }
    }
}
