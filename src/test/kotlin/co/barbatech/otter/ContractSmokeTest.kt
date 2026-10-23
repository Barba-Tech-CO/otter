package co.barbatech.otter

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionType
import co.barbatech.otter.connectionstore.InMemoryConnectionStore
import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.store.InMemoryHistoryStore
import kotlinx.coroutines.test.runTest
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ContractSmokeTest {
  @Test
  fun connectionStoreSavesAndDeletes() =
    runTest {
      val store = InMemoryConnectionStore()
      val config =
        ConnectionConfig(id = "1", name = "local", type = ConnectionType.Postgres, host = "localhost", port = 5432)
      store.save(config)
      assertEquals(listOf(config), store.list())
      store.delete("1")
      assertEquals(emptyList(), store.list())
    }

  @Test
  fun historyStoreFindsByText() =
    runTest {
      val store = InMemoryHistoryStore()
      store.record(HistoryEntry("local", "select 1", Instant.now(), Duration.ofMillis(3), success = true))
      assertEquals(1, store.search("SELECT", 10).size)
    }

  @Test
  fun nullCellIsNotEmptyString() {
    assertNotEquals<CellValue>(CellValue.Null, CellValue.Text(""))
  }
}
