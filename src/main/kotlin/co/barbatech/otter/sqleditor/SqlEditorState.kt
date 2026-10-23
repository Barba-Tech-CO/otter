package co.barbatech.otter.sqleditor

import co.barbatech.otter.database.error.DatabaseException
import co.barbatech.otter.database.session.DatabaseSession
import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.store.HistoryStore
import co.barbatech.otter.resultgrid.state.ResultGridState
import co.barbatech.otter.resultgrid.state.ResultTab
import co.barbatech.otter.sqlsplitter.SqlStatementSplitter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * UI state of the editor. Running splits the script, executes each statement on [activeSession],
 * records each one in [history] and hands the outcomes to [resultGrid].
 */
class SqlEditorState(
  private val activeSession: StateFlow<DatabaseSession?>,
  private val splitter: SqlStatementSplitter,
  private val history: HistoryStore,
  private val resultGrid: ResultGridState,
  private val scope: CoroutineScope,
) {
  private val _text = MutableStateFlow("")
  val text: StateFlow<String> = _text.asStateFlow()

  private val _running = MutableStateFlow(false)
  val running: StateFlow<Boolean> = _running.asStateFlow()

  private val _message = MutableStateFlow("")
  val message: StateFlow<String> = _message.asStateFlow()

  private var cancellationRequested = false

  fun setText(value: String) {
    _text.value = value
  }

  /** Runs [selectedText] when not blank, otherwise the whole editor text. */
  fun run(selectedText: String?) {
    if (_running.value) return
    _message.value = ""
    val session = activeSession.value ?: return
    val script = selectedText?.takeIf(String::isNotEmpty) ?: _text.value
    val statements = splitter.split(script)
    if (statements.isEmpty()) return

    _running.value = true
    cancellationRequested = false
    scope.launch {
      val tabs = mutableListOf<ResultTab>()
      try {
        for (sql in statements) {
          val startedAt = Instant.now()
          val startedNanos = System.nanoTime()
          try {
            val queryResult = session.execute(sql)
            val duration = Duration.ofNanos(System.nanoTime() - startedNanos)
            history.record(HistoryEntry(session.config.name, sql, startedAt, duration, true))
            tabs += ResultTab(sql, duration, Result.success(queryResult)) { session.execute(sql) }
          } catch (cancellation: CancellationException) {
            throw cancellation
          } catch (error: Exception) {
            val duration = Duration.ofNanos(System.nanoTime() - startedNanos)
            val detail =
              when (error) {
                is DatabaseException -> error.message.orEmpty() + (error.position?.let { " (position $it)" } ?: "")
                else -> error.message ?: "Query cancelled"
              }
            history.record(HistoryEntry(session.config.name, sql, startedAt, duration, false, detail))
            tabs += ResultTab(sql, duration, Result.failure(error)) { session.execute(sql) }
            _message.value = detail
            break
          }
          if (cancellationRequested) break
        }
      } finally {
        resultGrid.show(tabs)
        _running.value = false
      }
    }
  }

  /** Cancels the running statement and stops the rest of the script. */
  fun cancel() {
    val session = activeSession.value ?: return
    if (!_running.value) return
    cancellationRequested = true
    scope.launch {
      try {
        session.cancel()
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (error: Exception) {
        _message.value = error.message ?: "Unable to cancel the running query"
      }
    }
  }
}
