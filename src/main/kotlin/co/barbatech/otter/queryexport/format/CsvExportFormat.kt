package co.barbatech.otter.queryexport.format

/**
 * CSV with UTF-8 (no BOM), CRLF line ends per RFC 4180, quoting of delimiters, quotes and newlines;
 * NULL is written as an empty field.
 */
data class CsvExportFormat(
  val delimiter: Char = ',',
  val includeHeader: Boolean = true,
) : ExportFormat
