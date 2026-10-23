package co.barbatech.otter.connectionstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Stores connection configs as a JSON array in [file]. Contains no secrets.
 * Writes go to a temporary file in the same directory and are then moved into place.
 */
class JsonConnectionStore(
  private val file: Path,
) : ConnectionStore {
  private val json =
    Json {
      prettyPrint = true
      encodeDefaults = true
      ignoreUnknownKeys = true
    }
  private val serializer = ListSerializer(ConnectionConfig.serializer())

  override suspend fun list(): List<ConnectionConfig> = withContext(Dispatchers.IO) { read() }

  override suspend fun save(config: ConnectionConfig) {
    withContext(Dispatchers.IO) {
      val updated = read().filterNot { it.id == config.id } + config
      write(updated)
    }
  }

  override suspend fun delete(id: String) {
    withContext(Dispatchers.IO) {
      if (!Files.exists(file)) return@withContext
      write(read().filterNot { it.id == id })
    }
  }

  private fun read(): List<ConnectionConfig> {
    if (!Files.exists(file)) return emptyList()
    val text =
      try {
        Files.readString(file)
      } catch (e: IOException) {
        throw ConnectionStoreException("Could not read the connections file $file.", e)
      }
    return try {
      json.decodeFromString(serializer, text)
    } catch (e: SerializationException) {
      throw ConnectionStoreException(
        "The connections file $file is damaged. Fix or delete it and restart otter; it was not overwritten.",
        e,
      )
    }
  }

  private fun write(connections: List<ConnectionConfig>) {
    val directory = file.toAbsolutePath().parent
    Files.createDirectories(directory)
    val temporary = Files.createTempFile(directory, ".connections", ".tmp")
    try {
      Files.writeString(temporary, json.encodeToString(serializer, connections))
      try {
        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
      } catch (e: AtomicMoveNotSupportedException) {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
      }
    } catch (e: IOException) {
      Files.deleteIfExists(temporary)
      throw ConnectionStoreException("Could not save the connections file $file.", e)
    }
  }
}
