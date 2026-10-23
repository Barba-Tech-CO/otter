package co.barbatech.otter.database.result

import java.math.BigDecimal

/**
 * One cell. NULL is its own type so it never gets confused with an empty string.
 * Dates and times arrive as [Text] in ISO format.
 */
sealed interface CellValue {
  /** Text form for copy and export, or null for SQL NULL. */
  fun textOrNull(): String?

  data object Null : CellValue {
    override fun textOrNull(): String? = null
  }

  data class Text(
    val value: String,
  ) : CellValue {
    override fun textOrNull(): String = value
  }

  data class Numeric(
    val value: BigDecimal,
  ) : CellValue {
    override fun textOrNull(): String = value.toPlainString()
  }

  data class Bool(
    val value: Boolean,
  ) : CellValue {
    override fun textOrNull(): String = value.toString()
  }

  /** Binary data; text form is "0x" plus lowercase hex. */
  class Binary(
    val bytes: ByteArray,
  ) : CellValue {
    override fun textOrNull(): String = "0x" + bytes.toHexString()

    override fun equals(other: Any?): Boolean = other is Binary && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()
  }
}
