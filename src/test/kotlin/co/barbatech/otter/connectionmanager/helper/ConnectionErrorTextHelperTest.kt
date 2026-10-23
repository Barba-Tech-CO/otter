package co.barbatech.otter.connectionmanager.helper

import co.barbatech.otter.database.error.DatabaseErrorKind
import co.barbatech.otter.database.error.DatabaseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class ConnectionErrorTextHelperTest {
  @Test
  fun describeUsesTheKindLabelAndTheReason() {
    assertEquals(
      "authentication failed: password authentication failed",
      ConnectionErrorTextHelper.describe(
        DatabaseException(DatabaseErrorKind.AuthenticationFailed, "password authentication failed"),
      ),
    )
    assertEquals(
      "host unreachable",
      ConnectionErrorTextHelper.describe(DatabaseException(DatabaseErrorKind.Unreachable, " ")),
    )
  }

  @Test
  fun timeoutAndFallbackMessagesStayReadable() {
    assertEquals("timed out after 250 ms", ConnectionErrorTextHelper.timeoutMessage(250.milliseconds))
    assertEquals("boom", ConnectionErrorTextHelper.message(IllegalStateException("boom")))
    assertEquals("IllegalStateException", ConnectionErrorTextHelper.message(IllegalStateException()))
    assertEquals(
      "unknown error",
      ConnectionErrorTextHelper.message(
        object : Exception() {
          override val message: String? = null
        },
      ),
    )
  }
}
