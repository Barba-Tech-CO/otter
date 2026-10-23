package co.barbatech.otter.queryhistory.ui

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryEntryFormatHelperTest {
  @Test
  fun previewKeepsTheFirstNonBlankLineTrimmed() {
    assertEquals(
      "select 1",
      HistoryEntryFormatHelper.sqlPreview("\n  \n  select 1  \nselect 2"),
    )
    assertEquals("", HistoryEntryFormatHelper.sqlPreview(" \n\t "))
  }

  @Test
  fun previewIsLimitedToTwoHundredCharacters() {
    val preview = HistoryEntryFormatHelper.sqlPreview("x".repeat(250))

    assertEquals(200, preview.length)
  }

  @Test
  fun timestampUsesTheRowPattern() {
    val text = HistoryEntryFormatHelper.timestamp(Instant.parse("2026-10-23T10:00:00Z"))

    assertTrue(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""").matches(text))
  }
}
