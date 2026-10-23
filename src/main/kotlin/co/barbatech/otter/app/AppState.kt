package co.barbatech.otter.app

import co.barbatech.otter.connectionmanager.state.ConnectionManagerState
import co.barbatech.otter.connectionstore.ConnectionStoreFactory
import co.barbatech.otter.database.DatabaseSessionFactoryProvider
import co.barbatech.otter.queryexport.ResultExporterFactory
import co.barbatech.otter.queryexport.state.ExportState
import co.barbatech.otter.queryhistory.HistoryStoreFactory
import co.barbatech.otter.queryhistory.state.HistoryState
import co.barbatech.otter.resultgrid.state.ResultGridState
import co.barbatech.otter.schemabrowser.SchemaBrowserState
import co.barbatech.otter.secretstore.SecretStoreFactory
import co.barbatech.otter.sqleditor.SqlEditorState
import co.barbatech.otter.sqlsplitter.SqlStatementSplitterFactory
import kotlinx.coroutines.CoroutineScope

/** Manual wiring of all components. No DI framework: each component gets exactly what it needs. */
class AppState(
  scope: CoroutineScope,
) {
  private val historyStore = HistoryStoreFactory.fileStore()

  val connectionManager =
    ConnectionManagerState(
      store = ConnectionStoreFactory.jsonFileStore(),
      secrets = SecretStoreFactory.keyringStore(),
      sessions = DatabaseSessionFactoryProvider.jdbcSessionFactory(),
      scope = scope,
    )
  val resultGrid = ResultGridState(ExportState(ResultExporterFactory.streamingExporter(), scope), scope)
  val sqlEditor =
    SqlEditorState(
      activeSession = connectionManager.activeSession,
      splitter = SqlStatementSplitterFactory.defaultSplitter(),
      history = historyStore,
      resultGrid = resultGrid,
      scope = scope,
    )
  val schemaBrowser = SchemaBrowserState(connectionManager.activeSession, sqlEditor::setText, scope)
  val queryHistory = HistoryState(historyStore, sqlEditor::setText, scope)
}
