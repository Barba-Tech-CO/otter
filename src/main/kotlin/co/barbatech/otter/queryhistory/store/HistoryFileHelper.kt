package co.barbatech.otter.queryhistory.store

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Keeps an unreadable history file next to the original instead of discarding it. */
internal object HistoryFileHelper {
  fun backupCorruptFile(source: Path): Path? =
    try {
      val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
      val backup = source.resolveSibling("${source.fileName}.corrupt-$stamp")
      Files.move(source, backup, StandardCopyOption.REPLACE_EXISTING)
      backup
    } catch (_: IOException) {
      null
    }
}
