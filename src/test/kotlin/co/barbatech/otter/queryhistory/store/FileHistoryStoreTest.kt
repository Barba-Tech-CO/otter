package co.barbatech.otter.queryhistory.store

import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.model.HistorySettings
import co.barbatech.otter.queryhistory.store.FileHistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileHistoryStoreTest {
  @Test
  fun `records newest first and searches sql or connection`() =
    runTest {
      val store = store(newDirectory())
      store.record(entry("select 1"))
      store.record(entry("select 2", at = Instant.parse("2026-10-23T10:01:00Z")))
      store.record(entry("update users", connection = "prod", at = Instant.parse("2026-10-23T10:02:00Z")))

      assertEquals(listOf("update users", "select 2", "select 1"), store.search("", 10).map { it.sql })
      assertEquals(listOf("select 1", "select 2"), store.search("SELECT", 10).map { it.sql }.sorted())
      assertEquals(listOf("update users"), store.search("prod", 10).map { it.sql })
    }

  @Test
  fun `entries and settings persist across instances`() =
    runTest {
      val directory = newDirectory()
      val first = store(directory)
      first.updateSettings(HistorySettings(enabled = true, maxEntries = 5))
      first.record(entry("select 1"))
      first.record(entry("select 2"))

      val second = store(directory)
      assertEquals(listOf("select 2", "select 1"), second.search("", 10).map { it.sql })
      assertEquals(5, second.settings.value.maxEntries)
      assertEquals(null, second.lastError.value)
    }

  @Test
  fun `drops oldest entries beyond the bound`() =
    runTest {
      val directory = newDirectory()
      val store = store(directory)
      store.updateSettings(HistorySettings(enabled = true, maxEntries = 2))
      store.record(entry("one"))
      store.record(entry("two", at = Instant.parse("2026-10-23T10:01:00Z")))
      store.record(entry("three", at = Instant.parse("2026-10-23T10:02:00Z")))

      assertEquals(listOf("three", "two"), store.search("", 10).map { it.sql })
      assertEquals(listOf("three", "two"), store(directory).search("", 10).map { it.sql })
    }

  @Test
  fun `loading before a settings update keeps the persisted bound`() =
    runTest {
      val directory = newDirectory()
      store(directory).updateSettings(HistorySettings(enabled = true, maxEntries = 5))

      val second = store(directory)
      second.load()
      second.updateSettings(second.settings.value.copy(enabled = false))

      val third = store(directory)
      third.load()
      assertEquals(5, third.settings.value.maxEntries)
      assertEquals(false, third.settings.value.enabled)
    }

  @Test
  fun `disabled recording stores nothing and stays disabled after restart`() =
    runTest {
      val directory = newDirectory()
      val store = store(directory)
      store.updateSettings(HistorySettings(enabled = false, maxEntries = 10))
      store.record(entry("select secret"))

      assertTrue(store.search("", 10).isEmpty())
      val reopened = store(directory)
      assertTrue(reopened.search("", 10).isEmpty())
      assertEquals(false, reopened.settings.value.enabled)
    }

  @Test
  fun `clear removes entries from disk`() =
    runTest {
      val directory = newDirectory()
      val store = store(directory)
      store.record(entry("select 1"))
      store.clear()

      assertTrue(store.search("", 10).isEmpty())
      assertTrue(store(directory).search("", 10).isEmpty())
    }

  @Test
  fun `corrupt entry lines are skipped backed up and reported`() =
    runTest {
      val directory = newDirectory()
      val first = store(directory)
      first.record(entry("select 1"))
      Files.writeString(directory.resolve("query-history.jsonl"), "not json\n", UTF_8, StandardOpenOption.APPEND)

      val second = store(directory)

      assertEquals(listOf("select 1"), second.search("", 10).map { it.sql })
      assertNotNull(second.lastError.value)
      val backupExists =
        Files.newDirectoryStream(directory).use { stream ->
          stream.any { it.fileName.toString().contains(".corrupt-") }
        }
      assertTrue(backupExists)
    }

  @Test
  fun `corrupt settings fall back to defaults with an error`() =
    runTest {
      val directory = newDirectory()
      Files.createDirectories(directory)
      Files.writeString(directory.resolve("history-settings.json"), "{not json", UTF_8)

      val store = store(directory)
      store.search("", 10)

      assertEquals(HistorySettings(), store.settings.value)
      assertTrue(
        store.lastError.value
          .orEmpty()
          .contains("settings"),
      )
    }

  private fun newDirectory(): Path = Files.createTempDirectory("history-test")

  private fun store(directory: Path): FileHistoryStore = FileHistoryStore(directory, Dispatchers.Unconfined)

  private fun entry(
    sql: String,
    connection: String = "local",
    at: Instant = Instant.parse("2026-10-23T10:00:00Z"),
    success: Boolean = true,
  ): HistoryEntry = HistoryEntry(connection, sql, at, Duration.ofMillis(12), success)
}
