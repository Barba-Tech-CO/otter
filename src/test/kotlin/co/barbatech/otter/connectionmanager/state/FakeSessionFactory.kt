package co.barbatech.otter.connectionmanager.state

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.database.error.DatabaseException
import co.barbatech.otter.database.session.DatabaseSession
import co.barbatech.otter.database.session.DatabaseSessionFactory

internal class FakeSessionFactory(
  private val failure: DatabaseException? = null,
) : DatabaseSessionFactory {
  val passwords = mutableListOf<String?>()
  val opened = mutableListOf<FakeDatabaseSession>()

  override suspend fun open(
    config: ConnectionConfig,
    password: String?,
  ): DatabaseSession {
    failure?.let { throw it }
    passwords += password
    val session = FakeDatabaseSession(config)
    opened += session
    return session
  }
}
