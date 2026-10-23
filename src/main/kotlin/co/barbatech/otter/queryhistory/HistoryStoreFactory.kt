package co.barbatech.otter.queryhistory

import co.barbatech.otter.queryhistory.store.FileHistoryStore
import co.barbatech.otter.queryhistory.store.HistoryStore

/** Single place the app asks for the store. */
object HistoryStoreFactory {
  fun fileStore(): HistoryStore = FileHistoryStore()
}
