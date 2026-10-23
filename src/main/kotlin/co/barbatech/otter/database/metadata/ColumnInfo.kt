package co.barbatech.otter.database.metadata

data class ColumnInfo(
  val name: String,
  val typeName: String,
  val nullable: Boolean,
  val defaultValue: String?,
  val isPrimaryKey: Boolean,
)
