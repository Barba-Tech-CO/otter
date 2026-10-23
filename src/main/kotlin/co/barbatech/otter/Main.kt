package co.barbatech.otter

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.barbatech.otter.app.App
import co.barbatech.otter.app.AppState
import co.barbatech.otter.generated.resources.Res
import co.barbatech.otter.generated.resources.icon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.resources.painterResource

fun main() =
  application {
    val state = remember { AppState(CoroutineScope(SupervisorJob() + Dispatchers.Main)) }
    Window(onCloseRequest = ::exitApplication, title = "otter", icon = painterResource(Res.drawable.icon)) {
      App(state)
    }
  }
