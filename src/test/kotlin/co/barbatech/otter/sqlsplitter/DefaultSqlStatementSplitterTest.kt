package co.barbatech.otter.sqlsplitter

import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSqlStatementSplitterTest {
  private val splitter = DefaultSqlStatementSplitter()

  @Test
  fun splitsPlainAndTrailingStatements() {
    assertEquals(listOf("select 1", "select 2"), splitter.split(" select 1; select 2 "))
  }

  @Test
  fun ignoresSemicolonsInsideQuotedText() {
    assertEquals(
      listOf("select 'a;''b'", "select \"column;name\""),
      splitter.split("select 'a;''b'; select \"column;name\";"),
    )
  }

  @Test
  fun ignoresSemicolonsInsideLineAndNestedBlockComments() {
    assertEquals(
      listOf("select 1 -- ; comment", "/* outer ; /* inner ; */ end */ select 2"),
      splitter.split("select 1 -- ; comment\r\n; /* outer ; /* inner ; */ end */ select 2;"),
    )
  }

  @Test
  fun ignoresSemicolonsInsideDollarQuotedStrings() {
    assertEquals(
      listOf($$$"select $$a;b$$", $$$"select $body$ c;d $body$"),
      splitter.split($$$"select $$a;b$$;select $body$ c;d $body$;"),
    )
  }

  @Test
  fun retainsUnterminatedQuotedTextAndComments() {
    assertEquals(listOf("select 'unfinished; text"), splitter.split("select 'unfinished; text"))
    assertEquals(listOf("select 1 /* unfinished ; comment"), splitter.split("select 1 /* unfinished ; comment"))
  }

  @Test
  fun returnsNoStatementsForEmptyOrCommentOnlyScripts() {
    assertEquals(emptyList(), splitter.split(" \r\n ; -- comment;\r\n /* only /* nested */ comment */ "))
  }
}
