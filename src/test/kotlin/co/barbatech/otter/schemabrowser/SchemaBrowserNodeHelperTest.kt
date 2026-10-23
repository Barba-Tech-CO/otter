package co.barbatech.otter.schemabrowser

import co.barbatech.otter.database.metadata.ColumnInfo
import co.barbatech.otter.database.metadata.ForeignKeyInfo
import co.barbatech.otter.database.metadata.IndexInfo
import co.barbatech.otter.database.metadata.TableDetails
import co.barbatech.otter.database.metadata.TableInfo
import co.barbatech.otter.database.metadata.TableKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaBrowserNodeHelperTest {
  private val table = TableInfo("main", "users", TableKind.Table)

  @Test
  fun detailNodesDescribeColumnsIndexesAndForeignKeys() {
    val details =
      TableDetails(
        table,
        listOf(ColumnInfo("id", "INTEGER", nullable = false, defaultValue = null, isPrimaryKey = true)),
        listOf(IndexInfo("users_pk", listOf("id"), unique = true)),
        listOf(ForeignKeyInfo("fk", listOf("id"), "main", "accounts", listOf("id"))),
      )

    val labels = SchemaBrowserNodeHelper.detailNodes("schema:main/table:users", details).map { it.label }

    assertEquals("Column id: INTEGER, not null [PK]", labels[0])
    assertEquals("Index users_pk: id, unique", labels[1])
    assertEquals("Foreign key fk: id → accounts(id)", labels[2])
    assertTrue(
      SchemaBrowserNodeHelper.detailNodes("schema:main/table:users", details).all {
        it.kind ==
          SchemaBrowserNode.Kind.Detail
      },
    )
  }

  @Test
  fun descendantQuestionsFollowTheParentChain() {
    val root = SchemaBrowserNode("schema:main", null, "main", SchemaBrowserNode.Kind.Schema)
    val child = SchemaBrowserNode("schema:main/table:users", root.id, "users", SchemaBrowserNode.Kind.Table)
    val detail =
      SchemaBrowserNode("schema:main/table:users/detail:Column id", child.id, "id", SchemaBrowserNode.Kind.Detail)
    val nodes = listOf(root, child, detail)

    assertTrue(SchemaBrowserNodeHelper.isDescendant(detail, root.id, nodes))
    assertFalse(SchemaBrowserNodeHelper.isDescendant(detail, "schema:other", nodes))
    assertTrue(SchemaBrowserNodeHelper.hasIncludedDescendant(root, nodes, setOf(detail.id)))
  }
}
