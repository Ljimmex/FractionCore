# FractionCore — Developer Guide

This document explains how to set up the development environment, build the project, and run a local test server.

## Prerequisites

- **JDK 21** — required by Paper 1.20.6+ and the Gradle toolchain.
- **Git** — for cloning the repository.
- *(Optional)* **IntelliJ IDEA** — recommended IDE.

## Clone and Build

```bash
git clone https://github.com/Ljinmex/FractionCore.git
cd FractionCore
./gradlew build
```

The compiled JAR is placed at:

```
build/libs/FractionCore-0.0.2.jar
```

## Run a Local Test Server

```bash
./gradlew runServer
```

This downloads Paper 1.20.6 and starts a local server. You can join with a Minecraft client in the same version range.

To grant yourself operator permissions in the test server console:

```
op YourNickname
```

## Project Structure

| Path | Purpose |
|------|---------|
| `src/main/java/.../fractionCore/` | Main source code |
| `src/main/resources/` | Plugin configs, language files, module templates |
| `src/main/resources/lang/` | Default translation files |
| `src/main/resources/modules/` | Default module configurations |
| `docs/` | Technical documentation |
| `index.html` | Interactive roadmap portal |

## Useful Gradle Tasks

| Task | Description |
|------|-------------|
| `./gradlew build` | Compile and package the plugin |
| `./gradlew runServer` | Start a local Paper server |
| `./gradlew clean` | Remove build artifacts |
| `./gradlew clean build` | Full clean rebuild |

## Debug Mode

Enable debug logging in-game or via console:

```
/guild admin debug true
```

This logs detailed operations to the server console. Disable with:

```
/guild admin debug false
```

## Code Style

- Use 4 spaces for indentation.
- Keep methods focused and modular.
- Prefer try-with-resources for JDBC operations.
- Log meaningful messages; avoid duplicate logging.
- Follow existing naming conventions in the codebase.

## Testing Changes

1. Build the plugin.
2. Copy `build/libs/FractionCore-0.0.2.jar` to a test server's `plugins/` directory.
3. Start the server and verify logs.
4. Test affected commands and modules.

## Adding a New Module

1. Create a class in `src/main/java/.../module/modules/MyModule.java` extending `BaseModule`.
2. Register it in `FractionCore#registerModules()`.
3. Add a default config file in `src/main/resources/modules/my_module.yml` (optional).
4. Declare dependencies via `getDependencies()` if the module relies on others.

## Contributing

1. Create a feature branch.
2. Make focused commits.
3. Update relevant documentation (`README.md`, `CHANGELOG.md`, `docs/`).
4. Open a pull request with a clear description.

## Resources

- [Paper Documentation](https://docs.papermc.io/)
- [Adventure Documentation](https://docs.advntr.dev/)
- [MiniMessage Format](https://docs.advntr.dev/minimessage/format.html)
