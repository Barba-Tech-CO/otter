package co.barbatech.otter.database.metadata

data class IndexInfo(
  val name: String,
  val columns: List<String>,
  val unique: Boolean,
)
