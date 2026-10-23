package co.barbatech.otter.database.session

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionType
import co.barbatech.otter.database.error.DatabaseErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.DriverManager

/** Opens JDBC sessions for SQLite and PostgreSQL. */
class JdbcDatabaseSessionFactory : DatabaseSessionFactory {
  override suspend fun open(
    config: ConnectionConfig,
    password: String?,
  ): DatabaseSession =
    withContext(Dispatchers.IO) {
      try {
        val connection =
          when (config.type) {
            ConnectionType.Sqlite -> {
              val path = requireNotNull(config.filePath) { "SQLite file path is required" }
              DriverManager.getConnection("jdbc:sqlite:$path")
            }

            ConnectionType.Postgres -> {
              val host = requireNotNull(config.host) { "PostgreSQL host is required" }
              val port = config.port ?: ConnectionType.Postgres.defaultPort
              val database = requireNotNull(config.database) { "PostgreSQL database is required" }
              val properties =
                java.util.Properties().apply {
                  setProperty("connectTimeout", "10")
                  config.user?.let { setProperty("user", it) }
                  password?.let { setProperty("password", it) }
                }
              DriverManager.getConnection("jdbc:postgresql://$host:$port/$database", properties)
            }
          }
        JdbcDatabaseSession(config, connection)
      } catch (exception: Exception) {
        throw DatabaseErrors.from(exception)
      }
    }
}
