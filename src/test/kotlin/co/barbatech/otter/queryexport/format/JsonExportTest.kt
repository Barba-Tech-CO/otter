package co.barbatech.otter.queryexport.format

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.queryexport.exporter.FakeResultCursor
import co.barbatech.otter.queryexport.exporter.StreamingResultExporter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsonExportTest {
  private val columns =
    listOf(
      ResultColumn("id", "int"),
      ResultColumn("flag", "bool"),
      ResultColumn("big", "numeric"),
      ResultColumn("price", "numeric"),
      ResultColumn("data", "bytea"),
      ResultColumn("note", "text"),
      ResultColumn("missing", "text"),
    )

  @Test
  fun `maps values without silent loss`() =
    runTest {
      val note = "quote \" backslash \\ newline \n tab\t unicode ü"
      val row =
        Row(
          listOf(
            CellValue.Numeric(BigDecimal("42")),
            CellValue.Bool(true),
            CellValue.Numeric(BigDecimal("9007199254740993")),
            CellValue.Numeric(BigDecimal("12.34")),
            CellValue.Binary(byteArrayOf(1, 2, 3)),
            CellValue.Text(note),
            CellValue.Null,
          ),
        )

      val objectValue =
        Json
          .parseToJsonElement(exportText(listOf(row)))
          .jsonArray
          .single()
          .jsonObject

      assertEquals("42", jsonNumber(objectValue["id"]))
      assertEquals("true", jsonNumber(objectValue["flag"]))
      assertEquals("12.34", jsonNumber(objectValue["price"]))
      assertEquals("9007199254740993", jsonString(objectValue["big"]))
      assertEquals("AQID", jsonString(objectValue["data"]))
      assertEquals(note, jsonString(objectValue["note"]))
      assertIs<JsonNull>(objectValue["missing"])
    }

  @Test
  fun `text that looks like a literal stays a string`() =
    runTest {
      val row =
        Row(
          listOf(
            CellValue.Text("null"),
            CellValue.Text("true"),
            CellValue.Numeric(BigDecimal("1")),
            CellValue.Numeric(BigDecimal("1.0")),
            CellValue.Text("x"),
            CellValue.Text("x"),
            CellValue.Text("x"),
          ),
        )

      val objectValue =
        Json
          .parseToJsonElement(exportText(listOf(row)))
          .jsonArray
          .single()
          .jsonObject

      assertEquals("null", jsonString(objectValue["id"]))
      assertEquals("true", jsonString(objectValue["flag"]))
      assertEquals("1", jsonNumber(objectValue["big"]))
      assertEquals("1", jsonNumber(objectValue["price"]))
    }

  @Test
  fun `escapes control characters`() =
    runTest {
      val row =
        Row(
          listOf(
            CellValue.Text("\u0001"),
            CellValue.Text("a/b"),
            CellValue.Text("x"),
            CellValue.Text("x"),
            CellValue.Text("x"),
            CellValue.Text("x"),
            CellValue.Text("x"),
          ),
        )

      val text = exportText(listOf(row))

      assertTrue(text.contains("\\u0001"))
      assertTrue(text.contains("a/b"))
    }

  @Test
  fun `multiple rows form one parseable array`() =
    runTest {
      val rows = listOf(singleValueRow(CellValue.Text("a")), singleValueRow(CellValue.Text("b")))

      val array = Json.parseToJsonElement(exportText(rows)).jsonArray

      assertEquals(2, array.size)
      assertEquals("a", jsonString(array[0].jsonObject["id"]))
      assertEquals("b", jsonString(array[1].jsonObject["id"]))
    }

  @Test
  fun `empty result is a valid empty array`() =
    runTest {
      val text = exportText(emptyList())

      assertEquals("[]", text)
      assertTrue(Json.parseToJsonElement(text).jsonArray.isEmpty())
    }

  private fun jsonNumber(element: JsonElement?): String {
    val primitive = element as? JsonPrimitive
    assertFalse(primitive == null || primitive.isString, "expected a JSON number, got $element")
    return primitive.content
  }

  private fun jsonString(element: JsonElement?): String? {
    val primitive = element as? JsonPrimitive
    assertEquals(true, primitive?.isString, "expected a JSON string, got $element")
    return primitive?.content
  }

  private fun singleValueRow(value: CellValue): Row =
    Row(List(columns.size) { index -> if (index == 0) value else CellValue.Null })

  private suspend fun exportText(rows: List<Row>): String {
    val target = Files.createTempDirectory("json-export").resolve("out.json")
    StreamingResultExporter().export(FakeResultCursor(columns, rows), JsonExportFormat, target).toList()
    return Files.readString(target, StandardCharsets.UTF_8)
  }
}
