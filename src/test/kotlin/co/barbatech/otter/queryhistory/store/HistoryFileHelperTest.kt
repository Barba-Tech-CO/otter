package co.barbatech.otter.queryhistory.store

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryFileHelperTest {
  @Test
  fun aCorruptFileIsKeptUnderABackupName() {
    val directory = Files.createTempDirectory("history-backup")
    val source = Files.createTempFile(directory, "history", ".jsonl")
    Files.writeString(source, "not json")

    val backup = HistoryFileHelper.backupCorruptFile(source)

    assertNotNull(backup)
    assertTrue(Files.exists(backup))
    assertEquals(false, Files.exists(source))
  }

  @Test
  fun aMissingFileHasNoBackup() {
    val directory = Files.createTempDirectory("history-backup-missing")

    assertNull(HistoryFileHelper.backupCorruptFile(directory.resolve("missing.jsonl")))
  }
}
