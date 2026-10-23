package co.barbatech.otter.database.error

/** Any database failure. [position] is the 1-based character offset in the SQL when the driver reports it. */
class DatabaseException(
  val kind: DatabaseErrorKind,
  message: String,
  val position: Int? = null,
  cause: Throwable? = null,
) : Exception(message, cause)
