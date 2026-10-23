package co.barbatech.otter.queryexport.state

import co.barbatech.otter.database.result.QueryResult
import co.barbatech.otter.queryexport.exporter.ExportException
import co.barbatech.otter.queryexport.exporter.ExportProgress
import co.barbatech.otter.queryexport.exporter.ResultExporter
import co.barbatech.otter.queryexport.format.ExportFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.nio.file.Path

/**
 * UI state of the export dialog. [start] re-runs the query to stream all rows;
 * cancelling stops the export and removes the partial file.
 */
class ExportState(
  private val exporter: ResultExporter,
  private val scope: CoroutineScope,
) {
  private val _progress = MutableStateFlow<ExportProgress?>(null)
  val progress: StateFlow<ExportProgress?> = _progress.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _running = MutableStateFlow(false)
  val running: StateFlow<Boolean> = _running.asStateFlow()

  private val _message = MutableStateFlow<String?>(null)
  val message: StateFlow<String?> = _message.asStateFlow()

  private var job: Job? = null

  fun start(
    rerun: suspend () -> QueryResult,
    format: ExportFormat,
    target: Path,
  ) {
    if (_running.value) return
    _running.value = true
    _progress.value = null
    _error.value = null
    _message.value = null
    val path = target.toAbsolutePath()
    job =
      scope.launch {
        try {
          when (val result = rerun()) {
            is QueryResult.RowSet -> {
              exporter.export(result.cursor, format, path).collect { _progress.value = it }
            }

            is QueryResult.Affected -> {
              _error.value = "The statement produced no rows to export."
            }
          }
          if (_error.value == null) {
            val rows = _progress.value?.rowsWritten ?: 0
            _message.value = "Exported $rows rows to $path"
          }
        } catch (cancelled: CancellationException) {
          _message.value = "Export cancelled; the partial file was removed."
          throw cancelled
        } catch (failure: ExportException) {
          _error.value = failure.message
        } catch (failure: Exception) {
          _error.value = "Export to $path failed: ${failure.message ?: "unknown error"}"
        } finally {
          _running.value = false
        }
      }
  }

  fun cancel() {
    job?.cancel()
  }
}
