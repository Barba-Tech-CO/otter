package co.barbatech.otter.connectionmanager.state

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.database.session.DatabaseSession
import co.barbatech.otter.database.session.DatabaseSessionFactory
import kotlinx.coroutines.awaitCancellation

internal class HangingSessionFactory : DatabaseSessionFactory {
  override suspend fun open(
    config: ConnectionConfig,
    password: String?,
  ): DatabaseSession = awaitCancellation()
}
