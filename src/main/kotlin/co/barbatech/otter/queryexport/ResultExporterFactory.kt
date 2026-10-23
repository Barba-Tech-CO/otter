package co.barbatech.otter.queryexport

import co.barbatech.otter.queryexport.exporter.ResultExporter
import co.barbatech.otter.queryexport.exporter.StreamingResultExporter

/** Single place the app asks for the streaming exporter. */
object ResultExporterFactory {
  fun streamingExporter(): ResultExporter = StreamingResultExporter()
}
