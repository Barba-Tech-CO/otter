package co.barbatech.otter.queryhistory.store

import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.model.HistorySettings
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HistoryJsonCodecTest {
  @Test
  fun entriesSurviveAnEncodeDecodeRoundTrip() {
    val entry =
      HistoryEntry(
        connectionName = "local",
        sql = "select 1",
        executedAt = Instant.parse("2026-10-23T10:00:00Z"),
        duration = Duration.ofMillis(12),
        success = false,
        error = "bad query",
      )

    val decoded = HistoryJsonCodec.decodeEntry(HistoryJsonCodec.encodeEntry(entry).toString())

    assertEquals(entry, decoded)
  }

  @Test
  fun anEntryWithoutErrorOmitsTheField() {
    val entry = HistoryEntry("local", "select 1", Instant.parse("2026-10-23T10:00:00Z"), Duration.ofMillis(1), true)

    val text = HistoryJsonCodec.encodeEntry(entry).toString()

    assertEquals(false, text.contains("\"error\""))
    assertEquals(entry, HistoryJsonCodec.decodeEntry(text))
  }

  @Test
  fun settingsMergeMissingKeysWithTheDefaultsAndCoerceTheBound() {
    assertEquals(
      HistorySettings(enabled = true, maxEntries = 0),
      HistoryJsonCodec.decodeSettings("""{"maxEntries":-3}""", HistorySettings(enabled = true, maxEntries = 7)),
    )
    assertEquals(
      HistorySettings(enabled = true, maxEntries = 20),
      HistoryJsonCodec.decodeSettings("""{"enabled":true}""", HistorySettings(enabled = false, maxEntries = 20)),
    )
  }

  @Test
  fun settingsRoundTrip() {
    val settings = HistorySettings(enabled = false, maxEntries = 42)

    assertEquals(
      settings,
      HistoryJsonCodec.decodeSettings(HistoryJsonCodec.encodeSettings(settings), HistorySettings()),
    )
  }

  @Test
  fun anEntryWithoutRequiredFieldsIsRejected() {
    assertFailsWith<IllegalStateException> { HistoryJsonCodec.decodeEntry("""{"sql":"select 1"}""") }
  }
}
