package co.barbatech.otter.database.session

/** SQL text helpers: identifier quoting and Postgres string literal decoding. */
internal object SqlTextHelper {
  fun quoteIdentifier(
    value: String,
    quote: String,
  ): String = "$quote${value.replace(quote, quote + quote)}$quote"

  fun unquoteSqlStringLiteral(value: String): String {
    if (value.length < 2 || value.first() != '\'' || value.last() != '\'') return value
    val decoded = StringBuilder()
    var index = 1
    while (index < value.lastIndex) {
      val character = value[index]
      if (character == '\'') {
        if (index + 1 >= value.lastIndex || value[index + 1] != '\'') return value
        index++
      }
      decoded.append(character)
      index++
    }
    return decoded.toString()
  }
}
