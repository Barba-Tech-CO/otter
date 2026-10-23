import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.ktlint)
}

group = "co.barbatech"
version = "0.1.0"

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(compose.desktop.currentOs)
  implementation(libs.composeMaterial3)
  implementation(libs.composeResources)
  implementation(libs.coroutinesCore)
  implementation(libs.coroutinesSwing)
  implementation(libs.serializationJson)
  implementation(libs.postgresql)
  implementation(libs.sqliteJdbc)
  implementation(libs.rsyntaxtextarea)
  implementation(libs.javaKeyring)

  testImplementation(libs.kotlinTest)
  testImplementation(libs.junitJupiter)
  testImplementation(libs.coroutinesTest)
  testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.test {
  useJUnitPlatform()
}

ktlint {
  version.set(libs.versions.ktlint.get())
  filter {
    exclude { it.file.path.contains("/build/generated/") }
  }
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
