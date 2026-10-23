package co.barbatech.otter.queryexport.exporter

import co.barbatech.otter.database.result.ResultCursor
import co.barbatech.otter.queryexport.exporter.encoder.RowEncoderFactory
import co.barbatech.otter.queryexport.format.ExportFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Streams a cursor to CSV or JSON in chunks on [dispatcher], closes the cursor at the end,
 * and on cancellation or failure deletes the partially written file.
 */
class StreamingResultExporter(
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ResultExporter {
  override fun export(
    cursor: ResultCursor,
    format: ExportFormat,
    target: Path,
  ): Flow<ExportProgress> =
    flow {
      val path = target.toAbsolutePath()
      path.parent?.let { Files.createDirectories(it) }
      var rowsWritten = 0L
      try {
        Files.newBufferedWriter(path, StandardCharsets.UTF_8).use { writer ->
          val encoder = RowEncoderFactory.encoderFor(cursor.columns, format, writer)
          encoder.begin()
          while (cursor.hasMore) {
            val rows = cursor.fetch(500)
            if (rows.isEmpty()) break
            rows.forEach(encoder::write)
            rowsWritten += rows.size
            emit(ExportProgress(rowsWritten))
          }
          encoder.end()
        }
        emit(ExportProgress(rowsWritten, finished = true))
      } catch (cancelled: CancellationException) {
        ExportFileHelper.deleteQuietly(path)
        throw cancelled
      } catch (failure: Exception) {
        ExportFileHelper.deleteQuietly(path)
        throw ExportException(path, failure.message ?: "unknown error", failure)
      } finally {
        cursor.close()
      }
    }.flowOn(dispatcher)
}
