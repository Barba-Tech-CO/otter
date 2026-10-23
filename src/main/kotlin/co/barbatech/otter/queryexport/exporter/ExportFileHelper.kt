package co.barbatech.otter.queryexport.exporter

import java.nio.file.Files
import java.nio.file.Path

/** Removes a partially written export without hiding the original failure. */
internal object ExportFileHelper {
  fun deleteQuietly(path: Path) {
    try {
      Files.deleteIfExists(path)
    } catch (_: Exception) {
      // The original failure is more useful than a cleanup failure.
    }
  }
}
