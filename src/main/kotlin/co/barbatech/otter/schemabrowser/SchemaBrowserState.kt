package co.barbatech.otter.schemabrowser

import co.barbatech.otter.database.metadata.SchemaInfo
import co.barbatech.otter.database.metadata.TableInfo
import co.barbatech.otter.database.session.DatabaseDefaults
import co.barbatech.otter.database.session.DatabaseSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI state of the lazy schema tree. [onOpenQuery] puts SQL into the editor. */
class SchemaBrowserState(
  private val activeSession: StateFlow<DatabaseSession?>,
  private val onOpenQuery: (String) -> Unit,
  private val scope: CoroutineScope,
) {
  private val _filter = MutableStateFlow("")
  val filter: StateFlow<String> = _filter.asStateFlow()
  private val _nodes = MutableStateFlow<List<SchemaBrowserNode>>(emptyList())
  val nodes: StateFlow<List<SchemaBrowserNode>> = _nodes.asStateFlow()
  private val _loadingNodeIds = MutableStateFlow<Set<String>>(emptySet())
  val loadingNodeIds: StateFlow<Set<String>> = _loadingNodeIds.asStateFlow()
  private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
  val errors: StateFlow<Map<String, String>> = _errors.asStateFlow()
  private val _expandedNodeIds = MutableStateFlow<Set<String>>(emptySet())
  val expandedNodeIds: StateFlow<Set<String>> = _expandedNodeIds.asStateFlow()
  private val _selectedNodeId = MutableStateFlow<String?>(null)
  val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()
  private var currentSession: DatabaseSession? = null

  init {
    scope.launch {
      activeSession.collectLatest { session ->
        currentSession = session
        _nodes.value = emptyList()
        _expandedNodeIds.value = emptySet()
        _selectedNodeId.value = null
        if (session != null) loadSchemas(session)
      }
    }
  }

  fun setFilter(value: String) {
    _filter.value = value
  }

  fun select(node: SchemaBrowserNode) {
    _selectedNodeId.value = node.id
  }

  fun toggle(node: SchemaBrowserNode) {
    select(node)
    if (node.kind == SchemaBrowserNode.Kind.Detail) return
    if (node.id in _expandedNodeIds.value) {
      _expandedNodeIds.update { it - node.id }
    } else {
      _expandedNodeIds.update { it + node.id }
      if (_nodes.value.none { it.parentId == node.id }) loadChildren(node)
    }
  }

  /** Reloads the selected node; with no selection it reloads the root schema list. */
  fun refresh() {
    val session = currentSession ?: return
    val selected = _nodes.value.firstOrNull { it.id == _selectedNodeId.value }
    when (selected?.kind) {
      SchemaBrowserNode.Kind.Table -> loadDetails(session, selected)
      SchemaBrowserNode.Kind.Schema -> loadTables(session, selected, replace = true)
      else -> loadSchemas(session)
    }
  }

  /** Opens a page-size preview query for [table]. */
  fun openTable(table: TableInfo) {
    currentSession?.let { onOpenQuery(it.previewQuery(table, DatabaseDefaults.defaultFetchSize)) }
  }

  fun visibleNodes(): List<SchemaBrowserNode> {
    val allNodes = _nodes.value
    val query = _filter.value.trim()
    if (query.isEmpty()) {
      return allNodes.filter { depth(it, allNodes) == 0 || it.parentId in _expandedNodeIds.value }
    }
    val included = allNodes.filter { it.label.contains(query, ignoreCase = true) }.mapTo(mutableSetOf()) { it.id }
    return allNodes.filter {
      it.id in included ||
        SchemaBrowserNodeHelper.hasIncludedDescendant(
          it,
          allNodes,
          included,
        )
    }
  }

  fun depth(
    node: SchemaBrowserNode,
    allNodes: List<SchemaBrowserNode> = _nodes.value,
  ): Int {
    var result = 0
    var parentId = node.parentId
    while (parentId != null) {
      result++
      parentId = allNodes.firstOrNull { it.id == parentId }?.parentId
    }
    return result
  }

  private fun loadSchemas(session: DatabaseSession) {
    load("root") {
      val schemas = withContext(Dispatchers.IO) { session.listSchemas() }
      _nodes.value = schemas.map(SchemaBrowserNodeHelper::schemaNode)
    }
  }

  private fun loadChildren(node: SchemaBrowserNode) {
    val session = currentSession ?: return
    when (node.kind) {
      SchemaBrowserNode.Kind.Schema -> loadTables(session, node, replace = false)
      SchemaBrowserNode.Kind.Table -> loadDetails(session, node)
      SchemaBrowserNode.Kind.Detail -> Unit
    }
  }

  private fun loadTables(
    session: DatabaseSession,
    node: SchemaBrowserNode,
    replace: Boolean,
  ) {
    load(node.id) {
      val tables = withContext(Dispatchers.IO) { session.listTables(SchemaInfo(node.label)) }
      updateChildren(node.id, tables.map { SchemaBrowserNodeHelper.tableNode(node.id, it) }, replace)
    }
  }

  private fun loadDetails(
    session: DatabaseSession,
    node: SchemaBrowserNode,
  ) {
    val table = node.table ?: return
    load(node.id) {
      val details = withContext(Dispatchers.IO) { session.describeTable(table) }
      updateChildren(node.id, SchemaBrowserNodeHelper.detailNodes(node.id, details), replace = true)
    }
  }

  private fun load(
    nodeId: String,
    action: suspend () -> Unit,
  ) {
    scope.launch {
      _loadingNodeIds.update { it + nodeId }
      _errors.update { it - nodeId }
      try {
        action()
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (exception: Exception) {
        _errors.update { it + (nodeId to (exception.message ?: "Could not load metadata")) }
      } finally {
        _loadingNodeIds.update { it - nodeId }
      }
    }
  }

  private fun updateChildren(
    parentId: String,
    children: List<SchemaBrowserNode>,
    replace: Boolean,
  ) {
    val currentNodes = _nodes.value
    val retained =
      if (replace) {
        currentNodes.filterNot { node ->
          node.parentId == parentId || SchemaBrowserNodeHelper.isDescendant(node, parentId, currentNodes)
        }
      } else {
        currentNodes
      }
    _nodes.value = retained + children
  }
}
