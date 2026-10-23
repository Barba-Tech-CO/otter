package co.barbatech.otter.queryhistory.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Timestamp and one-line preview shown in a history row. */
internal object HistoryEntryFormatHelper {
  private val timestampFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

  fun timestamp(instant: Instant): String = timestampFormat.format(instant)

  fun sqlPreview(sql: String): String =
    sql
      .lineSequence()
      .firstOrNull { it.isNotBlank() }
      ?.trim()
      .orEmpty()
      .take(200)
}
