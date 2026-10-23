package co.barbatech.otter.database.metadata

/** A schema (namespace). SQLite reports a single schema named "main". */
data class SchemaInfo(
  val name: String,
)
