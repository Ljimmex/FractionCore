# FractionCore

<p align="center">
  <strong>The core engine for Fraction Guild Clans v2.0</strong><br>
  A modular, zero-dependency guild ecosystem for Paper-based Minecraft servers.
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#requirements">Requirements</a> •
  <a href="#installation">Installation</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#commands">Commands</a> •
  <a href="#building">Building</a> •
  <a href="#changelog">Changelog</a>
</p>

---

> ⚠️ **Development Preview v0.0.1**
>
> This release delivers the foundational infrastructure for Fraction Guild Clans v2.0 (Sprint 0). Core gameplay systems — guild creation, cuboid protection, guild eggs, economy, ranking, GUI, and map rendering — are scheduled for subsequent releases.

---

## Features

### Modular Architecture

FractionCore is built around a module system where every major feature is an independent, togglable component. Each module has its own configuration file and lifecycle.

| Module | Purpose | Status |
|--------|---------|--------|
| `guild` | Guild creation, ranks, membership | Infrastructure |
| `cuboid` | Custom territory protection system | Infrastructure |
| `egg` | Guild egg defense and destruction stages | Infrastructure |
| `economy` | Built-in EcoCore economy | Infrastructure |
| `ranking` | ELO-based guild and player ranking | Infrastructure |
| `gui` | Inventory menus and interfaces | Infrastructure |
| `tab` | Custom player list (TAB) | Infrastructure |
| `map` | Interactive HTML guild map | Infrastructure |
| `villagers` | Guild trading villagers and daily quests | Infrastructure |
| `join_items` | Starter items for new players | Infrastructure |
| `lang` | Multi-language file system | ✅ Ready |
| `database` | Persistence layer (SQLite/MySQL/PostgreSQL) | ✅ Ready |
| `backup` | Automatic and manual backups | Infrastructure |
| `webhook` | Discord/webhook notifications | Infrastructure |

### Multi-Database Support

- **SQLite** as the default embedded engine.
- **MySQL** and **PostgreSQL** for production deployments.
- **HikariCP** connection pooling.
- Automatic schema migrations with version tracking.
- Clean DAO/entity architecture.

### Internationalization (i18n)

- YAML-based language files in `plugins/FractionCore/lang/`.
- Support for **MiniMessage** and legacy `&` color codes.
- Fallback chain: active language → `en_US` → raw key.
- Placeholder engine with player, guild, and custom context variables.
- Hot-reload without server restart.

### Configuration Management

- Central `config.yml` with schema versioning.
- Per-module configs in `plugins/FractionCore/modules/`.
- Automatic default file generation.
- Runtime reload via `/guild admin reload`.

---

## Requirements

| Component | Minimum Version |
|-----------|-----------------|
| Server Software | Paper 1.20.6+ |
| Client Compatibility | 1.20.x – 1.21.11 |
| Java | 21 |
| Required Plugins | None |
| Optional Plugins | PlaceholderAPI |

---

## Installation

1. Download the latest release JAR from the [Releases](../../releases) page.
2. Place the JAR file into your server's `plugins/` directory.
3. Start or restart the server.
4. FractionCore will automatically generate the following structure:

```
plugins/FractionCore/
├── config.yml
├── lang/
│   ├── pl_PL.yml
│   └── en_US.yml
├── modules/
│   ├── guild.yml
│   ├── cuboid.yml
│   ├── egg.yml
│   ├── economy.yml
│   ├── ranking.yml
│   ├── gui.yml
│   ├── tab.yml
│   ├── map.yml
│   ├── villagers.yml
│   ├── join_items.yml
│   ├── backup.yml
│   └── webhook.yml
└── data/
    └── database.db
```

---

## Configuration

### Main Config (`config.yml`)

```yaml
general:
  version: 1
  language: pl_PL
  debug: false
  prefix: "<dark_gray>[<aqua>FGC<dark_gray>] <gray>"

lang:
  default: pl_PL

database:
  type: SQLITE # SQLITE, MYSQL, POSTGRESQL
  sqlite:
    file: "data/database.db"
  mysql:
    host: localhost
    port: 3306
    database: fractioncore
    username: root
    password: ""
  postgresql:
    host: localhost
    port: 5432
    database: fractioncore
    username: postgres
    password: ""
  pool:
    max-size: 10
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000

modules:
  guild:
    enabled: true
  cuboid:
    enabled: true
  # ...
```

### Module Configs

Each enabled module reads its dedicated file from `plugins/FractionCore/modules/<module>.yml`. These files are generated automatically with sensible defaults.

---

## Commands

### Player Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/guild help` | `fractioncore.command.guild` | Displays help |

### Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/guild admin module list` | `fractioncore.admin.module` | Lists all modules |
| `/guild admin module enable <module>` | `fractioncore.admin.module` | Enables a module |
| `/guild admin module disable <module>` | `fractioncore.admin.module` | Disables a module |
| `/guild admin module reload [module]` | `fractioncore.admin.module` | Reloads module(s) |
| `/guild admin lang reload` | `fractioncore.admin.lang.reload` | Reloads language files |
| `/guild admin reload` | `fractioncore.admin.reload` | Reloads all configs and modules |
| `/guild admin debug <true\|false>` | `fractioncore.admin.debug` | Toggles debug mode |

---

## Building from Source

### Prerequisites

- JDK 21
- Gradle 8.x (wrapper included)

### Build

```bash
./gradlew build
```

The compiled JAR will be available at:

```
build/libs/FractionCore-0.0.1.jar
```

### Development Server

```bash
./gradlew runServer
```

This starts a local Paper 1.20.6 server for testing.

---

## Project Structure

```
FractionCore/
├── src/main/java/pl/Ljimmex/fractionCore/
│   ├── FractionCore.java          # Main plugin class
│   ├── command/                   # Command handlers
│   ├── config/                    # ConfigManager, DebugManager
│   ├── database/                  # DatabaseManager, DAOs, entities
│   ├── lang/                      # LangManager, parser, placeholders
│   └── module/                    # Module system
├── src/main/resources/
│   ├── config.yml
│   ├── paper-plugin.yml
│   ├── lang/
│   │   ├── pl_PL.yml
│   │   └── en_US.yml
│   └── modules/
│       └── *.yml
├── docs/
│   ├── Fraction_Guild_Clans_v2.0_Dokumentacja.pdf
│   └── FGC_Roadmap_v2.pdf
├── index.html                     # Interactive roadmap portal
├── build.gradle.kts
└── README.md
```

---

## Technology Stack

- **Language:** Java 21
- **Build Tool:** Gradle (Kotlin DSL)
- **Server API:** Paper 1.20.6
- **Text Engine:** Adventure / MiniMessage
- **Database:** HikariCP with SQLite, MySQL, and PostgreSQL JDBC drivers

---

## Documentation

- Full feature specification: `docs/Fraction_Guild_Clans_v2.0_Dokumentacja.pdf`
- Development roadmap: `docs/FGC_Roadmap_v2.pdf`
- Interactive progress portal: `index.html`

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete version history.

---

## License

This project is proprietary software. Redistribution or modification without the author's permission is prohibited unless otherwise stated in a dedicated `LICENSE` file.

---

## Author

- **Ljimex** — [GitHub](https://github.com/Ljinmex)

---

<p align="center">
  Built for Fraction Guild Clans v2.0
</p>
