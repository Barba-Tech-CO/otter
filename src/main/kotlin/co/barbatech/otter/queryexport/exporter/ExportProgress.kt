package co.barbatech.otter.queryexport.exporter

/** Emitted while exporting. The last emission has [finished] true. */
data class ExportProgress(
  val rowsWritten: Long,
  val finished: Boolean = false,
)
