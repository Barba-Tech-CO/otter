package co.barbatech.otter.database.session

import kotlin.test.Test
import kotlin.test.assertEquals

class SqlTextHelperTest {
  @Test
  fun quotingDoublesEmbeddedQuotes() {
    assertEquals("\"users\"", SqlTextHelper.quoteIdentifier("users", "\""))
    assertEquals("\"odd\"\"name\"", SqlTextHelper.quoteIdentifier("odd\"name", "\""))
  }

  @Test
  fun stringLiteralsAreUnquotedAndDoubledQuotesDecoded() {
    assertEquals("plain", SqlTextHelper.unquoteSqlStringLiteral("'plain'"))
    assertEquals("it's", SqlTextHelper.unquoteSqlStringLiteral("'it''s'"))
    assertEquals("lower('X')", SqlTextHelper.unquoteSqlStringLiteral("lower('X')"))
    assertEquals("'unterminated", SqlTextHelper.unquoteSqlStringLiteral("'unterminated"))
  }
}
