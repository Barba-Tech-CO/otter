package co.barbatech.otter.osdirectory

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class OtterDirectoryHelperTest {
  @Test
  fun macOSUsesApplicationSupport() {
    assertEquals(
      Paths.get("/Users/dev/Library/Application Support/otter"),
      OtterDirectoryHelper.configDirectory("Mac OS X", emptyMap(), "/Users/dev"),
    )
  }

  @Test
  fun windowsUsesAppData() {
    assertEquals(
      Paths.get("C:\\Users\\dev\\AppData\\Roaming", "otter"),
      OtterDirectoryHelper.configDirectory(
        "Windows 11",
        mapOf("APPDATA" to "C:\\Users\\dev\\AppData\\Roaming"),
        "C:\\Users\\dev",
      ),
    )
  }

  @Test
  fun windowsFallsBackToTheDefaultRoamingFolder() {
    assertEquals(
      Paths.get("C:\\Users\\dev", "AppData", "Roaming", "otter"),
      OtterDirectoryHelper.configDirectory("Windows 10", emptyMap(), "C:\\Users\\dev"),
    )
  }

  @Test
  fun linuxUsesXdgConfigHome() {
    assertEquals(
      Paths.get("/home/dev/custom-config/otter"),
      OtterDirectoryHelper.configDirectory(
        "Linux",
        mapOf("XDG_CONFIG_HOME" to "/home/dev/custom-config"),
        "/home/dev",
      ),
    )
  }

  @Test
  fun linuxFallsBackToDotConfig() {
    assertEquals(
      Paths.get("/home/dev/.config/otter"),
      OtterDirectoryHelper.configDirectory("Linux", emptyMap(), "/home/dev"),
    )
  }

  @Test
  fun macOSDataUsesApplicationSupport() {
    assertEquals(
      Paths.get("/Users/dev/Library/Application Support/otter"),
      OtterDirectoryHelper.dataDirectory("Mac OS X", emptyMap(), "/Users/dev"),
    )
  }

  @Test
  fun windowsDataUsesAppData() {
    assertEquals(
      Paths.get("C:\\Users\\dev\\AppData\\Roaming", "otter"),
      OtterDirectoryHelper.dataDirectory(
        "Windows 11",
        mapOf("APPDATA" to "C:\\Users\\dev\\AppData\\Roaming"),
        "C:\\Users\\dev",
      ),
    )
  }

  @Test
  fun linuxDataUsesXdgDataHome() {
    assertEquals(
      Paths.get("/home/dev/custom-data/otter"),
      OtterDirectoryHelper.dataDirectory(
        "Linux",
        mapOf("XDG_DATA_HOME" to "/home/dev/custom-data"),
        "/home/dev",
      ),
    )
  }

  @Test
  fun linuxDataFallsBackToLocalShare() {
    assertEquals(
      Paths.get("/home/dev/.local/share/otter"),
      OtterDirectoryHelper.dataDirectory("Linux", emptyMap(), "/home/dev"),
    )
  }
}
