package co.barbatech.otter.queryhistory.store

import co.barbatech.otter.queryhistory.model.HistoryEntry
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryEntriesHelperTest {
  private fun entry(
    sql: String,
    connection: String = "local",
  ): HistoryEntry = HistoryEntry(connection, sql, Instant.parse("2026-10-23T10:00:00Z"), Duration.ofMillis(1), true)

  @Test
  fun matchingSearchesSqlAndConnectionNameAndHonoursTheLimit() {
    val entries = listOf(entry("select 1"), entry("update users", "prod"), entry("select 2"))

    assertEquals(listOf("select 1", "select 2"), HistoryEntriesHelper.matching(entries, "SELECT", 10).map { it.sql })
    assertEquals(listOf("update users"), HistoryEntriesHelper.matching(entries, "prod", 10).map { it.sql })
    assertEquals(1, HistoryEntriesHelper.matching(entries, "", 1).size)
    assertEquals(emptyList(), HistoryEntriesHelper.matching(entries, "missing", 10))
  }

  @Test
  fun trimDropsTheOldestEntriesBeyondTheBound() {
    val entries = ArrayDeque(listOf(entry("one"), entry("two"), entry("three")))

    HistoryEntriesHelper.trim(entries, 2)

    assertEquals(listOf("one", "two"), entries.map { it.sql })
  }
}
