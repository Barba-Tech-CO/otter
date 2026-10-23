<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/otter-horizontal-dark.png">
    <img alt="otter" src="assets/otter-horizontal.png" width="360">
  </picture>
</p>

<p align="center">
  A lightweight, open source database client inspired by DBeaver.
</p>

<p align="center">
  <img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-blue.svg">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Compose%20Desktop-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Platforms" src="https://img.shields.io/badge/platforms-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey">
</p>

## About

otter aims to cover the essential features of a database client without the weight of a full IDE: fast startup, low memory use and a small, focused interface. It runs as a native desktop app on Windows, macOS and Linux.

otter is an independent project and is not affiliated with DBeaver.

## Status

Early scaffold. The app currently opens an empty window.

## Planned features

Not implemented yet:

- Connection manager
- Schema browser (schemas, tables, columns, indexes, foreign keys)
- SQL editor with syntax highlighting
- Result grid with pagination
- Export to CSV and JSON
- Query history

## Stack

- Kotlin
- Compose Multiplatform (Compose Desktop, JVM)
- Gradle (Kotlin DSL, wrapper included)

## Requirements

- JDK 17 or newer

## Run

```sh
./gradlew run
```

## Package

Native installers are built per operating system, so each one must be built on its own OS:

```sh
./gradlew packageDmg   # macOS
./gradlew packageMsi   # Windows
./gradlew packageDeb   # Linux
```

## Project structure

```
assets/                    logo and installer icons
src/main/kotlin/           application code (co.barbatech.otter)
src/main/composeResources/ bundled resources (window icon)
build.gradle.kts           build and packaging configuration
```

## Contributing

Issues and pull requests are welcome.

- Indentation is 2 spaces (see `.editorconfig`).
- Commits follow [Conventional Commits](https://www.conventionalcommits.org/), in English.
- Run `./gradlew compileKotlin` before opening a pull request.

## License

Apache License 2.0. See [LICENSE](LICENSE).
