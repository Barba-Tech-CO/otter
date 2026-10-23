package co.barbatech.otter.resultgrid.clipboard

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.resultgrid.state.CellPosition
import co.barbatech.otter.resultgrid.state.GridSelection
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class GridClipboardTest {
  private val columns =
    listOf(
      ResultColumn("id", "INTEGER"),
      ResultColumn("note", "TEXT"),
      ResultColumn("raw", "TEXT"),
    )

  @Test
  fun nullIsCopiedAsEmptyText() {
    assertEquals("", GridClipboardHelper.clipboardText(null))
    assertEquals("", GridClipboardHelper.clipboardText(CellValue.Null))
  }

  @Test
  fun selectionIsTabSeparatedWithEmptyNullFields() {
    val rows =
      listOf(
        Row(listOf(CellValue.Numeric(BigDecimal("1")), CellValue.Null, CellValue.Text("x"))),
      )
    val selection = GridSelection(CellPosition(0, 0), CellPosition(0, 2))
    assertEquals("1\t\tx", GridClipboardHelper.formatRowsForClipboard(rows, columns, selection, includeHeaders = false))
  }

  @Test
  fun tabsAndNewlinesInsideValuesAreQuoted() {
    val rows =
      listOf(
        Row(listOf(CellValue.Text("a\tb"), CellValue.Text("line1\nline2"), CellValue.Text("say \"hi\""))),
      )
    val selection = GridSelection(CellPosition(0, 0), CellPosition(0, 2))
    assertEquals(
      "\"a\tb\"\t\"line1\nline2\"\t\"say \"\"hi\"\"\"",
      GridClipboardHelper.formatRowsForClipboard(rows, columns, selection, includeHeaders = false),
    )
  }

  @Test
  fun headersUseTheSelectedColumns() {
    val rows = listOf(Row(listOf(CellValue.Numeric(BigDecimal("1")), CellValue.Text("a"), CellValue.Text("b"))))
    val selection = GridSelection(CellPosition(0, 1), CellPosition(0, 2))
    assertEquals(
      "note\traw\na\tb",
      GridClipboardHelper.formatRowsForClipboard(rows, columns, selection, includeHeaders = true),
    )
  }

  @Test
  fun selectionIsNormalizedRegardlessOfAnchorDirection() {
    val selection = GridSelection(CellPosition(2, 1), CellPosition(0, 0))
    assertEquals(0, selection.firstRow)
    assertEquals(2, selection.lastRow)
    assertEquals(0, selection.firstColumn)
    assertEquals(1, selection.lastColumn)
    assertEquals(true, selection.contains(1, 1))
    assertEquals(false, selection.contains(1, 2))
  }
}
