package co.barbatech.otter

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.barbatech.otter.generated.resources.Res
import co.barbatech.otter.generated.resources.icon
import org.jetbrains.compose.resources.painterResource

fun main() =
  application {
    Window(onCloseRequest = ::exitApplication, title = "otter", icon = painterResource(Res.drawable.icon)) {
      MaterialTheme {
        Text("otter")
      }
    }
  }
