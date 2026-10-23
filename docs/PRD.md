# otter - Product Requirements Document

Status: Approved by the owner. Items marked **Proposed** were part of the approved draft.

## 1. Overview

otter is a lightweight, open source desktop database client (Apache 2.0) built with Kotlin and Compose Multiplatform Desktop (JVM, Gradle). It targets Windows, macOS and Linux. It is inspired by DBeaver and is not affiliated with it.

**Problem.** Full database IDEs cover far more than most developers need day to day, and pay for it in startup time, memory use and interface complexity.

**Vision.** Cover only the essentials of a database client: connect, browse the schema, run SQL, inspect and export results. Fast startup, low memory, a small and focused interface.

**Current state.** Empty scaffold that opens a blank window.

## 2. Goals and non-goals

### Goals

- Cover the essential workflow: connection manager, schema browser, SQL editor, result grid, export, query history.
- Support SSH-tunneled connections inside the MVP.
- Store credentials in the OS keychain, never in plain text.
- Keep the app small and quick: startup time and memory are tracked as budgets (values TBD, to be measured).
- Ship native installers for Windows (MSI), macOS (DMG) and Linux (DEB).

### Non-goals (not in the MVP, and not planned unless the owner decides otherwise)

- ER diagrams.
- Schema compare / schema diff.
- Data transfer or migration between databases.
- Plugin or extension system.
- NoSQL databases.
- Visual query builder.
- Data editing in the grid (inline insert/update/delete). Proposed: read-only grid for the MVP.
- Server administration tooling (users, roles, backups, monitoring).

## 3. Target users

- Developers and analysts who run ad hoc queries and inspect schemas, and find a full IDE too heavy.
- Users who reach databases through an SSH bastion host.
- Users on all three desktop operating systems who want one consistent tool.

## 4. Scope and phasing

| Phase | Contents |
|-------|----------|
| **MVP Phase 1 (core)** | Connection manager, SQLite and PostgreSQL support, schema browser, SQL editor, result grid, CSV/JSON export, query history |
| **MVP Phase 2** | SSH tunnel (**Proposed** placement, see section 8.3), MySQL/MariaDB support (**Proposed**) |
| **Post-MVP** | See list below |

Post-MVP candidates (not committed): additional databases, multiple editor tabs beyond the minimum, saved snippets/favorites, DDL viewer, grid editing, themes beyond light/dark, auto-update.

Phase tags used in section 5: `[P1]` = MVP Phase 1, `[P2]` = MVP Phase 2.

The SSH tunnel stays inside the MVP, as requested. MVP is considered complete only when Phase 2 is done.

## 5. Functional requirements

### 5.1 Connection manager `[P1]`

Create, edit, delete, duplicate and test saved connections.

Acceptance criteria:
- A user can create a connection by choosing a database type and entering host, port, database, user and password (SQLite: file path only).
- "Test connection" reports success, or a specific failure reason (unreachable host, bad credentials, unknown database), within the configured connect timeout.
- Connections persist across restarts.
- Passwords (and SSH passwords/passphrases once SSH exists) are stored only in the OS keychain (macOS Keychain, Windows Credential Manager, Linux Secret Service). They never appear in the connection config file, logs or exports.
- If no keychain is available (e.g. headless Linux without Secret Service), the app tells the user and offers "prompt each time"; it does not silently fall back to plain text.
- Editing or deleting a connection updates the keychain entry accordingly; deleting removes the secret.
- Connections can be opened and closed independently; closing releases the JDBC connection.

### 5.2 Schema browser `[P1]`

Tree view of the connected database.

Acceptance criteria:
- Shows schemas (where the database has them), tables and views.
- Expanding a table shows columns (name, type, nullability, default, primary key marker), indexes (name, columns, uniqueness) and foreign keys (columns and referenced table/columns).
- Metadata loads lazily per expanded node and does not block the UI thread.
- A refresh action reloads the selected node.
- Filter box narrows visible objects by name.
- Double-click or context action on a table opens a new query pre-filled with a `SELECT` limited by the page size.

### 5.3 SQL editor `[P1]`

Acceptance criteria:
- Syntax highlighting for SQL keywords, strings, numbers, comments and identifiers.
- Run selection if text is selected, otherwise run the whole script; keyboard shortcut documented in the UI.
- Statements are split correctly for scripts with multiple statements (including semicolons inside strings and comments).
- Multiple statements produce multiple result sets, each reachable in a result tab; statements with no result set show the affected row count.
- A running query can be cancelled (JDBC `Statement.cancel`); the UI stays responsive during execution and returns to idle after cancel.
- Errors show the database message and, when the driver provides it, the position.
- Basic editing: undo/redo, find, line numbers, tab indentation.
- Auto-completion is out of Phase 1 (post-MVP).

### 5.4 Result grid `[P1]`

Acceptance criteria:
- Results are paged or lazily fetched: a result with a very large row count does not load fully into memory at once (fetch size and page size configurable; defaults TBD to be measured).
- The grid is virtualized: only visible rows are composed.
- Column sorting is available for the loaded data; server-side re-sort when the result is paged is **Proposed** for Phase 1 only if it can be done without rewriting the user's SQL unexpectedly, otherwise sorting applies to the current page and the UI says so.
- Copy cell, row(s) and selection to the clipboard (tab-separated for multi-cell).
- NULL is visually distinct from the empty string.
- Long values are truncated in the cell and viewable in full via a detail view.
- Status bar shows row count, elapsed time and the page position.

### 5.5 Export CSV / JSON `[P1]`

Acceptance criteria:
- Export the current result set (all rows by re-running the query, or the loaded page only; user chooses) to CSV or JSON.
- CSV: configurable delimiter, header row toggle, correct quoting/escaping of delimiters, quotes and newlines, UTF-8.
- JSON: array of objects keyed by column name; NULL exported as JSON `null`; types mapped without silent loss (large numbers and binary handled explicitly, documented).
- Export streams to disk; it does not require the full result in memory.
- Export can be cancelled and reports success or failure with the output path.

### 5.6 Query history `[P1]`

Acceptance criteria:
- Every executed statement is recorded with connection name, timestamp, duration and success/failure.
- History is searchable and a past entry can be reopened in the editor with one action.
- History persists across restarts, can be cleared by the user, and has a bounded size (limit TBD).
- Statements are stored locally only. Because SQL can contain sensitive literals, the user can disable history recording.

### 5.7 SSH tunnel `[P2]` (**Proposed** phase)

Local port forwarding from a loopback port to the database host:port as seen from the SSH server.

Acceptance criteria:
- A connection can enable "Connect through SSH" with SSH host, port, user and authentication method.
- Authentication by password.
- Authentication by private key file, with optional passphrase (OpenSSH-format keys at minimum; supported key types listed in the docs once verified).
- SSH secrets (password, passphrase) are stored in the OS keychain under the same rules as 5.1.
- The tunnel binds to a loopback address on an ephemeral local port; the JDBC URL targets that port. The port is never exposed on non-loopback interfaces.
- Host key verification uses the user's `known_hosts`: a known matching key connects; an unknown key prompts the user to accept (showing the fingerprint) and, on accept, records it; a changed key blocks the connection with a clear warning. There is no option that silently disables verification.
- Connect and authentication timeouts are configurable with sensible defaults (values TBD).
- Failures return distinct, readable messages: SSH host unreachable, authentication failed, host key mismatch, tunnel could not reach the database host, database authentication failed.
- Closing the connection (or exiting the app) closes the tunnel and its local listener. A dropped SSH session marks the connection as disconnected and tells the user.
- "Test connection" exercises the full path (SSH, then database).
- Jump hosts / ProxyJump, SSH agent and `~/.ssh/config` parsing are not in the MVP.

## 6. Supported databases

**Proposed** order:

1. SQLite and PostgreSQL (Phase 1).
2. MySQL and MariaDB (Phase 2).
3. Others after the MVP, by demand.

JDBC driver licenses, read from each driver's published Maven POM (versions are the current releases found on Maven Central on 2026-10-23):

| Database | Driver | Version checked | License (per POM) | Fit with Apache 2.0 |
|----------|--------|-----------------|-------------------|---------------------|
| PostgreSQL | `org.postgresql:postgresql` (pgjdbc) | 42.7.14 | BSD-2-Clause | Permissive; compatible |
| SQLite | `org.xerial:sqlite-jdbc` | 3.53.4.0 | Apache 2.0 | Compatible |
| MySQL | `com.mysql:mysql-connector-j` | 26.10.0 | GPL v2 with Universal FOSS Exception v1.0 | Unconfirmed, see Open questions |
| MariaDB / MySQL | `org.mariadb.jdbc:mariadb-java-client` | 4.0.0 | LGPL-2.1-or-later | Unconfirmed for bundling, see Open questions |

Notes:
- Only the license field of the POM was checked. Whether bundling a GPL-with-exception or LGPL driver in an Apache 2.0 distribution is acceptable, and how it must be attributed, has not been legally verified. This is why MySQL/MariaDB is in Phase 2 and flagged as an open question.
- Possible alternative that avoids bundling: let the user supply the driver JAR. This is **Proposed** only as a fallback if bundling is not acceptable.
- Third-party license notices must be shipped with the installers for every bundled dependency.

## 7. Non-functional requirements

- **Lightweight.** Cold start time: TBD (to be measured). Idle memory: TBD (to be measured). Baseline measurements on all three OSes are taken before setting budgets, and regressions are checked per release.
- **Cross-platform.** Windows, macOS, Linux; behavior and keyboard shortcuts consistent, with platform conventions for modifier keys.
- **Security.**
  - Secrets only in the OS keychain; never logged or exported.
  - SSH host key verification always on (see 5.7).
  - Connection config files contain no secrets.
  - Query history is local and can be disabled.
  - No telemetry in the MVP.
- **Accessibility (basic).** Every core action is reachable by keyboard; visible focus; logical tab order; sufficient contrast in light and dark themes.
- **Responsiveness.** Database and network calls never run on the UI thread; long operations are cancellable.
- **Packaging.** Use the existing Compose Desktop tasks: `packageDmg` (macOS), `packageMsi` (Windows), `packageDeb` (Linux). Each is built on its own OS. Code signing and notarization: see Open questions.
- **Code conventions.** 2-space indentation, Conventional Commits, `./gradlew compileKotlin` before PRs (per README).

## 8. Technical considerations

### 8.1 Architecture (high level)

- Kotlin on JVM 17+, Compose Desktop UI (Material 3 already a dependency).
- JDBC for all database access, behind a small interface per database so dialect differences (metadata queries, paging syntax, type mapping) stay isolated.
- Kotlin coroutines for query execution, metadata loading and export, off the UI thread, with cancellation mapped to `Statement.cancel`.
- Connections, schema metadata, editor state and results held in separate, small state holders so the UI stays simple.
- Local storage: a small config file for connections (no secrets), and local storage for history (format TBD).
- Keychain access through a JVM library or per-OS integration: library choice is not yet researched (Open questions).

### 8.2 SQL editor component

Options:
- **RSyntaxTextArea via `SwingPanel`.** Mature Swing editor with SQL highlighting, undo, find and line numbers out of the box. Cost: Swing interop inside Compose (focus, theming, sizing), and an extra dependency.
- **Compose text field with custom highlighting.** Native look and no interop, but highlighting, large-text performance, line numbers, undo and find must all be built.

**Proposed:** start with RSyntaxTextArea via `SwingPanel` for Phase 1, because it delivers the editor requirements with the least code, in line with the "essentials only" goal. Revisit if interop issues (theming, focus, accessibility) prove costly. License of RSyntaxTextArea not yet verified (Open questions).

### 8.3 SSH tunnel: libraries and complexity

Library comparison, with data from Maven Central POMs and context7 (checked 2026-10-23):

| Library | License (per POM) | Current version on Maven Central | Port forwarding | Notes |
|---------|-------------------|----------------------------------|-----------------|-------|
| sshj (`com.hierynomus:sshj`) | Apache 2.0 | 0.41.2 (updated 2026-10-12) | `newLocalPortForwarder(Parameters, ServerSocket)`; `listen()` blocks until interrupted, `close()` stops it | `loadKnownHosts()` for `~/.ssh/known_hosts`; `loadKeys(path, passphrase)`; documented in context7 |
| Apache MINA SSHD (`org.apache.sshd:sshd-core`) | Apache 2.0 | 2.20.0 is the newest non-milestone release found; 3.0.0-M6 is a milestone (2026-09-28) | Supported (session port forwarding API) | Larger, client and server, more modules; more surface than needed |
| JSch, mwiede fork (`com.github.mwiede:jsch`) | Revised BSD and ISC (per POM) | 2.28.8 (updated 2026-10-10) | Supported | Low-level API; known_hosts handling and error reporting need more wiring |

Only the facts in the table were verified. Key-type and cipher support per library were not verified and must be tested against real servers.

**Proposed:** sshj. Apache 2.0 matches the project license, the API is small and directly matches the need (local forwarder, known_hosts, key with passphrase), and the library is actively released. MINA SSHD is a reasonable alternative if sshj fails a compatibility test.

**Complexity assessment and phasing (Proposed: MVP Phase 2).** The SSH tunnel is more complex than the core features for these reasons:
1. It adds a new failure surface on top of the database connection: network, SSH auth, host key trust and tunnel lifecycle each need distinct, clear errors.
2. Host key verification needs UI (accept/reject unknown, block changed) and correct `known_hosts` handling, with no silent bypass.
3. It needs a second set of secrets (SSH password, key passphrase) in the keychain.
4. Lifecycle must be correct: ephemeral loopback port, forwarder thread, teardown on close/exit, handling of dropped sessions.
5. It needs a real SSH server for tests (e.g. a container), which Phase 1 does not.

None of this blocks Phase 1, and the connection manager can be designed in Phase 1 so that a tunnel is a wrapper that yields a host:port for the JDBC URL. Placing it in Phase 2 keeps it inside the MVP while letting Phase 1 ship and be validated first.

### 8.4 Result grid

Compose lazy layout (`LazyColumn`/lazy rows) over a paged data source. Only fetched pages stay in memory. Column widths are computed from a sample of rows, not the full result.

## 9. Phasing / milestones (ordered, no dates)

**MVP Phase 1**
1. App shell: window layout, navigation, theme, local config storage.
2. Connection manager with keychain storage; SQLite and PostgreSQL drivers; test connection.
3. Schema browser.
4. SQL editor with execution, cancellation and history recording.
5. Result grid with lazy fetch, sort, copy.
6. CSV/JSON export.
7. Query history UI.
8. Packaging for all three OSes; baseline performance measurements; third-party license notices.

**MVP Phase 2**
9. SSH tunnel (password and key auth, known_hosts verification, errors, teardown, integration tests with a test SSH server).
10. MySQL/MariaDB support, after the driver license question is resolved.
11. MVP stabilization and release.

## 10. Risks and mitigations

| Risk | Mitigation |
|------|------------|
| `SwingPanel` interop problems (focus, theming, HiDPI) | Isolate the editor behind an interface so it can be replaced |
| OS keychain access differs per platform, and is missing on some Linux setups | Evaluate libraries early; "prompt each time" fallback; no plain-text storage |
| Large result sets exhaust memory | Lazy fetch, virtualized grid, streaming export; measure |
| SSH compatibility with real servers (key types, ciphers) | Integration tests against a real SSH server; fallback to MINA SSHD |
| Driver license conflicts with Apache 2.0 | Resolve before bundling MySQL/MariaDB drivers; user-supplied JAR as fallback |
| Scope creep toward a full IDE | Non-goals list; any new feature needs an owner decision |
| JVM startup and memory may miss "lightweight" expectations | Measure early; JVM options and packaging (e.g. trimmed runtime) investigated if budgets are missed |

## 11. Open questions (owner decisions)

1. Approve the phase placement of the SSH tunnel (Phase 2) and the library choice (sshj)?
2. Approve the database order (SQLite and PostgreSQL first, MySQL/MariaDB in Phase 2)?
3. May the MySQL (GPL v2 with Universal FOSS Exception) and MariaDB (LGPL-2.1-or-later) drivers be bundled with an Apache 2.0 app, or should users supply the driver JAR?
4. Approve RSyntaxTextArea via `SwingPanel` for the editor, and is its license acceptable (not yet verified)?
5. Which library or approach for OS keychain access (not yet researched)?
6. Is a read-only result grid acceptable for the MVP (editing is a non-goal here)?
7. What are the startup time and memory budgets (to be measured first, then set)?
8. Is code signing / notarization (macOS, Windows) required for the first release?
9. Minimum JDK and bundled runtime: keep JDK 17 as the minimum?
10. Should query history recording be on or off by default?
