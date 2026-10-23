package co.barbatech.otter.queryhistory.model

/** Recording switch and size bound. Defaults are provisional (PRD open question 10). */
data class HistorySettings(
  val enabled: Boolean = true,
  val maxEntries: Int = 1000,
)
