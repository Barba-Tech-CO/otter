package co.barbatech.otter.resultgrid.sort

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.resultgrid.format.GridFormattingHelper
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class GridSortingTest {
  private fun numbers(vararg values: Int?): List<Row> =
    values.map { value -> Row(listOf(if (value == null) CellValue.Null else CellValue.Numeric(BigDecimal(value)))) }

  private fun column(rows: List<Row>): List<String> = rows.map { GridFormattingHelper.cellText(it.cells.first()) }

  @Test
  fun sortsNumericValuesAscending() {
    val rows = numbers(3, 1, 2)
    assertEquals(listOf("1", "2", "3"), column(GridSortingHelper.sortRows(rows, 0, ascending = true)))
  }

  @Test
  fun sortsNumericValuesDescending() {
    val rows = numbers(3, 1, 2)
    assertEquals(listOf("3", "2", "1"), column(GridSortingHelper.sortRows(rows, 0, ascending = false)))
  }

  @Test
  fun nullsStayLastInBothDirections() {
    val rows = numbers(2, null, 1)
    assertEquals(listOf("1", "2", "NULL"), column(GridSortingHelper.sortRows(rows, 0, ascending = true)))
    assertEquals(listOf("2", "1", "NULL"), column(GridSortingHelper.sortRows(rows, 0, ascending = false)))
  }

  @Test
  fun sortsTextAndKeepsStableOrderForEqualValues() {
    val rows =
      listOf(
        Row(listOf(CellValue.Text("b"))),
        Row(listOf(CellValue.Text("a"))),
        Row(listOf(CellValue.Text("a"))),
      )
    val sorted = GridSortingHelper.sortRows(rows, 0, ascending = true)
    assertEquals(listOf("a", "a", "b"), column(sorted))
  }
}
