package co.barbatech.otter.connectionmanager.state

import co.barbatech.otter.secretstore.SecretStore
import co.barbatech.otter.secretstore.SecretStoreUnavailableException

internal class UnavailableSecretStore : SecretStore {
  override suspend fun get(connectionId: String): String? = throw SecretStoreUnavailableException("no keychain")

  override suspend fun set(
    connectionId: String,
    secret: String,
  ): Unit = throw SecretStoreUnavailableException("no keychain")

  override suspend fun delete(connectionId: String): Unit = throw SecretStoreUnavailableException("no keychain")
}
