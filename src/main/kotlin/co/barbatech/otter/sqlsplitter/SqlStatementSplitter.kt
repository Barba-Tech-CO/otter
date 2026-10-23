package co.barbatech.otter.sqlsplitter

/** Splits a script into single statements, ignoring semicolons inside strings and comments. */
fun interface SqlStatementSplitter {
  fun split(script: String): List<String>
}
