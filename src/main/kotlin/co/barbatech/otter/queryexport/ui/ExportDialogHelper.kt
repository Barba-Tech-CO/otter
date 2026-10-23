package co.barbatech.otter.queryexport.ui

import co.barbatech.otter.queryexport.format.CsvExportFormat
import co.barbatech.otter.queryexport.format.ExportFormat
import co.barbatech.otter.queryexport.format.JsonExportFormat
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JFileChooser

/** Defaults, format selection and file handling for the export dialog. */
internal object ExportDialogHelper {
  val delimiters: List<Pair<Char, String>> =
    listOf(',' to "Comma (,)", ';' to "Semicolon (;)", '\t' to "Tab", '|' to "Pipe (|)")

  fun delimiterLabel(delimiter: Char): String =
    delimiters.firstOrNull { it.first == delimiter }?.second ?: delimiter.toString()

  fun exportFormat(
    kind: ExportKind,
    delimiter: Char,
    includeHeader: Boolean,
  ): ExportFormat =
    when (kind) {
      ExportKind.Csv -> CsvExportFormat(delimiter, includeHeader)
      ExportKind.Json -> JsonExportFormat
    }

  fun suggestedPath(
    baseName: String,
    kind: ExportKind,
  ): Path {
    val extension = if (kind == ExportKind.Csv) "csv" else "json"
    return Paths.get(System.getProperty("user.home"), "$baseName.$extension")
  }

  fun chooseTarget(current: Path): Path? {
    val chooser = JFileChooser()
    chooser.dialogTitle = "Export result"
    chooser.selectedFile = current.toFile()
    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.toPath() else null
  }
}
