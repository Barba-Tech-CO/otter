package co.barbatech.otter.sqlsplitter

/** Single place the app asks for the statement splitter. */
object SqlStatementSplitterFactory {
  fun defaultSplitter(): SqlStatementSplitter = DefaultSqlStatementSplitter()
}
