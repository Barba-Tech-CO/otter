package co.barbatech.otter.queryexport.ui

import co.barbatech.otter.queryexport.format.CsvExportFormat
import co.barbatech.otter.queryexport.format.JsonExportFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExportDialogHelperTest {
  @Test
  fun knownDelimitersHaveLabelsAndUnknownOnesFallBackToTheCharacter() {
    assertEquals("Comma (,)", ExportDialogHelper.delimiterLabel(','))
    assertEquals("Semicolon (;)", ExportDialogHelper.delimiterLabel(';'))
    assertEquals("#", ExportDialogHelper.delimiterLabel('#'))
  }

  @Test
  fun exportFormatFollowsTheSelectedKind() {
    val csv = ExportDialogHelper.exportFormat(ExportKind.Csv, ';', includeHeader = false)
    assertIs<CsvExportFormat>(csv)
    assertEquals(';', csv.delimiter)
    assertEquals(false, csv.includeHeader)

    assertIs<JsonExportFormat>(ExportDialogHelper.exportFormat(ExportKind.Json, ',', includeHeader = true))
  }

  @Test
  fun suggestedPathUsesTheFormatExtension() {
    assertEquals("report.csv", ExportDialogHelper.suggestedPath("report", ExportKind.Csv).fileName.toString())
    assertEquals("report.json", ExportDialogHelper.suggestedPath("report", ExportKind.Json).fileName.toString())
  }
}
