package co.barbatech.otter.schemabrowser

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Lazy, keyboard-accessible view of schemas, tables, and table metadata. */
@Composable
fun SchemaBrowser(
  state: SchemaBrowserState,
  modifier: Modifier = Modifier,
) {
  val filter by state.filter.collectAsState()
  val nodes by state.nodes.collectAsState()
  val loading by state.loadingNodeIds.collectAsState()
  val errors by state.errors.collectAsState()
  val expanded by state.expandedNodeIds.collectAsState()
  val visible = state.visibleNodes()
  Column(modifier.padding(8.dp).fillMaxSize()) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = filter,
        onValueChange = state::setFilter,
        label = { Text("Filter objects") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      Button(onClick = state::refresh) { Text("Refresh") }
    }
    errors["root"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    LazyColumn(Modifier.fillMaxSize()) {
      items(visible, key = { it.id }) { node ->
        val canExpand = node.kind != SchemaBrowserNode.Kind.Detail
        val isExpanded = node.id in expanded
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .combinedClickable(
                onClick = { state.toggle(node) },
                onDoubleClick = { node.table?.let(state::openTable) },
              ).padding(start = (state.depth(node, nodes) * 16).dp, top = 4.dp, bottom = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            if (canExpand) {
              if (isExpanded) {
                "▾"
              } else {
                "▸"
              }
            } else {
              "•"
            },
            color = MaterialTheme.colorScheme.primary,
          )
          Text(node.label, modifier = Modifier.weight(1f).padding(start = 6.dp))
          if (node.table != null) Button(onClick = { state.openTable(node.table) }) { Text("Open") }
          if (node.id in loading) Text("Loading…", modifier = Modifier.padding(start = 6.dp))
        }
        errors[node.id]?.let { error ->
          Text(
            error,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = ((state.depth(node, nodes) * 16) + 24).dp),
          )
        }
      }
    }
  }
}
