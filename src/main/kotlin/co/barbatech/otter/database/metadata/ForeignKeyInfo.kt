package co.barbatech.otter.database.metadata

data class ForeignKeyInfo(
  val name: String?,
  val columns: List<String>,
  val referencedSchema: String?,
  val referencedTable: String,
  val referencedColumns: List<String>,
)
