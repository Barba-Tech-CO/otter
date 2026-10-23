package co.barbatech.otter.connectionstore

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Database engines otter can connect to. [defaultPort] is null for file based engines. */
@Serializable
enum class ConnectionType(
  val defaultPort: Int?,
) {
  @SerialName("SQLITE")
  Sqlite(null),

  @SerialName("POSTGRES")
  Postgres(5432),
  ;

  /** Name shown to the user. */
  val displayName: String
    get() =
      when (this) {
        Sqlite -> "SQLite"
        Postgres -> "PostgreSQL"
      }
}
