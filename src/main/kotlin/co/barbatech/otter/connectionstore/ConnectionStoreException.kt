package co.barbatech.otter.connectionstore

/** A connections file that cannot be read or written, for example because its JSON is damaged. */
class ConnectionStoreException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)
