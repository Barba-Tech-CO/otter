package co.barbatech.otter.secretstore

/** The OS has no usable keychain (for example headless Linux without Secret Service). */
class SecretStoreUnavailableException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)
