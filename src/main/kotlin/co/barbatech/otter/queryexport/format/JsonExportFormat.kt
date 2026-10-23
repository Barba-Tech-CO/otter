package co.barbatech.otter.queryexport.format

/**
 * JSON array of objects keyed by column name: NULL becomes `null`, booleans `true`/`false`,
 * numbers only when lossless (integers within ±2^53, decimals with ≤15 digits and ≤15 decimal places,
 * otherwise a string), binary as base64 and text escaped.
 */
data object JsonExportFormat : ExportFormat
