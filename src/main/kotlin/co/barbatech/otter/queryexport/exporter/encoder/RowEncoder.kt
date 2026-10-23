package co.barbatech.otter.queryexport.exporter.encoder

import co.barbatech.otter.database.result.Row

internal interface RowEncoder {
  fun begin()

  fun write(row: Row)

  fun end()
}
