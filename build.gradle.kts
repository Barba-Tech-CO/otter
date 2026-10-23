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
}

compose.desktop {
  application {
    mainClass = "co.barbatech.otter.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "otter"
      packageVersion = "1.0.0"
    }
  }
}
