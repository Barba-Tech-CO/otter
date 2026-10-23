package co.barbatech.otter.secretstore

import com.github.javakeyring.BackendNotSupportedException
import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OS keychain through java-keyring: macOS Keychain, Windows Credential Manager, Linux Secret Service.
 * Blocking calls run on [Dispatchers.IO]; there is no plain text fallback.
 */
class KeyringSecretStore(
  private val service: String = "otter",
) : SecretStore {
  private val keyring: Keyring by lazy {
    try {
      Keyring.create()
    } catch (e: BackendNotSupportedException) {
      throw SecretStoreUnavailableException("No OS keychain is available on this system.", e)
    }
  }

  override suspend fun get(connectionId: String): String? =
    withContext(Dispatchers.IO) {
      synchronized(keyring) {
        try {
          keyring.getPassword(service, connectionId)
        } catch (e: PasswordAccessException) {
          null
        }
      }
    }

  override suspend fun set(
    connectionId: String,
    secret: String,
  ) {
    withContext(Dispatchers.IO) {
      synchronized(keyring) {
        try {
          keyring.setPassword(service, connectionId, secret)
        } catch (e: PasswordAccessException) {
          throw SecretStoreUnavailableException("Could not store the password in the OS keychain.", e)
        }
      }
    }
  }

  override suspend fun delete(connectionId: String) {
    withContext(Dispatchers.IO) {
      synchronized(keyring) {
        try {
          keyring.deletePassword(service, connectionId)
        } catch (e: PasswordAccessException) {
          // Backends report a missing entry differently; deleting it is not an error.
        }
      }
    }
  }
}
