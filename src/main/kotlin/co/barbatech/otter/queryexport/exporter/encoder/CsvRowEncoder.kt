package co.barbatech.otter.queryexport.exporter.encoder

import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.queryexport.format.CsvExportFormat
import java.io.Writer

/** RFC 4180 quoting; CRLF line ends; UTF-8 without BOM; NULL is an empty field. */
internal class CsvRowEncoder(
  private val columns: List<ResultColumn>,
  private val format: CsvExportFormat,
  private val writer: Writer,
) : RowEncoder {
  override fun begin() {
    if (!format.includeHeader) return
    writeFields(columns.map { it.name })
  }

  override fun write(row: Row) {
    writeFields(row.cells.map { it.textOrNull() ?: "" })
  }

  override fun end() = Unit

  private fun writeFields(fields: List<String>) {
    fields.forEachIndexed { index, field ->
      if (index > 0) writer.write(format.delimiter.code)
      writer.write(escape(field))
    }
    writer.write("\r\n")
  }

  private fun escape(value: String): String {
    val needsQuotes =
      value.any { it == format.delimiter || it == '"' || it == '\r' || it == '\n' }
    if (!needsQuotes) return value
    return buildString {
      append('"')
      value.forEach { character ->
        if (character == '"') append("\"\"") else append(character)
      }
      append('"')
    }
  }
}
