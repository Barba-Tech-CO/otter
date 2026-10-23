package co.barbatech.otter

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import co.barbatech.otter.generated.resources.Res
import co.barbatech.otter.generated.resources.icon

fun main() = application {
  Window(onCloseRequest = ::exitApplication, title = "otter", icon = painterResource(Res.drawable.icon)) {
    MaterialTheme {
      Text("otter")
    }
  }
}
