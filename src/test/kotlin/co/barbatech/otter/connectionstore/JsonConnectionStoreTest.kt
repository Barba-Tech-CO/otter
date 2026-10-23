package co.barbatech.otter.connectionstore

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsonConnectionStoreTest {
  private lateinit var directory: Path

  private val postgres =
    ConnectionConfig(
      id = "pg",
      name = "Local PG",
      type = ConnectionType.Postgres,
      host = "localhost",
      port = 5432,
      database = "app",
      user = "dev",
    )
  private val sqlite =
    ConnectionConfig(
      id = "lite",
      name = "Local file",
      type = ConnectionType.Sqlite,
      filePath = "/tmp/app.db",
      savePassword = false,
    )

  @BeforeEach
  fun setUp() {
    directory = Files.createTempDirectory("otter-connectionstore")
  }

  @AfterEach
  fun tearDown() {
    directory.toFile().deleteRecursively()
  }

  @Test
  fun savesAndListsConnectionsAcrossInstances() =
    runTest {
      val file = directory.resolve("connections.json")
      JsonConnectionStore(file).save(postgres)
      JsonConnectionStore(file).save(sqlite)

      assertEquals(listOf(postgres, sqlite), JsonConnectionStore(file).list())
      assertTrue(Files.exists(file))
    }

  @Test
  fun replacesTheConfigWithTheSameId() =
    runTest {
      val store = JsonConnectionStore(directory.resolve("connections.json"))
      store.save(postgres)
      store.save(postgres.copy(name = "Renamed", port = 5433))

      assertEquals(listOf(postgres.copy(name = "Renamed", port = 5433)), store.list())
    }

  @Test
  fun deletingRemovesOnlyThatConnection() =
    runTest {
      val store = JsonConnectionStore(directory.resolve("connections.json"))
      store.save(postgres)
      store.save(sqlite)
      store.delete(postgres.id)

      assertEquals(listOf(sqlite), store.list())
    }

  @Test
  fun missingFileIsAnEmptyList() =
    runTest {
      assertEquals(emptyList(), JsonConnectionStore(directory.resolve("absent.json")).list())
    }

  @Test
  fun missingFileIsNotCreatedByDelete() =
    runTest {
      val file = directory.resolve("absent.json")
      JsonConnectionStore(file).delete("anything")

      assertFalse(Files.exists(file))
    }

  @Test
  fun corruptFileRaisesAClearErrorAndIsNotOverwritten() =
    runTest {
      val file = directory.resolve("connections.json")
      Files.writeString(file, "{ this is not json")
      val store = JsonConnectionStore(file)

      assertIs<ConnectionStoreException>(runCatching { store.list() }.exceptionOrNull())
      assertIs<ConnectionStoreException>(runCatching { store.save(postgres) }.exceptionOrNull())
      assertEquals("{ this is not json", Files.readString(file))
    }

  @Test
  fun writesLeaveNoTemporaryFiles() =
    runTest {
      val file = directory.resolve("connections.json")
      val store = JsonConnectionStore(file)
      store.save(postgres)
      store.save(sqlite)

      val names =
        Files.newDirectoryStream(directory).use { stream -> stream.map { it.fileName.toString() } }
      assertEquals(listOf("connections.json"), names)
      assertEquals(2, JsonConnectionStore(file).list().size)
    }

  @Test
  fun fileContainsNoSecretField() =
    runTest {
      val file = directory.resolve("connections.json")
      JsonConnectionStore(file).save(postgres.copy(savePassword = true))

      val text = Files.readString(file)
      assertFalse(text.lowercase().contains("\"password\":"))
      assertTrue(text.contains("\"savePassword\""))
    }

  @Test
  fun readsLegacyTypeSpellingsFromDisk() =
    runTest {
      val file = directory.resolve("connections.json")
      Files.writeString(
        file,
        """
        [
          {"id": "pg", "name": "Legacy PG", "type": "POSTGRES"},
          {"id": "lite", "name": "Legacy file", "type": "SQLITE"}
        ]
        """.trimIndent(),
      )

      val loaded = JsonConnectionStore(file).list()

      assertEquals(listOf(ConnectionType.Postgres, ConnectionType.Sqlite), loaded.map { it.type })
    }
}
