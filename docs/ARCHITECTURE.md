# otter architecture

Kotlin + Compose Multiplatform Desktop (JVM 17). One package per component under `src/main/kotlin/co/barbatech/otter/`, one public type per file. No DI framework: `app/AppState.kt` wires everything by hand.

## Components

| Folder | Responsibility |
|--------|----------------|
| `app` | Theme, 3-area layout, status bar, `AppState` wiring. Only place that knows all components. |
| `connectionmanager` | Connection list UI and state: create, edit, delete, test, open, close. Exposes `activeSession`. `state/`: list and form state, status, test result and password prompt types. `ui/`: Compose list, row, form fields and dialogs. `helper/`: error text and native file chooser. |
| `connectionstore` | `ConnectionConfig`, `ConnectionStore`; JSON file in the OS config dir, no secrets. |
| `osdirectory` | `OtterDirectoryHelper`: per-user config and data directories for macOS, Windows and Linux. Flat, no role subfolders. |
| `secretstore` | `SecretStore`; OS keychain via java-keyring (`KeyringSecretStore`). |
| `database` | JDBC layer. `session/`: `DatabaseSession`, JDBC sessions and factory. `metadata/`: schema, table, column, index and foreign-key types. `result/`: `QueryResult`, cursors, rows, cell values. `error/`: `DatabaseException`, error kinds and JDBC error mapping. |
| `schemabrowser` | Lazy schema tree, filter, refresh, open table query. |
| `sqlsplitter` | `SqlStatementSplitter`: splits scripts, aware of strings and comments. |
| `sqleditor` | RSyntaxTextArea in `SwingPanel`, run/cancel, drives execution and history recording. |
| `resultgrid` | `state/`: grid state, tabs, paging view and selection. `sort/`: loaded-row sorting. `format/`: cell text, truncation, widths, status text. `clipboard/`: TSV export. `ui/tab/`: `ResultGrid`, tabs, tab content and error content. `ui/table/`: table, header, rows, cells, detail dialog, key handler and metrics. |
| `queryexport` | `format/`: CSV and JSON formats. `exporter/`: streaming exporter, progress and errors. `exporter/encoder/`: row encoders for CSV and JSON, value encoding and the encoder factory. `state/`: `ExportState`. `ui/`: `ExportDialog`. |
| `queryhistory` | `model/`: entries and settings. `store/`: `HistoryStore`, file and in-memory implementations. `state/`: `HistoryState`. `ui/`: history panel. |

### Folder rule

A component with up to about 8 files stays flat (`app`, `connectionstore`, `osdirectory`, `secretstore`, `schemabrowser`, `sqleditor`, `sqlsplitter`). A component above that splits into subfolders by technical role, never by business feature: `state`, `ui`, `tab`, `table`, `metadata`, `result`, `format`, `exporter`, `encoder`, `helper`, `store`, `model`, `error`, `sort`, `clipboard`, `session`. Folder names are lowercase single words. Test folders mirror the main layout. Factories are `object <X>Factory` at the component root (`ConnectionStoreFactory`, `HistoryStoreFactory`, ...) or in the role subfolder that owns the type (`queryexport/exporter/encoder/RowEncoderFactory`).

## Boundary rules

- Components talk through the contract types only; nobody edits another component's files. Imports name the declaring file's package; there are no re-exports or typealias shims.
- Allowed dependencies (caller -> callee), with the packages of the role subfolders:
  - `app` -> everything.
  - `connectionmanager` -> `connectionstore`, `secretstore`, `database.session`.
  - `database.session` -> `connectionstore` (`ConnectionConfig` only), `database.metadata`, `database.result`, `database.error`.
  - `schemabrowser` -> `database.session`, `database.metadata`.
  - `sqleditor` -> `database.session`, `database.result`, `sqlsplitter`, `queryhistory.store`, `resultgrid.state`.
  - `resultgrid.state` -> `database.result`, `queryexport.state`; `resultgrid.{ui.tab,ui.table}` -> `resultgrid.{state,sort,format,clipboard}`, `queryexport.ui`.
  - `queryexport.{format,exporter,exporter.encoder,state,ui}` -> `database.result`.
  - `connectionstore` -> `osdirectory`.
  - `queryhistory.{model,store,state,ui}` -> `osdirectory`.
  - `secretstore`, `sqlsplitter` -> nothing else.
- Schema browser and editor never see the connection manager: they receive `StateFlow<DatabaseSession?>` from `AppState`.
- Each component has an `object <X>Factory` at its root (`ConnectionStoreFactory`, `HistoryStoreFactory`, `SecretStoreFactory`, ...); the database session factory is the exception `DatabaseSessionFactoryProvider` (name clash with the `DatabaseSessionFactory` interface). `AppState` calls only these; the factory and the concrete class share the root or a role subfolder of the same component.
- UI state: one plain class per component with `StateFlow` properties and a `CoroutineScope` passed in. Composables take the state and a `Modifier`.
- Database and file work runs off the UI thread (`suspend`/`Flow`); the user's SQL is never rewritten (paging = JDBC fetch size + incremental cursor reads). PostgreSQL only streams with a fetch size when auto-commit is off: the `database` owner must handle that.
- SSH tunnel (Phase 2): a wrapper around `DatabaseSessionFactory` that starts the tunnel and opens the session with a config copy whose host/port point to the loopback port.

## Libraries

| Library | Version | License | Why |
|---------|---------|---------|-----|
| Kotlin / Compose Multiplatform | 2.4.21 / 1.12.2 | Apache 2.0 | Platform (already chosen). |
| compose material3 | 1.9.0 | Apache 2.0 | Direct coordinate; `compose.material3` is deprecated. |
| kotlinx-coroutines (core, swing, test) | 1.11.0 | Apache 2.0 | Async work; `swing` gives `Dispatchers.Main` on desktop. |
| kotlinx-serialization-json | 1.12.0 | Apache 2.0 | Connection config file. |
| pgjdbc | 42.7.14 | BSD-2-Clause | PostgreSQL driver. |
| xerial sqlite-jdbc | 3.53.4.0 | Apache 2.0 | SQLite driver. |
| RSyntaxTextArea | 4.0.1 | BSD-3-Clause | SQL editor with highlighting, undo, find, line numbers. |
| java-keyring | 1.0.4 | BSD-3-Clause | Only JVM library covering macOS Keychain, Windows Credential Manager and Linux Secret Service. Last release 2023: watch it. |
| kotlin-test + JUnit 6 | 2.4.21 / 6.1.3 | Apache 2.0 / EPL-2.0 | Tests (`useJUnitPlatform()`). |
| ktlint (Gradle plugin) | 14.2.0 (ktlint 1.8.0) | MIT / MIT | Style check. |

## Conventions

- Indentation is 2 spaces (`.editorconfig`); `./gradlew ktlintCheck` must pass (`ktlintFormat` fixes most issues).
- Stateless helpers are `object <Concern>Helper` in a role subfolder; factories are `object <X>Factory`; a loose top-level function is allowed only for `@Composable`, `main` and the `fun interface`; one type or composable per file; no `SCREAMING_CASE`, camelCase constants; single-use constants are inlined; nothing loose in the source root except `Main.kt`.
- Never re-export: no barrel files, no typealias shims. Import from the file that declares the symbol.
- Identifiers in English. Folders are per component, never per business feature.
- Before a PR: `./gradlew compileKotlin test ktlintCheck`.
