package co.barbatech.otter.resultgrid.format

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.Row
import java.math.BigDecimal
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridFormattingTest {
  @Test
  fun nullAndEmptyStringLookDifferent() {
    assertEquals("NULL", GridFormattingHelper.cellText(CellValue.Null))
    assertEquals("", GridFormattingHelper.cellText(CellValue.Text("")))
    assertTrue(GridFormattingHelper.isNullCell(CellValue.Null))
    assertEquals(false, GridFormattingHelper.isNullCell(CellValue.Text("")))
  }

  @Test
  fun binaryShowsPlaceholderWithSize() {
    val value = CellValue.Binary(byteArrayOf(1, 2, 3))
    assertEquals("<BLOB 3 bytes>", GridFormattingHelper.cellText(value))
  }

  @Test
  fun longValuesAreTruncated() {
    val text = "x".repeat(200)
    val truncated = GridFormattingHelper.truncateForCell(text, maxChars = 10)
    assertEquals(11, truncated.length)
    assertTrue(truncated.endsWith("\u2026"))
  }

  @Test
  fun columnWidthsUseTheHeaderAndOnlySampleRows() {
    val columns = listOf(ResultColumn("id", "INTEGER"), ResultColumn("name", "TEXT"))
    val rows =
      (0 until 60).map { index ->
        Row(
          listOf(
            CellValue.Numeric(BigDecimal.valueOf(index.toLong())),
            CellValue.Text(if (index == 55) "x".repeat(30) else "a"),
          ),
        )
      }
    assertEquals(listOf(4, 4), GridFormattingHelper.columnCharWidths(columns, rows, maxChars = 40))
  }

  @Test
  fun columnWidthsGrowWithSampledValues() {
    val columns = listOf(ResultColumn("id", "INTEGER"))
    val rows = listOf(Row(listOf(CellValue.Text("longer text"))))
    assertEquals(listOf(11), GridFormattingHelper.columnCharWidths(columns, rows))
  }

  @Test
  fun statusShowsLoadedRowsTimeAndMoreFlag() {
    assertEquals(
      "500 rows loaded - 1.50 s - more available",
      GridFormattingHelper.formatStatusText(500, Duration.ofMillis(1500), hasMore = true, sortedLoadedOnly = false),
    )
    assertEquals(
      "500 rows loaded - 12 ms - end of result - sorted loaded rows only",
      GridFormattingHelper.formatStatusText(500, Duration.ofMillis(12), hasMore = false, sortedLoadedOnly = true),
    )
  }
}
