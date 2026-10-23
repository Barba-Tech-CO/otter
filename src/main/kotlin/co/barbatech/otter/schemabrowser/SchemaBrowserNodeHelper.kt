package co.barbatech.otter.schemabrowser

import co.barbatech.otter.database.metadata.SchemaInfo
import co.barbatech.otter.database.metadata.TableDetails
import co.barbatech.otter.database.metadata.TableInfo

/** Builds schema browser nodes and answers parent/child questions about them. */
internal object SchemaBrowserNodeHelper {
  fun schemaNode(schema: SchemaInfo): SchemaBrowserNode =
    SchemaBrowserNode("schema:${schema.name}", null, schema.name, SchemaBrowserNode.Kind.Schema)

  fun tableNode(
    parentId: String,
    table: TableInfo,
  ): SchemaBrowserNode =
    SchemaBrowserNode(
      id = "$parentId/table:${table.name}",
      parentId = parentId,
      label = "${table.name} (${table.kind.displayName})",
      kind = SchemaBrowserNode.Kind.Table,
      table = table,
    )

  fun detailNodes(
    parentId: String,
    details: TableDetails,
  ): List<SchemaBrowserNode> =
    buildList {
      details.columns.forEach { column ->
        val marker = if (column.isPrimaryKey) " [PK]" else ""
        val nullability = if (column.nullable) "nullable" else "not null"
        val default = column.defaultValue?.let { ", default $it" } ?: ""
        add(detail(parentId, "Column ${column.name}: ${column.typeName}, $nullability$default$marker"))
      }
      details.indexes.forEach { index ->
        val uniqueness = if (index.unique) "unique" else "not unique"
        add(detail(parentId, "Index ${index.name}: ${index.columns.joinToString()}, $uniqueness"))
      }
      details.foreignKeys.forEach { key ->
        val label =
          "Foreign key ${key.name ?: ""}: ${key.columns.joinToString()} → " +
            "${key.referencedTable}(${key.referencedColumns.joinToString()})"
        add(detail(parentId, label))
      }
    }

  fun detail(
    parentId: String,
    label: String,
  ): SchemaBrowserNode = SchemaBrowserNode("$parentId/detail:$label", parentId, label, SchemaBrowserNode.Kind.Detail)

  fun isDescendant(
    node: SchemaBrowserNode,
    parentId: String,
    allNodes: List<SchemaBrowserNode>,
  ): Boolean {
    var ancestorId = node.parentId
    while (ancestorId != null) {
      if (ancestorId == parentId) return true
      ancestorId = allNodes.firstOrNull { it.id == ancestorId }?.parentId
    }
    return false
  }

  fun hasIncludedDescendant(
    node: SchemaBrowserNode,
    allNodes: List<SchemaBrowserNode>,
    included: Set<String>,
  ): Boolean = allNodes.any { it.id in included && isDescendant(it, node.id, allNodes) }
}
