package co.barbatech.otter.secretstore

/** Secrets held in memory only (lost on exit). Placeholder until the keychain store exists, and for tests. */
class InMemorySecretStore : SecretStore {
  private val secrets = HashMap<String, String>()

  override suspend fun get(connectionId: String): String? = secrets[connectionId]

  override suspend fun set(
    connectionId: String,
    secret: String,
  ) {
    secrets[connectionId] = secret
  }

  override suspend fun delete(connectionId: String) {
    secrets.remove(connectionId)
  }
}
