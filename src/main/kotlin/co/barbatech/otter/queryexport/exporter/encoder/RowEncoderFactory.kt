package co.barbatech.otter.queryexport.exporter.encoder

import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.queryexport.format.CsvExportFormat
import co.barbatech.otter.queryexport.format.ExportFormat
import co.barbatech.otter.queryexport.format.JsonExportFormat
import java.io.Writer

/** Chooses the row encoder for the requested export format. */
internal object RowEncoderFactory {
  fun encoderFor(
    columns: List<ResultColumn>,
    format: ExportFormat,
    writer: Writer,
  ): RowEncoder =
    when (format) {
      is CsvExportFormat -> CsvRowEncoder(columns, format, writer)
      JsonExportFormat -> JsonRowEncoder(columns, writer)
    }
}
