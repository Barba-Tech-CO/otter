package co.barbatech.otter.connectionstore

/** Persists connection configs (without secrets) in the OS config directory. */
interface ConnectionStore {
  suspend fun list(): List<ConnectionConfig>

  /** Inserts the config, or replaces the one with the same id. */
  suspend fun save(config: ConnectionConfig)

  suspend fun delete(id: String)
}
