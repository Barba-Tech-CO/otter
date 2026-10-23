package co.barbatech.otter.connectionstore

/** Non persistent store used until the JSON store exists, and in tests. */
class InMemoryConnectionStore : ConnectionStore {
  private val items = LinkedHashMap<String, ConnectionConfig>()

  override suspend fun list(): List<ConnectionConfig> = items.values.toList()

  override suspend fun save(config: ConnectionConfig) {
    items[config.id] = config
  }

  override suspend fun delete(id: String) {
    items.remove(id)
  }
}
