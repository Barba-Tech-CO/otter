import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeMultiplatform)
}

group = "co.barbatech"
version = "0.1.0"

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(compose.desktop.currentOs)
  implementation(compose.material3)
  implementation("org.jetbrains.compose.components:components-resources:${libs.versions.compose.get()}")
}

compose.desktop {
  application {
    mainClass = "co.barbatech.otter.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "otter"
      packageVersion = "1.0.0"

      macOS {
        iconFile.set(project.file("assets/icons/otter.icns"))
      }
      windows {
        iconFile.set(project.file("assets/icons/otter.ico"))
      }
      linux {
        iconFile.set(project.file("assets/icons/otter.png"))
      }
    }
  }
}
