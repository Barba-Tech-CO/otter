package co.barbatech.otter.database.metadata

enum class TableKind {
  Table,
  View,
  ;

  /** Name shown in the schema browser. */
  val displayName: String
    get() =
      when (this) {
        Table -> "table"
        View -> "view"
      }
}
