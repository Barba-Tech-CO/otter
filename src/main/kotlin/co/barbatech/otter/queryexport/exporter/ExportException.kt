package co.barbatech.otter.queryexport.exporter

import java.nio.file.Path

/** Export failure with the output path and a readable reason. */
class ExportException(
  val path: Path,
  reason: String,
  cause: Throwable? = null,
) : Exception("Export to $path failed: $reason", cause)
