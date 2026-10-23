package co.barbatech.otter.database.error

/** Coarse failure category so the UI can show a specific reason. */
enum class DatabaseErrorKind {
  Unreachable,
  AuthenticationFailed,
  UnknownDatabase,
  QueryFailed,
  Cancelled,
  Other,
}
