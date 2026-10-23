package co.barbatech.otter.database.metadata

/** A table or view. [schema] is null when the engine has no schemas. */
data class TableInfo(
  val schema: String?,
  val name: String,
  val kind: TableKind,
)
