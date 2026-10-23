package co.barbatech.otter.database.metadata

/** Everything the schema browser shows when a table node is expanded. */
data class TableDetails(
  val table: TableInfo,
  val columns: List<ColumnInfo>,
  val indexes: List<IndexInfo>,
  val foreignKeys: List<ForeignKeyInfo>,
)
