package co.barbatech.otter.database.error

import org.postgresql.util.PSQLException

internal object DatabaseErrors {
  fun from(exception: Exception): DatabaseException {
    if (exception is DatabaseException) return exception
    val sqlException =
      generateSequence<Throwable>(exception) { it.cause }
        .filterIsInstance<java.sql.SQLException>()
        .firstOrNull()
    val state = sqlException?.sqlState
    val message = exception.message ?: "Database operation failed"
    val unreachable = state?.startsWith("08") == true || message.contains("unable to open", ignoreCase = true)
    val authenticationFailed = state == "28P01" || state?.startsWith("28") == true
    val unknownDatabase = state == "3D000" || message.contains("no such database", ignoreCase = true)
    val cancelled = state == "57014" || message.contains("interrupt", ignoreCase = true)
    val kind =
      when {
        unreachable -> DatabaseErrorKind.Unreachable
        authenticationFailed -> DatabaseErrorKind.AuthenticationFailed
        unknownDatabase -> DatabaseErrorKind.UnknownDatabase
        cancelled -> DatabaseErrorKind.Cancelled
        sqlException != null -> DatabaseErrorKind.QueryFailed
        else -> DatabaseErrorKind.Other
      }
    val position =
      generateSequence<Throwable>(exception) { it.cause }
        .filterIsInstance<PSQLException>()
        .firstNotNullOfOrNull { it.serverErrorMessage?.position }
    return DatabaseException(kind, message, position, exception)
  }
}
