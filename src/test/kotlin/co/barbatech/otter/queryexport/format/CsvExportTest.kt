package co.barbatech.otter.queryexport.format

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.queryexport.exporter.FakeResultCursor
import co.barbatech.otter.queryexport.exporter.StreamingResultExporter
import co.barbatech.otter.queryexport.format.CsvExportFormat
import co.barbatech.otter.queryexport.format.ExportFormat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CsvExportTest {
  private val columns = listOf(ResultColumn("name", "text"), ResultColumn("note", "text"))

  @Test
  fun `quotes delimiters quotes and newlines`() =
    runTest {
      val rows =
        listOf(
          Row(listOf(CellValue.Text("alice"), CellValue.Text("plain"))),
          Row(listOf(CellValue.Text("bob"), CellValue.Text("a,b"))),
          Row(listOf(CellValue.Text("carol"), CellValue.Text("say \"hi\""))),
          Row(listOf(CellValue.Text("dave"), CellValue.Text("line1\nline2"))),
          Row(listOf(CellValue.Text("erin"), CellValue.Text("cr\rhere"))),
          Row(listOf(CellValue.Null, CellValue.Text(""))),
        )
      val expected =
        buildString {
          append("name,note\r\n")
          append("alice,plain\r\n")
          append("bob,\"a,b\"\r\n")
          append("carol,\"say \"\"hi\"\"\"\r\n")
          append("dave,\"line1\nline2\"\r\n")
          append("erin,\"cr\rhere\"\r\n")
          append(",\r\n")
        }

      assertEquals(expected, exportText(CsvExportFormat(), rows))
    }

  @Test
  fun `custom delimiter without header`() =
    runTest {
      val rows = listOf(Row(listOf(CellValue.Text("a;b"), CellValue.Text("c"))))

      assertEquals("\"a;b\";c\r\n", exportText(CsvExportFormat(delimiter = ';', includeHeader = false), rows))
    }

  @Test
  fun `header toggle and empty result`() =
    runTest {
      assertEquals("name,note\r\n", exportText(CsvExportFormat(), emptyList()))
      assertEquals("", exportText(CsvExportFormat(includeHeader = false), emptyList()))
    }

  @Test
  fun `null and empty string are empty fields`() =
    runTest {
      val rows = listOf(Row(listOf(CellValue.Null, CellValue.Text(""))))

      assertEquals("name,note\r\n,\r\n", exportText(CsvExportFormat(), rows))
    }

  @Test
  fun `writes utf8 without bom`() =
    runTest {
      val target = Files.createTempDirectory("csv-utf8").resolve("out.csv")
      StreamingResultExporter()
        .export(
          FakeResultCursor(columns, listOf(Row(listOf(CellValue.Text("héllo"), CellValue.Text("wörld"))))),
          CsvExportFormat(),
          target,
        ).toList()

      val bytes = Files.readAllBytes(target)
      assertNotEquals(listOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), bytes.take(3))
      assertContentEquals("name,note\r\nhéllo,wörld\r\n".toByteArray(StandardCharsets.UTF_8), bytes)
    }

  private suspend fun exportText(
    format: ExportFormat,
    rows: List<Row>,
  ): String {
    val target = Files.createTempDirectory("csv-export").resolve("out.csv")
    StreamingResultExporter().export(FakeResultCursor(columns, rows), format, target).toList()
    return Files.readString(target, StandardCharsets.UTF_8)
  }
}
