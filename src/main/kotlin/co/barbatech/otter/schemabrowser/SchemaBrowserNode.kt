package co.barbatech.otter.schemabrowser

import co.barbatech.otter.database.metadata.TableInfo

/** One visible schema browser row and its optional database table. */
data class SchemaBrowserNode(
  val id: String,
  val parentId: String?,
  val label: String,
  val kind: Kind,
  val table: TableInfo? = null,
) {
  enum class Kind { Schema, Table, Detail }
}
