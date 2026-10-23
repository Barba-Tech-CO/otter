package co.barbatech.otter.secretstore

/**
 * Keeps one secret per connection id in the OS keychain.
 * Throws [SecretStoreUnavailableException] when no keychain exists; never falls back to plain text.
 */
interface SecretStore {
  suspend fun get(connectionId: String): String?

  suspend fun set(
    connectionId: String,
    secret: String,
  )

  suspend fun delete(connectionId: String)
}
