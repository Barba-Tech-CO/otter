package co.barbatech.otter.connectionmanager.state

import co.barbatech.otter.connectionmanager.helper.ConnectionErrorTextHelper
import co.barbatech.otter.connectionstore.ConnectionConfig
import co.barbatech.otter.connectionstore.ConnectionStore
import co.barbatech.otter.connectionstore.ConnectionType
import co.barbatech.otter.database.error.DatabaseErrorKind
import co.barbatech.otter.database.error.DatabaseException
import co.barbatech.otter.database.session.DatabaseSession
import co.barbatech.otter.database.session.DatabaseSessionFactory
import co.barbatech.otter.secretstore.SecretStore
import co.barbatech.otter.secretstore.SecretStoreUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * UI state of the connection list. Other components only read [activeSession].
 * Passwords go to [secrets], never to [store].
 */
class ConnectionManagerState(
  private val store: ConnectionStore,
  private val secrets: SecretStore,
  private val sessions: DatabaseSessionFactory,
  private val scope: CoroutineScope,
  private val connectTimeout: Duration = 10.seconds,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private val _connections = MutableStateFlow<List<ConnectionConfig>>(emptyList())
  val connections: StateFlow<List<ConnectionConfig>> = _connections.asStateFlow()

  /** The session that the schema browser and the editor work on; null when nothing is open. */
  private val _activeSession = MutableStateFlow<DatabaseSession?>(null)
  val activeSession: StateFlow<DatabaseSession?> = _activeSession.asStateFlow()

  private val _openIds = MutableStateFlow<Set<String>>(emptySet())
  val openIds: StateFlow<Set<String>> = _openIds.asStateFlow()

  private val _busyIds = MutableStateFlow<Set<String>>(emptySet())
  val busyIds: StateFlow<Set<String>> = _busyIds.asStateFlow()

  private val _status = MutableStateFlow<ConnectionManagerStatus?>(null)
  val status: StateFlow<ConnectionManagerStatus?> = _status.asStateFlow()

  private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
  val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()

  private val _passwordRequest = MutableStateFlow<PasswordRequest?>(null)
  val passwordRequest: StateFlow<PasswordRequest?> = _passwordRequest.asStateFlow()

  private val openSessions = LinkedHashMap<String, DatabaseSession>()

  init {
    scope.launch {
      try {
        _connections.value = store.list()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _status.value =
          ConnectionManagerStatus(
            "Could not load the saved connections: ${ConnectionErrorTextHelper.message(e)}",
            isError = true,
          )
      }
    }
  }

  /** Saves the config; [password] goes to the secret store when [ConnectionConfig.savePassword]. */
  fun save(
    config: ConnectionConfig,
    password: String?,
  ) {
    scope.launch {
      markBusy(config.id, true)
      try {
        store.save(config)
        val stored = storeSecretFor(config, password)
        reloadConnections()
        val suffix =
          if (stored) {
            ""
          } else {
            " The password was not saved: no OS keychain is available, so otter will ask for it each time."
          }
        _status.value = ConnectionManagerStatus("Saved '${config.name}'.$suffix")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _status.value =
          ConnectionManagerStatus(
            "Could not save '${config.name}': ${ConnectionErrorTextHelper.message(e)}",
            isError = true,
          )
      } finally {
        markBusy(config.id, false)
      }
    }
  }

  /** Deletes the config and its secret, closing the session if open. */
  fun delete(id: String) {
    scope.launch {
      val config = findConnection(id) ?: return@launch
      closeSession(id)
      try {
        store.delete(id)
        removeSecret(id)
        reloadConnections()
        _status.value = ConnectionManagerStatus("Deleted '${config.name}'.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _status.value =
          ConnectionManagerStatus(
            "Could not delete '${config.name}': ${ConnectionErrorTextHelper.message(e)}",
            isError = true,
          )
      }
    }
  }

  /** Opens a session and makes it active; an already open connection is only made active. */
  fun open(
    id: String,
    password: String? = null,
  ) {
    scope.launch {
      val config = findConnection(id)
      if (config == null) {
        _status.value = ConnectionManagerStatus("That connection no longer exists.", isError = true)
        return@launch
      }
      val alreadyOpen = openSessions[id]
      if (alreadyOpen != null) {
        _activeSession.value = alreadyOpen
        _status.value = ConnectionManagerStatus("'${config.name}' is already open.")
        return@launch
      }
      if (_busyIds.value.contains(id)) return@launch
      when (val outcome = resolvePassword(config, password)) {
        PasswordOutcome.Prompted -> Unit
        is PasswordOutcome.Ready -> openSession(config, outcome.password, password)
      }
    }
  }

  fun close(id: String) {
    scope.launch {
      val config = findConnection(id)
      val closed = closeSession(id)
      _status.value =
        when {
          closed -> ConnectionManagerStatus("Closed '${config?.name ?: id}'.")
          else -> ConnectionManagerStatus("'${config?.name ?: id}' was not open.")
        }
    }
  }

  /** Opens and immediately closes a session, reporting a specific failure reason. */
  fun test(
    config: ConnectionConfig,
    password: String?,
  ) {
    scope.launch {
      _testResult.value = null
      markBusy(config.id, true)
      val startedAt = System.nanoTime()
      try {
        val resolved = password ?: storedPassword(config)
        val session = withTimeout(connectTimeout) { sessions.open(config, resolved) }
        try {
          withContext(ioDispatcher) { session.close() }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          // The test result is about connecting, not about closing.
        }
        val elapsed = (System.nanoTime() - startedAt) / 1_000_000
        _testResult.value = ConnectionTestResult(success = true, message = "Connection succeeded in $elapsed ms.")
      } catch (e: TimeoutCancellationException) {
        _testResult.value =
          ConnectionTestResult(
            success = false,
            kind = DatabaseErrorKind.Unreachable,
            message = ConnectionErrorTextHelper.timeoutMessage(connectTimeout),
          )
      } catch (e: DatabaseException) {
        _testResult.value =
          ConnectionTestResult(success = false, kind = e.kind, message = ConnectionErrorTextHelper.describe(e))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _testResult.value =
          ConnectionTestResult(
            success = false,
            kind = DatabaseErrorKind.Other,
            message = ConnectionErrorTextHelper.message(e),
          )
      } finally {
        markBusy(config.id, false)
      }
    }
  }

  /** Adds a copy of the connection, including its saved password when there is one. */
  fun duplicate(id: String) {
    scope.launch {
      val source = findConnection(id) ?: return@launch
      val copy = source.copy(id = UUID.randomUUID().toString(), name = copyName(source.name))
      try {
        store.save(copy)
        copySecret(source.id, copy)
        reloadConnections()
        _status.value = ConnectionManagerStatus("Duplicated '${source.name}' as '${copy.name}'.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _status.value =
          ConnectionManagerStatus(
            "Could not duplicate '${source.name}': ${ConnectionErrorTextHelper.message(e)}",
            isError = true,
          )
      }
    }
  }

  /** Clears the last status message. */
  fun clearStatus() {
    _status.value = null
  }

  /** Clears the last test result, for example when the form opens. */
  fun clearTestResult() {
    _testResult.value = null
  }

  /** Opens the pending connection with the password typed into the prompt. */
  fun continueWithPassword(password: String) {
    val request = _passwordRequest.value ?: return
    _passwordRequest.value = null
    open(request.connectionId, password)
  }

  /** Dismisses the password prompt without opening anything. */
  fun cancelPasswordPrompt() {
    if (_passwordRequest.value == null) return
    _passwordRequest.value = null
    _status.value = ConnectionManagerStatus("The connection was cancelled because no password was given.", true)
  }

  private suspend fun openSession(
    config: ConnectionConfig,
    password: String?,
    typedPassword: String?,
  ) {
    markBusy(config.id, true)
    try {
      val session = withTimeout(connectTimeout) { sessions.open(config, password) }
      openSessions[config.id] = session
      _openIds.value = openSessions.keys.toSet()
      _activeSession.value = session
      val stored = if (typedPassword != null) storeSecretFor(config, typedPassword) else true
      _status.value =
        when {
          stored -> {
            ConnectionManagerStatus("Connected to '${config.name}'.")
          }

          else -> {
            ConnectionManagerStatus(
              "Connected to '${config.name}'. The password was not saved: no OS keychain is available.",
            )
          }
        }
    } catch (e: TimeoutCancellationException) {
      _status.value =
        ConnectionManagerStatus(
          "Could not connect to '${config.name}': ${ConnectionErrorTextHelper.timeoutMessage(connectTimeout)}",
          isError = true,
        )
    } catch (e: DatabaseException) {
      _status.value =
        ConnectionManagerStatus(
          "Could not connect to '${config.name}': ${ConnectionErrorTextHelper.describe(e)}",
          isError = true,
        )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      _status.value =
        ConnectionManagerStatus(
          "Could not connect to '${config.name}': ${ConnectionErrorTextHelper.message(e)}",
          isError = true,
        )
    } finally {
      markBusy(config.id, false)
    }
  }

  private suspend fun closeSession(id: String): Boolean {
    val session = openSessions.remove(id) ?: return false
    _openIds.value = openSessions.keys.toSet()
    if (_activeSession.value == session) {
      _activeSession.value = openSessions.values.lastOrNull()
    }
    try {
      withContext(ioDispatcher) { session.close() }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      _status.value =
        ConnectionManagerStatus(
          "The database connection could not be closed cleanly: ${ConnectionErrorTextHelper.message(e)}",
          isError = true,
        )
    }
    return true
  }

  private suspend fun resolvePassword(
    config: ConnectionConfig,
    provided: String?,
  ): PasswordOutcome {
    if (config.type == ConnectionType.Sqlite) return PasswordOutcome.Ready(null)
    if (!provided.isNullOrEmpty()) return PasswordOutcome.Ready(provided)
    if (config.savePassword) {
      val stored = readSecret(config.id)
      if (stored != null) return PasswordOutcome.Ready(stored)
    }
    _passwordRequest.value = PasswordRequest(config.id, config.name)
    return PasswordOutcome.Prompted
  }

  private suspend fun storedPassword(config: ConnectionConfig): String? =
    if (config.type == ConnectionType.Sqlite || !config.savePassword) null else readSecret(config.id)

  private suspend fun readSecret(id: String): String? =
    try {
      secrets.get(id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: SecretStoreUnavailableException) {
      _status.value =
        ConnectionManagerStatus("No OS keychain is available. otter will ask for the password each time.")
      null
    }

  private suspend fun storeSecretFor(
    config: ConnectionConfig,
    password: String?,
  ): Boolean {
    if (config.type == ConnectionType.Sqlite || !config.savePassword) {
      removeSecret(config.id)
      return true
    }
    if (password.isNullOrEmpty()) return true
    return try {
      secrets.set(config.id, password)
      true
    } catch (e: CancellationException) {
      throw e
    } catch (e: SecretStoreUnavailableException) {
      false
    }
  }

  private suspend fun removeSecret(id: String) {
    try {
      secrets.delete(id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: SecretStoreUnavailableException) {
      // No keychain means there is nothing to remove.
    }
  }

  private suspend fun copySecret(
    sourceId: String,
    copy: ConnectionConfig,
  ) {
    if (copy.type == ConnectionType.Sqlite || !copy.savePassword) return
    val secret = readSecret(sourceId) ?: return
    try {
      secrets.set(copy.id, secret)
    } catch (e: CancellationException) {
      throw e
    } catch (e: SecretStoreUnavailableException) {
      // The copy still works; it will ask for the password.
    }
  }

  private suspend fun reloadConnections() {
    _connections.value = store.list()
  }

  private fun findConnection(id: String): ConnectionConfig? = _connections.value.firstOrNull { it.id == id }

  private fun copyName(base: String): String {
    val existing = _connections.value.map { it.name }.toSet()
    var candidate = "$base copy"
    var index = 2
    while (candidate in existing) {
      candidate = "$base copy $index"
      index++
    }
    return candidate
  }

  private fun markBusy(
    id: String,
    busy: Boolean,
  ) {
    _busyIds.update { current -> if (busy) current + id else current - id }
  }
}
