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

> ⚠️ **Development Preview v0.0.3**
>
> This release delivers Sprint 1 of Fraction Guild Clans v2.0. The **guild system** is fully playable: creation, ranks, member management, relations, chat tags, and disband. **Cuboid protection** (Task 2.1) is now available with configurable flags. Guild eggs, economy, ranking, GUI, and map rendering are scheduled for subsequent releases.

---

## Features

### Modular Architecture

FractionCore is built around a module system where every major feature is an independent, togglable component. Each module has its own configuration file and lifecycle.

| Module | Purpose | Status |
|--------|---------|--------|
| `guild` | Guild creation, ranks, member management, relations, chat tags | ✅ Ready |
| `cuboid` | Custom territory protection system with flags | ✅ Ready |
| `egg` | Guild egg defense and destruction stages | Infrastructure |
| `economy` | Built-in EcoCore economy | Infrastructure |
| `ranking` | ELO-based guild and player ranking | Infrastructure |
| `gui` | Inventory menus and interfaces | Infrastructure |
| `tab` | Custom player list (TAB) | Infrastructure |
| `map` | Interactive HTML guild map | Infrastructure |
| `villagers` | Guild trading villagers and daily quests | Infrastructure (disabled by default) |
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
| `/guild create <nazwa> <tag>` | `fractioncore.command.guild` | Start guild creation |
| `/guild create confirm` | `fractioncore.command.guild` | Confirm guild creation |
| `/guild invite <nick>` | `guild.user.invite` | Invite a player |
| `/guild invite cancel` | `guild.user.invite` | Cancel pending invitations |
| `/guild invite decline <tag>` | `guild.user.join` | Decline an invitation |
| `/guild join <tag>` | `guild.user.join` | Accept invitation and join guild |
| `/guild leave` | `guild.user.leave` | Leave your guild |
| `/guild kick <nick>` | `guild.user.kick` | Kick a guild member |
| `/guild promote <nick> [ranga]` | `guild.user.promote` | Promote a member |
| `/guild demote <nick> [ranga]` | `guild.user.demote` | Demote a member |
| `/guild leader <nick>` | `guild.user.leader` | Transfer leadership |
| `/guild ban <nick> [powod]` | `guild.user.ban` | Ban a player from the guild |
| `/guild unban <nick>` | `guild.user.unban` | Unban a player |
| `/guild banlist` | `guild.user.ban` | List banned players |
| `/guild info [tag]` | `fractioncore.command.guild` | Show guild info |
| `/guild sethome` | `guild.user.sethome` | Set guild home |
| `/guild home` | `guild.user.home` | Teleport to guild home |
| `/guild description <tekst>` | `guild.user.description` | Set guild description |
| `/guild flag <flaga> <true/false>` | `guild.user.flag` | Toggle guild flags |
| `/guild requests` | `guild.user.requests` | List join requests |
| `/guild joinaccept <nick>` | `guild.user.joinaccept` | Accept join request |
| `/guild joindecline <nick>` | `guild.user.joindecline` | Decline join request |
| `/guild disband` | `guild.user.disband` | Start disband confirmation |
| `/guild disband confirm` | `guild.user.disband` | Confirm guild disband |
| `/guild ally <tag>` | `guild.user.ally` | Send ally request |
| `/guild allyaccept <tag>` | `guild.user.allyaccept` | Accept ally request |
| `/guild allydecline <tag>` | `guild.user.allydecline` | Decline ally request |
| `/guild enemy <tag>` | `guild.user.enemy` | Declare enemy |
| `/guild neutral <tag>` | `guild.user.neutral` | Set neutral relation |
| `/guild relations` | `guild.user.relations` | List relations |
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

### Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `fractioncore.command.guild` | `true` | Allows use of the `/guild` command |
| `guild.user.*` | `true` | All default guild member commands (see `plugin.yml`) |
| `fractioncore.admin` | `op` | Base access to `/guild admin` commands |
| `fractioncore.admin.module` | `op` | Manage module lifecycle |
| `fractioncore.admin.lang.reload` | `op` | Reload language files |
| `fractioncore.admin.reload` | `op` | Reload configuration and modules |
| `fractioncore.admin.debug` | `op` | Toggle debug mode |

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
build/libs/FractionCore-0.0.2.jar
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
│   ├── plugin.yml
│   ├── lang/
│   │   ├── pl_PL.yml
│   │   └── en_US.yml
│   └── modules/
│       └── *.yml
├── docs/
│   ├── GUILD_SYSTEM.md
│   ├── ARCHITECTURE.md
│   ├── DEVELOPMENT.md
│   ├── DATABASE.md
│   ├── API.md
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
- Technical documentation:
  - [`docs/GUILD_SYSTEM.md`](docs/GUILD_SYSTEM.md) — full guild system documentation (commands, ranks, relations, config)
  - [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — system architecture
  - [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) — developer setup and build guide
  - [`docs/API.md`](docs/API.md) — public API skeleton
  - [`docs/DATABASE.md`](docs/DATABASE.md) — database schema reference

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
