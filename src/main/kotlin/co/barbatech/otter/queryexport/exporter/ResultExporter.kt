package co.barbatech.otter.queryexport.exporter

import co.barbatech.otter.database.result.ResultCursor
import co.barbatech.otter.queryexport.format.ExportFormat
import kotlinx.coroutines.flow.Flow
import java.nio.file.Path

/** Streams a cursor to disk. */
interface ResultExporter {
  /**
   * Writes every remaining row of [cursor] to [target] and closes the cursor at the end.
   * Cancelling the collector cancels the export. Throws on I/O or database failure.
   */
  fun export(
    cursor: ResultCursor,
    format: ExportFormat,
    target: Path,
  ): Flow<ExportProgress>
}
