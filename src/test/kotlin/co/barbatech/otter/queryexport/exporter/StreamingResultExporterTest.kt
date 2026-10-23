package co.barbatech.otter.queryexport.exporter

import co.barbatech.otter.database.result.CellValue
import co.barbatech.otter.database.result.ResultColumn
import co.barbatech.otter.database.result.Row
import co.barbatech.otter.queryexport.exporter.ExportException
import co.barbatech.otter.queryexport.exporter.StreamingResultExporter
import co.barbatech.otter.queryexport.format.CsvExportFormat
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamingResultExporterTest {
  private val columns = listOf(ResultColumn("n", "int"))

  @Test
  fun `reads in chunks and reports progress`() =
    runTest {
      val rows = (1..1205).map { Row(listOf(CellValue.Text(it.toString()))) }
      val cursor = FakeResultCursor(columns, rows)
      val target = Files.createTempDirectory("chunk-export").resolve("out.csv")

      val progress = StreamingResultExporter().export(cursor, CsvExportFormat(), target).toList()

      assertEquals(listOf(500, 500, 500), cursor.fetchCalls)
      assertEquals(listOf(500L, 1000L, 1205L, 1205L), progress.map { it.rowsWritten })
      assertEquals(listOf(false, false, false, true), progress.map { it.finished })
      assertTrue(cursor.isClosed)
    }

  @Test
  fun `cancellation removes the partial file and closes the cursor`() =
    runTest {
      val exporter = StreamingResultExporter(StandardTestDispatcher(testScheduler))
      val rows = listOf(Row(listOf(CellValue.Text("1"))))
      val cursor = FakeResultCursor(columns, rows, fetchDelayMillis = 100)
      val target = Files.createTempDirectory("cancel-export").resolve("out.csv")

      val job = launch { exporter.export(cursor, CsvExportFormat(), target).collect { } }
      runCurrent()
      assertTrue(Files.exists(target))

      job.cancelAndJoin()

      assertFalse(Files.exists(target))
      assertTrue(cursor.isClosed)
    }

  @Test
  fun `failure reports the path and removes the partial file`() =
    runTest {
      val rows = listOf(Row(listOf(CellValue.Text("1"))))
      val cursor = FakeResultCursor(columns, rows, fetchFailure = IOException("disk gone"))
      val target = Files.createTempDirectory("fail-export").resolve("out.csv")

      val failure =
        assertFailsWith<ExportException> {
          StreamingResultExporter().export(cursor, CsvExportFormat(), target).toList()
        }

      assertEquals(target.toAbsolutePath(), failure.path)
      assertTrue(failure.message.orEmpty().contains("disk gone"))
      assertFalse(Files.exists(target))
      assertTrue(cursor.isClosed)
    }
}
