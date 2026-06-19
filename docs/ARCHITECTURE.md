# FractionCore — Architecture Overview

This document describes the high-level architecture of FractionCore, the engine behind Fraction Guild Clans v2.0.

## Design Goals

- **Zero external dependencies** — FractionCore runs without Vault, WorldGuard, WorldEdit, or any economy plugin.
- **Modular architecture** — every feature is an independent module that can be enabled or disabled.
- **Modern Paper APIs** — built on Paper 1.20.6+ with Adventure components and MiniMessage.
- **Multi-database support** — SQLite for local deployments, MySQL/PostgreSQL for production.
- **Hot-reloadable configuration** — most config changes apply without server restart.

## Package Structure

```
pl.Ljimmex.fractionCore
├── FractionCore.java          # Main plugin class, lifecycle manager
├── command/                   # Command dispatchers and handlers
├── config/                    # ConfigManager, DebugManager
├── database/                  # DatabaseManager, DAOs, entities, migrations
├── lang/                      # LangManager, parser, placeholders
└── module/                    # Module system core
    └── modules/               # Concrete module implementations
```

## Module System

The plugin is built around a module system managed by `ModuleManager`.

### Lifecycle

1. **Registration** — modules are registered in `FractionCore#registerModules()`.
2. **Configuration load** — `ModuleManager#loadConfiguration()` reads enabled states from `config.yml`.
3. **Topological sort** — modules are ordered by declared dependencies.
4. **Enable** — modules are initialized in order.
5. **Disable** — modules are shut down in reverse order.

### Module Interface

Each module extends `BaseModule` and implements:

- `getName()` — unique module identifier.
- `getDependencies()` — list of module names required before this one loads.
- `onEnable()` — initialization logic.
- `onDisable()` — cleanup logic.
- `onReload()` — optional hot-reload logic.

### Communication

Modules communicate through:

- **Direct references** via `ModuleManager#getModule(String)`.
- **Events** (planned) — Bukkit events such as `GuildCreateEvent`, `EggDestroyEvent`.
- **Database** — shared persistence layer.

## Data Flow

```
Player Command
      │
      ▼
GuildCommand / AdminCommand
      │
      ▼
Module Logic
      │
      ▼
DAO / DatabaseManager
      │
      ▼
SQLite / MySQL / PostgreSQL
```

Messages to players go through `LangManager`, which resolves keys, applies placeholders, and parses MiniMessage/legacy formatting.

## Configuration Layers

1. **`config.yml`** — global settings: language, database, module enablement.
2. **`modules/*.yml`** — per-module settings.
3. **`lang/*.yml`** — translation files.

All layers support hot-reload via administrative commands.

## Technology Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Build Tool | Gradle (Kotlin DSL) |
| Server API | Paper 1.20.6 |
| Text | Adventure / MiniMessage |
| Database | HikariCP + SQLite/MySQL/PostgreSQL |

## Future Additions

- Brigadier-based command system.
- Event bus for cross-module communication.
- Public FGCAPI for external plugin integrations.
- PlaceholderAPI expansion.
