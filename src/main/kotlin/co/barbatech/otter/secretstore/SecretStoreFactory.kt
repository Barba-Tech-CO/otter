package co.barbatech.otter.secretstore

/** Single place the app asks for the store: the OS keychain. */
object SecretStoreFactory {
  fun keyringStore(): SecretStore = KeyringSecretStore()
}
