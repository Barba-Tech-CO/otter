package co.barbatech.otter.osdirectory

import java.nio.file.Path
import java.nio.file.Paths

/** Per-user otter directories: config for connections, data for query history. */
internal object OtterDirectoryHelper {
  fun configDirectory(
    osName: String = System.getProperty("os.name"),
    environment: Map<String, String> = System.getenv(),
    userHome: String = System.getProperty("user.home"),
  ): Path {
    val name = osName.lowercase()
    return when {
      name.contains("win") -> windowsDirectory(environment, userHome)
      name.contains("mac") -> macDirectory(userHome)
      else -> xdgDirectory(environment["XDG_CONFIG_HOME"], Paths.get(userHome, ".config"))
    }
  }

  fun dataDirectory(
    osName: String = System.getProperty("os.name"),
    environment: Map<String, String> = System.getenv(),
    userHome: String = System.getProperty("user.home"),
  ): Path {
    val name = osName.lowercase()
    return when {
      name.contains("mac") -> macDirectory(userHome)
      name.contains("win") -> windowsDirectory(environment, userHome)
      else -> xdgDirectory(environment["XDG_DATA_HOME"], Paths.get(userHome, ".local", "share"))
    }
  }

  private fun macDirectory(userHome: String): Path = Paths.get(userHome, "Library", "Application Support", "otter")

  private fun windowsDirectory(
    environment: Map<String, String>,
    userHome: String,
  ): Path {
    val appData = environment["APPDATA"] ?: Paths.get(userHome, "AppData", "Roaming").toString()
    return Paths.get(appData, "otter")
  }

  private fun xdgDirectory(
    variable: String?,
    fallback: Path,
  ): Path = if (variable.isNullOrBlank()) fallback.resolve("otter") else Paths.get(variable, "otter")
}
