package co.barbatech.otter.queryexport.exporter.encoder

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.Row
import java.io.Writer

/** Streaming JSON array of objects; typed values avoid silent precision loss. */
internal class JsonRowEncoder(
  private val columns: List<ResultColumn>,
  private val writer: Writer,
) : RowEncoder {
  private var first = true

  override fun begin() {
    writer.write("[")
  }

  override fun write(row: Row) {
    if (!first) writer.write(",")
    first = false
    writer.write("\n  {")
    columns.forEachIndexed { index, column ->
      if (index > 0) writer.write(",")
      writer.write(JsonValueEncoder.jsonString(column.name))
      writer.write(":")
      writer.write(JsonValueEncoder.jsonValue(row.cells.getOrElse(index) { CellValue.Null }))
    }
    writer.write("}")
  }

  override fun end() {
    if (!first) writer.write("\n")
    writer.write("]")
  }
}
