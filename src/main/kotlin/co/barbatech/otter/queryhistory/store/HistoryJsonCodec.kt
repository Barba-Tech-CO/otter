package co.barbatech.otter.queryhistory.store

import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.model.HistorySettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.Duration
import java.time.Instant

/** JSON form of history entries and settings. */
internal object HistoryJsonCodec {
  private val json = Json { ignoreUnknownKeys = true }

  fun decodeSettings(
    text: String,
    defaults: HistorySettings,
  ): HistorySettings {
    val element = json.parseToJsonElement(text).jsonObject
    val enabled = element["enabled"]?.jsonPrimitive?.booleanOrNull ?: defaults.enabled
    val maxEntries = element["maxEntries"]?.jsonPrimitive?.intOrNull ?: defaults.maxEntries
    return HistorySettings(enabled, maxEntries.coerceAtLeast(0))
  }

  fun encodeSettings(settings: HistorySettings): String =
    buildJsonObject {
      put("enabled", settings.enabled)
      put("maxEntries", settings.maxEntries)
    }.toString()

  fun decodeEntry(line: String): HistoryEntry {
    val element = json.parseToJsonElement(line).jsonObject
    val durationMillis = element["durationMillis"]?.jsonPrimitive?.longOrNull ?: error("missing durationMillis")
    return HistoryEntry(
      connectionName = requiredText(element, "connectionName"),
      sql = requiredText(element, "sql"),
      executedAt = Instant.parse(requiredText(element, "executedAt")),
      duration = Duration.ofMillis(durationMillis),
      success = element["success"]?.jsonPrimitive?.booleanOrNull ?: error("missing success"),
      error = (element["error"] as? JsonPrimitive)?.contentOrNull,
    )
  }

  fun encodeEntry(entry: HistoryEntry): JsonObject =
    buildJsonObject {
      put("connectionName", entry.connectionName)
      put("sql", entry.sql)
      put("executedAt", entry.executedAt.toString())
      put("durationMillis", entry.duration.toMillis())
      put("success", entry.success)
      entry.error?.let { put("error", it) }
    }

  private fun requiredText(
    element: JsonObject,
    name: String,
  ): String = (element[name] as? JsonPrimitive)?.contentOrNull ?: error("missing $name")
}
