package co.barbatech.otter.connectionstore

import co.barbatech.otter.osdirectory.OtterDirectoryHelper

/** Single place the app asks for the store: a JSON file in the OS config directory. */
object ConnectionStoreFactory {
  fun jsonFileStore(): ConnectionStore =
    JsonConnectionStore(OtterDirectoryHelper.configDirectory().resolve("connections.json"))
}
