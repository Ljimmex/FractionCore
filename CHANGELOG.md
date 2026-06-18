# Changelog

All notable changes to **FractionCore** are documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/) and follows the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.

---

## [0.0.1] - 2026-06-18

### Overview

Initial infrastructure release for the Fraction Guild Clans v2.0 ecosystem. This version establishes the project foundation, modular architecture, database layer, language system, and configuration management. No gameplay mechanics are active yet.

### Added

#### Task 0.1 — Project Setup
- Gradle project with Kotlin DSL build scripts.
- Java 21 toolchain and Paper API 1.20.6 dependency.
- Server compatibility range: **1.20.x – 1.21.11**.
- `paper-plugin.yml` metadata, command registration, and permission definitions.
- Local development server via `run-paper` plugin.
- Self-contained fat-jar packaging (HikariCP, SQLite, MySQL, PostgreSQL drivers).

#### Task 0.2 — Module System
- `ModuleManager` with topological dependency resolution.
- 14 registered modules: `guild`, `cuboid`, `egg`, `economy`, `ranking`, `gui`, `tab`, `map`, `villagers`, `join_items`, `lang`, `database`, `backup`, `webhook`.
- Administrative command: `/guild admin module <list|enable|disable|reload> [module]`.
- Runtime module enable/disable through `config.yml` without server restart.

#### Task 0.3 — Database Layer
- `DatabaseManager` supporting **SQLite** (default), **MySQL**, and **PostgreSQL**.
- HikariCP connection pooling.
- Schema migration system with `schema_version` tracking.
- Entities and DAOs for `Guild`, `PlayerData`, `CuboidData`, `GuildBan`, `GuildActivityLog`, `GuildEggLog`, and `Season`.
- Database tables: `guilds`, `players`, `cuboids`, `ranking_history`, `transactions`, `guild_bans`, `guild_activity_log`, `guild_egg_logs`, `seasons`.
- `SqlDialect` abstraction for cross-database SQL compatibility.

#### Task 0.4 — Language System
- `LangManager` loading YAML translation files from `plugins/FractionCore/lang/`.
- Language fallback chain: active language → `en_US` → raw key.
- Support for **MiniMessage** (`<color>`, `<gradient>`, `<click>`, `<hover>`) and legacy color codes (`&a`, `&l`).
- Automatic format detection per message.
- Placeholder engine: `{player}`, `{guild}`, `{tag}`, `{points}`, `{level}`, `{members}`, and custom context placeholders.
- Categorized message prefixes: `info`, `error`, `success`, `warning`.
- Language hot-reload via `/guild admin lang reload`.
- Default language packs: `pl_PL.yml` and `en_US.yml`.

#### Task 0.5 — Configuration Management
- `ConfigManager` for centralized `config.yml` and per-module configuration in `modules/`.
- Automatic generation of default configuration files on first startup.
- Configuration migration framework based on `general.version`.
- `DebugManager` with runtime debug toggle.
- Administrative commands: `/guild admin reload` and `/guild admin debug <true|false>`.
- Module configuration templates in `src/main/resources/modules/`.

### Changed
- Adjusted server compatibility from `1.8–1.21.11` to `1.20.x–1.21.11`.
- Replaced Shadow plugin fat-jar configuration with direct `tasks.jar` packaging to resolve ASM processing errors.

### Removed
- Redundant `/guild lang <code>` and `/guild setlang <key> <value>` commands.
- Unused database artifacts: `guild_custom_lang` table, `guild_notifications` table, and `language` column on `players`.
- Empty module configs `modules/lang.yml` and `modules/database.yml`.

### Security & Quality
- All JDBC resources handled with try-with-resources.
- Permission checks on every administrative command.
- Graceful fallbacks to prevent empty messages and communication failures.

---

## Roadmap

Upcoming development phases are outlined in `docs/FGC_Roadmap_v2.pdf` and tracked in `index.html`.

[0.0.1]: https://github.com/Ljinmex/FractionCore/releases/tag/v0.0.1
