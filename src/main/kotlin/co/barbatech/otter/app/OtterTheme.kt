package co.barbatech.otter.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Material 3 theme that follows the system light or dark setting. */
@Composable
fun OtterTheme(content: @Composable () -> Unit) {
  val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
  MaterialTheme(colorScheme = colors, content = content)
}
