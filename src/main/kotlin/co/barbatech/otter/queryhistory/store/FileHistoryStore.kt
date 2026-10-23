package co.barbatech.otter.queryhistory.store

import co.barbatech.otter.osdirectory.OtterDirectoryHelper
import co.barbatech.otter.queryhistory.model.HistoryEntry
import co.barbatech.otter.queryhistory.model.HistorySettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Persistent history in the OS data directory: entries as JSON lines in query-history.jsonl and
 * settings in history-settings.json. Writes are atomic; unreadable data is backed up and reported.
 */
class FileHistoryStore(
  private val directory: Path = OtterDirectoryHelper.dataDirectory(),
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : HistoryStore {
  private val entriesFile = directory.resolve("query-history.jsonl")
  private val settingsFile = directory.resolve("history-settings.json")
  private val mutex = Mutex()
  private val entries = ArrayDeque<HistoryEntry>()
  private var loaded = false

  private val _settings = MutableStateFlow(HistorySettings())
  override val settings: StateFlow<HistorySettings> = _settings.asStateFlow()

  private val _lastError = MutableStateFlow<String?>(null)
  override val lastError: StateFlow<String?> = _lastError.asStateFlow()

  override suspend fun load() {
    mutex.withLock { ensureLoaded() }
  }

  override suspend fun updateSettings(settings: HistorySettings) {
    mutex.withLock {
      ensureLoaded()
      val sanitized = settings.copy(maxEntries = settings.maxEntries.coerceAtLeast(0))
      _settings.value = sanitized
      trim()
      writeSettings(sanitized)
      writeEntries()
    }
  }

  override suspend fun record(entry: HistoryEntry) {
    mutex.withLock {
      ensureLoaded()
      if (!_settings.value.enabled) return
      entries.addFirst(entry)
      trim()
      writeEntries()
    }
  }

  override suspend fun search(
    query: String,
    limit: Int,
  ): List<HistoryEntry> =
    mutex.withLock {
      ensureLoaded()
      HistoryEntriesHelper.matching(entries, query, limit)
    }

  override suspend fun clear() {
    mutex.withLock {
      ensureLoaded()
      entries.clear()
      writeEntries()
    }
  }

  private suspend fun ensureLoaded() {
    if (loaded) return
    withContext(dispatcher) {
      try {
        Files.createDirectories(directory)
      } catch (failure: IOException) {
        _lastError.value = "History directory $directory could not be created: ${failure.message}"
      }
      loadSettings()
      loadEntries()
    }
    loaded = true
  }

  private fun loadSettings() {
    if (!Files.exists(settingsFile)) return
    try {
      _settings.value =
        HistoryJsonCodec.decodeSettings(Files.readString(settingsFile, UTF_8), _settings.value)
    } catch (failure: Exception) {
      val backup = HistoryFileHelper.backupCorruptFile(settingsFile)
      _lastError.value =
        "History settings could not be read (${failure.message}); defaults are used." +
        (backup?.let { " The original file was kept at $it." } ?: "")
    }
  }

  private fun loadEntries() {
    if (!Files.exists(entriesFile)) return
    val invalidLines = mutableListOf<Int>()
    try {
      Files.readAllLines(entriesFile, UTF_8).forEachIndexed { index, line ->
        if (line.isBlank()) return@forEachIndexed
        val entry = runCatching { HistoryJsonCodec.decodeEntry(line) }.getOrNull()
        if (entry == null) invalidLines += index + 1 else entries.addLast(entry)
      }
    } catch (failure: IOException) {
      val backup = HistoryFileHelper.backupCorruptFile(entriesFile)
      _lastError.value =
        "History file $entriesFile could not be read: ${failure.message}" +
        (backup?.let { " The original file was kept at $it." } ?: "")
      return
    }
    trim()
    if (invalidLines.isNotEmpty()) {
      val backup = HistoryFileHelper.backupCorruptFile(entriesFile)
      writeEntries()
      _lastError.value =
        "History entries on lines ${invalidLines.joinToString()} could not be read and were skipped." +
        (backup?.let { " The original file was kept at $it." } ?: "")
    }
  }

  private fun writeSettings(settings: HistorySettings) {
    writeAtomically(settingsFile, HistoryJsonCodec.encodeSettings(settings))
  }

  private fun writeEntries() {
    val content =
      buildString {
        entries.forEach { entry ->
          append(HistoryJsonCodec.encodeEntry(entry).toString())
          append('\n')
        }
      }
    writeAtomically(entriesFile, content)
  }

  private fun writeAtomically(
    target: Path,
    content: String,
  ) {
    try {
      target.parent?.let { Files.createDirectories(it) }
      val temporary = Files.createTempFile(target.parent, target.fileName.toString(), ".tmp")
      try {
        Files.writeString(temporary, content, UTF_8)
        try {
          Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
          Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
        }
      } catch (failure: IOException) {
        Files.deleteIfExists(temporary)
        throw failure
      }
    } catch (failure: IOException) {
      _lastError.value = "History could not be saved to $target: ${failure.message}"
    }
  }

  private fun trim() {
    HistoryEntriesHelper.trim(entries, _settings.value.maxEntries)
  }
}
