package co.barbatech.otter.queryexport.exporter.encoder

import co.barbatech.otter.database.result.CellValue
import java.math.BigDecimal
import java.util.Base64

/** JSON encoding of one cell value without silent numeric precision loss. */
internal object JsonValueEncoder {
  fun jsonValue(cell: CellValue): String =
    when (cell) {
      CellValue.Null -> "null"
      is CellValue.Bool -> cell.value.toString()
      is CellValue.Numeric -> jsonNumber(cell.value)
      is CellValue.Text -> jsonString(cell.value)
      is CellValue.Binary -> jsonString(Base64.getEncoder().encodeToString(cell.bytes))
    }

  fun jsonString(value: String): String =
    buildString {
      append('"')
      value.forEach { character ->
        when (character) {
          '"' -> append("\\\"")
          '\\' -> append("\\\\")
          '\b' -> append("\\b")
          '\u000C' -> append("\\f")
          '\n' -> append("\\n")
          '\r' -> append("\\r")
          '\t' -> append("\\t")
          else -> if (character < ' ') append("\\u%04x".format(character.code)) else append(character)
        }
      }
      append('"')
    }

  fun jsonNumber(value: BigDecimal): String {
    val stripped = value.stripTrailingZeros()
    if (stripped.scale() <= 0) {
      return if (stripped.abs() <= maxSafeInteger) {
        stripped.toBigInteger().toString()
      } else {
        jsonString(value.toPlainString())
      }
    }
    return if (value.precision() <= maxSafeDecimalDigits && value.scale() <= maxSafeDecimalScale) {
      value.toPlainString()
    } else {
      jsonString(value.toPlainString())
    }
  }

  private val maxSafeInteger: BigDecimal = BigDecimal.valueOf(9_007_199_254_740_992L)
  private val maxSafeDecimalDigits = 15
  private val maxSafeDecimalScale = 15
}
