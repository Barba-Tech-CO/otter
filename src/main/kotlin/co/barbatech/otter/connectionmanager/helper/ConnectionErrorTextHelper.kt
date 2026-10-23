package co.barbatech.otter.connectionmanager.helper

import co.barbatech.otter.database.error.DatabaseErrorKind
import co.barbatech.otter.database.error.DatabaseException
import kotlin.time.Duration

/** User-facing text for connection failures. */
internal object ConnectionErrorTextHelper {
  fun describe(error: DatabaseException): String {
    val label =
      when (error.kind) {
        DatabaseErrorKind.Unreachable -> "host unreachable"
        DatabaseErrorKind.AuthenticationFailed -> "authentication failed"
        DatabaseErrorKind.UnknownDatabase -> "unknown database"
        DatabaseErrorKind.QueryFailed -> "query failed"
        DatabaseErrorKind.Cancelled -> "cancelled"
        DatabaseErrorKind.Other -> "error"
      }
    val reason = error.message?.takeIf { it.isNotBlank() }
    return if (reason == null) label else "$label: $reason"
  }

  fun timeoutMessage(connectTimeout: Duration): String = "timed out after ${connectTimeout.inWholeMilliseconds} ms"

  fun message(error: Throwable): String =
    error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown error"
}
