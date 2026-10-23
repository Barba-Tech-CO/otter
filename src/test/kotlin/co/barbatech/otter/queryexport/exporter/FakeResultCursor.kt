package co.barbatech.otter.queryexport.exporter

import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.ResultCursor
import co.barbatech.otter.database.result.Row
import kotlinx.coroutines.delay

internal class FakeResultCursor(
  override val columns: List<ResultColumn>,
  rows: List<Row>,
  private val fetchDelayMillis: Long = 0,
  private val fetchFailure: Exception? = null,
) : ResultCursor {
  private val remaining = ArrayDeque(rows)
  val fetchCalls = mutableListOf<Int>()
  var isClosed = false
    private set

  override val hasMore: Boolean get() = remaining.isNotEmpty()

  override suspend fun fetch(count: Int): List<Row> {
    fetchCalls += count
    fetchFailure?.let { throw it }
    if (fetchDelayMillis > 0) delay(fetchDelayMillis)
    val taken = mutableListOf<Row>()
    repeat(count) {
      if (remaining.isNotEmpty()) taken += remaining.removeFirst()
    }
    return taken
  }

  override fun close() {
    isClosed = true
  }
}
