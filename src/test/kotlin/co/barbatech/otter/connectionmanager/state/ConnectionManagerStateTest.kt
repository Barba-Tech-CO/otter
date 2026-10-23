package co.barbatech.otter.connectionmanager.state

import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionStore
import co.barbatech.otter.connectionstore.ConnectionType
import co.barbatech.otter.connectionstore.InMemoryConnectionStore
import co.barbatech.otter.database.error.DatabaseErrorKind
import co.barbatech.otter.database.error.DatabaseException
import co.barbatech.otter.database.session.DatabaseSessionFactory
import co.barbatech.otter.secretstore.InMemorySecretStore
import co.barbatech.otter.secretstore.SecretStore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConnectionManagerStateTest {
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

  @Test
  fun savePersistsTheConfigAndKeepsThePasswordOutOfIt() =
    runTest {
      val store = InMemoryConnectionStore()
      val secrets = InMemorySecretStore()
      val state = createState(store = store, secrets = secrets)
      advanceUntilIdle()

      state.save(postgres, "s3cret")
      advanceUntilIdle()

      assertEquals(listOf(postgres), store.list())
      assertEquals(listOf(postgres), state.connections.value)
      assertEquals("s3cret", secrets.get(postgres.id))
    }

  @Test
  fun saveWithoutSavePasswordRemovesTheStoredSecret() =
    runTest {
      val secrets = InMemorySecretStore()
      val state = createState(secrets = secrets)
      advanceUntilIdle()
      secrets.set(postgres.id, "old")

      state.save(postgres.copy(savePassword = false), null)
      advanceUntilIdle()

      assertNull(secrets.get(postgres.id))
      assertEquals(
        false,
        state.connections.value
          .single()
          .savePassword,
      )
    }

  @Test
  fun openUsesTheStoredPasswordAndBecomesActive() =
    runTest {
      val factory = FakeSessionFactory()
      val state = createState(sessions = factory)
      advanceUntilIdle()
      state.save(postgres, "s3cret")
      advanceUntilIdle()

      state.open(postgres.id)
      advanceUntilIdle()

      assertEquals(listOf<String?>("s3cret"), factory.passwords)
      assertNotNull(state.activeSession.value)
      assertEquals(setOf(postgres.id), state.openIds.value)
    }

  @Test
  fun openPromptsWhenTheUserChosePromptEachTime() =
    runTest {
      val secrets = InMemorySecretStore()
      val factory = FakeSessionFactory()
      val state = createState(secrets = secrets, sessions = factory)
      advanceUntilIdle()
      state.save(postgres.copy(savePassword = false), null)
      advanceUntilIdle()

      state.open(postgres.id)
      advanceUntilIdle()
      assertNotNull(state.passwordRequest.value)
      assertNull(state.activeSession.value)
      assertTrue(factory.passwords.isEmpty())

      state.continueWithPassword("typed")
      advanceUntilIdle()

      assertNull(state.passwordRequest.value)
      assertEquals(listOf<String?>("typed"), factory.passwords)
      assertNotNull(state.activeSession.value)
      assertNull(secrets.get(postgres.id))
    }

  @Test
  fun openPromptsWhenTheKeychainIsUnavailable() =
    runTest {
      val state = createState(secrets = UnavailableSecretStore())
      advanceUntilIdle()
      state.save(postgres, null)
      advanceUntilIdle()

      state.open(postgres.id)
      advanceUntilIdle()
      assertNotNull(state.passwordRequest.value)
      assertNull(state.activeSession.value)

      state.continueWithPassword("typed")
      advanceUntilIdle()
      assertNotNull(state.activeSession.value)
      assertTrue(
        state.status.value
          ?.text
          ?.contains("not saved") == true,
      )
    }

  @Test
  fun cancellingThePasswordPromptLeavesTheConnectionClosed() =
    runTest {
      val state = createState()
      advanceUntilIdle()
      state.save(postgres.copy(savePassword = false), null)
      advanceUntilIdle()
      state.open(postgres.id)
      advanceUntilIdle()

      state.cancelPasswordPrompt()
      advanceUntilIdle()

      assertNull(state.passwordRequest.value)
      assertNull(state.activeSession.value)
      assertTrue(state.status.value?.isError == true)
    }

  @Test
  fun openingAnOpenConnectionOnlyActivatesIt() =
    runTest {
      val factory = FakeSessionFactory()
      val state = createState(sessions = factory)
      advanceUntilIdle()
      state.save(postgres, "s3cret")
      advanceUntilIdle()
      state.open(postgres.id)
      advanceUntilIdle()
      val first = state.activeSession.value

      state.open(postgres.id)
      advanceUntilIdle()

      assertEquals(1, factory.opened.size)
      assertSame(first, state.activeSession.value)
    }

  @Test
  fun closeReleasesTheSessionAndClearsTheActiveOne() =
    runTest {
      val factory = FakeSessionFactory()
      val store = InMemoryConnectionStore()
      val state = createState(store = store, sessions = factory)
      advanceUntilIdle()
      state.save(postgres, "s3cret")
      advanceUntilIdle()
      state.open(postgres.id)
      advanceUntilIdle()
      assertNotNull(state.activeSession.value)

      state.close(postgres.id)
      advanceUntilIdle()

      assertTrue(factory.opened.single().closed)
      assertNull(state.activeSession.value)
      assertEquals(emptySet(), state.openIds.value)
      assertEquals(listOf(postgres), store.list())
    }

  @Test
  fun deleteRemovesTheConfigTheSecretAndClosesTheSession() =
    runTest {
      val store = InMemoryConnectionStore()
      val secrets = InMemorySecretStore()
      val factory = FakeSessionFactory()
      val state = createState(store = store, secrets = secrets, sessions = factory)
      advanceUntilIdle()
      state.save(postgres, "s3cret")
      advanceUntilIdle()
      state.open(postgres.id)
      advanceUntilIdle()

      state.delete(postgres.id)
      advanceUntilIdle()

      assertEquals(emptyList(), store.list())
      assertNull(secrets.get(postgres.id))
      assertTrue(factory.opened.single().closed)
      assertNull(state.activeSession.value)
      assertEquals(emptySet(), state.openIds.value)
    }

  @Test
  fun duplicateCopiesTheConfigAndTheSecret() =
    runTest {
      val secrets = InMemorySecretStore()
      val state = createState(secrets = secrets)
      advanceUntilIdle()
      state.save(postgres, "s3cret")
      advanceUntilIdle()

      state.duplicate(postgres.id)
      advanceUntilIdle()

      val copy = state.connections.value.first { it.id != postgres.id }
      assertEquals("Local PG copy", copy.name)
      assertEquals("s3cret", secrets.get(copy.id))
      assertEquals(2, state.connections.value.size)
    }

  @Test
  fun testSucceedsAndClosesTheSessionItOpened() =
    runTest {
      val factory = FakeSessionFactory()
      val state = createState(sessions = factory)
      advanceUntilIdle()

      state.test(postgres, "s3cret")
      advanceUntilIdle()

      val result = assertNotNull(state.testResult.value)
      assertTrue(result.success)
      assertNull(result.kind)
      assertTrue(result.message.contains("succeeded"))
      assertTrue(factory.opened.single().closed)
      assertNull(state.activeSession.value)
    }

  @Test
  fun testReportsTheSpecificFailureKind() =
    runTest {
      val factory =
        FakeSessionFactory(
          failure = DatabaseException(DatabaseErrorKind.AuthenticationFailed, "password authentication failed"),
        )
      val state = createState(sessions = factory)
      advanceUntilIdle()

      state.test(postgres, "wrong")
      advanceUntilIdle()

      val result = assertNotNull(state.testResult.value)
      assertFalse(result.success)
      assertEquals(DatabaseErrorKind.AuthenticationFailed, result.kind)
      assertTrue(result.message.contains("authentication failed"))
    }

  @Test
  fun testReportsATimeoutAsUnreachable() =
    runTest {
      val state = createState(sessions = HangingSessionFactory(), timeout = 500.milliseconds)
      advanceUntilIdle()

      state.test(postgres, "s3cret")
      advanceUntilIdle()

      val result = assertNotNull(state.testResult.value)
      assertFalse(result.success)
      assertEquals(DatabaseErrorKind.Unreachable, result.kind)
      assertTrue(result.message.contains("timed out"))
    }

  private fun TestScope.createState(
    store: ConnectionStore = InMemoryConnectionStore(),
    secrets: SecretStore = InMemorySecretStore(),
    sessions: DatabaseSessionFactory = FakeSessionFactory(),
    timeout: Duration = 5.seconds,
  ): ConnectionManagerState =
    ConnectionManagerState(
      store = store,
      secrets = secrets,
      sessions = sessions,
      scope = this,
      connectTimeout = timeout,
      ioDispatcher = StandardTestDispatcher(testScheduler),
    )
}
