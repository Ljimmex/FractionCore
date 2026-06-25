# Changelog

All notable changes to **FractionCore** are documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/) and follows the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.

---

## [0.0.3] - 2026-06-21

### Added

#### Task 2.1 — Cuboid Architecture
- Added `CuboidFlagType`, `CuboidFlagValue`, and `CuboidAction` enums under `pl.Ljimmex.fractionCore.cuboid.model`.
- Added `GuildFlag` entity plus `GuildFlagDao` implementation.
- Added database migration v9 creating the `guild_flags` table.
- Added `CuboidManager` loading all cuboids into memory and resolving cuboids by location or guild.
- Implemented 6 cuboid flags (`BUILD`, `DESTROY`, `USE`, `INTERACT`, `TNT`, `FRIENDLY_FIRE`) with 5 values each (`ALLOW`, `DENY`, `MEMBERS`, `ALLIES`, `LEADER`).
- Added `CuboidProtectionListener` handling `BlockPlace`, `BlockBreak`, `PlayerInteract`, TNT explosions, and PvP.
- Added configurable TNT build cooldown (`cuboid.tnt-build-cooldown-seconds`, default 60s): after TNT explodes inside a cuboid, building and destroying are blocked for guild members.
- Removed the `BUCKET` and `ENTRY` cuboid flags.
- Added `/guild cuboidflag` command.
- Added language keys for cuboid protection and flags in `pl_PL.yml` and `en_US.yml`.
- Rebuilt `/guild help` with `main`, `guild`, and `admin` categories.
- Added missing foundation config keys to `modules/guild.yml`: `starting-radius`, `min-distance-between-guilds`, `egg-center-y`, `egg-block`, `min-distance-from-spawn`.

### Removed
- Removed the trusted-player system (`GuildTrustedPlayer`, `GuildTrustedDao`, `/guild trust`, `/guild untrust`, `/guild trusted`) in favor of explicit guild membership and ally relations.

## [0.0.2] - 2026-06-18

### Added

#### Task 1.1 — Guild Entities and DAOs
- `GuildRank` enum: `LEADER`, `CO_LEADER`, `MODERATOR`, `MEMBER`, `RECRUIT`.
- `GuildMember` entity: `uuid`, `name`, `rank`, `joinedAt`.
- Extended `Guild` entity with `color` and `members` list.
- `GuildDao` CRUD implementation with member loading from the `players` table.
- `PlayerDao` mapping for `GuildRank` and player stats (kills, deaths, assists, points, guild membership).
- Database migration v2 adding the `color` column to `guilds`.

#### Task 1.2 — Guild Creation
- `/guild create <name> <tag> [color]` command with preview and `/guild create confirm` step.
- Configurable validation for guild name (3–24 chars) and tag (2–6 alphanumeric).
- Uniqueness checks for name and tag via `GuildDao`.
- Foundation item cost validation, preview and deduction from player inventory.
- Foundation requirements: leave cooldown, max guilds per player, blocked worlds.
- Location validation: blocked worlds, minimum distance from spawn, minimum distance from other guilds.
- Automatic creation of the guild record, founder as `LEADER`, and guild home location.
- Automatic generation of the starting cuboid via `CuboidDao`.
- Teleportation of the founder to the guild center.
- 6x6x6 empty cube generation around the guild egg at Y=20 with the egg in the center.
- Optional global broadcast announcement on guild creation.
- `GuildService` orchestrating the entire creation flow.
- `GuildTagManager` adding the guild tag before the player name in chat and above the head.
- `GuildJoinListener` restoring the guild tag on player join.
- Database migration v3 adding guild home columns and `players.left_guild_at`.

#### Task 1.3 — Guild Member Management
- Database migration v4 adding the `guild_invites` table (guild, player, invited_by, timestamps, expiry).
- `GuildInvite` entity and `GuildInviteDao`/`GuildInviteDaoImpl` for persistent invitation storage.
- `/guild invite <nick>` command with rank check (Moderator+), member limit, invite limit, and ban checks.
- `/guild invite cancel` command to cancel all pending invitations from the sender's guild.
- `/guild invite decline <tag>` command for invited players to decline an invitation.
- `/guild join <tag>` command to accept an invitation, including cooldown, ban, and guild-membership validation.
- Optional configurable item cost for joining a guild (`member-management.join-cost` in `modules/guild.yml`).
- `/guild leave` command with leader-leave protection.
- `/guild kick <nick>` command with rank hierarchy validation.
- `/guild promote <nick>` and `/guild demote <nick>` commands respecting rank weights.
- `/guild leader <nick>` command for transferring guild leadership.
- `/guild ban <nick> [reason]`, `/guild unban <nick>` and `/guild banlist` commands using the existing `guild_bans` table.
- `/guild info [tag]` command displaying guild name, tag, leader, points, level, member count, and creation date.
- Guild-wide broadcast messages for joins, leaves, kicks, promotions, demotions, leadership transfers, bans, and unbans.
- `GuildChatListener` adding the guild tag and rank to chat messages via configurable `chat.format` in `modules/guild.yml`.
- Promote/demote success messages now show both the old and the new rank.
- Scoreboard tag updates/clears for affected players.
- Join cost moved from separate `join_items` module into `modules/guild.yml` (`member-management.join-cost`); `JoinItemsModule` and `modules/join_items.yml` removed.
- `ConfigManager` now merges missing module config keys from JAR defaults, so new options (e.g. `member-management`) appear automatically in existing server configs.
- Cooldown after leaving a guild before joining a new one is fully configurable via `member-management.cooldown-minutes-after-leave` (default 1440 minutes = 24h).
- New permission nodes: `guild.user.invite|join|leave|kick|promote|demote|leader|ban|unban` (default `true`).
- New language keys in `pl_PL.yml` and `en_US.yml` for all member-management messages.

### Fixed
- Guild chat no longer displays raw closing MiniMessage tags such as `</aqua>`.
- Rank letters (`L`, `Z`, `M`, `C`, `R`) in guild chat are now automatically wrapped in brackets, e.g. `[MBA][C] Maniek: message`.
- `ConfigManager` guild-chat migration now detects and repairs formats containing broken closing tags.
- Scoreboard / nameplate guild tags now update their relation color correctly instead of staying stale.
- `GuildTagManager` no longer destroys and recreates all scoreboard teams on every update, preventing clients from missing color changes.
- `GuildTagManager` now supports hex relation colors for nameplates by approximating to the nearest vanilla color.
- Mixed legacy (`&`/`§`) and MiniMessage color codes in scoreboard prefixes are now parsed correctly.
- Default neutral relation color changed from `<white>` to `<gray>` so other guild tags are no longer plain white.
- Fixed ally command messages not replacing the `{target_tag}` placeholder (was using `{tag}` instead).
- Fixed `/guild disband` showing `0` seconds when `disband.timeout-seconds` is misconfigured; it now falls back to 60 seconds.
- Added `confirm` tab-completion for `/guild disband confirm` and listed it in `/guild help`.
- Added an action-bar countdown while waiting for `/guild disband confirm`.
- `/guild disband` no longer sends a chat confirmation message; only the action-bar countdown is shown.
- Action-bar countdown for `/guild disband` no longer includes the `[Gildia]` prefix.
- Fixed the player disbanding the guild receiving both the success and broadcast messages; broadcast now goes to everyone else.
- Fixed guild chat closing bracket color — the `]` after the guild tag now uses the format color instead of resetting to white.
- Added complete Guild System documentation in `docs/GUILD_SYSTEM.md` (Task 1.6).
- Fixed guild egg not being removed when the guild is disbanded.
- Updated `README.md` to reflect the current v0.0.2 feature set, guild commands, permissions, and documentation links.

---

## [0.0.1] - 2026-06-18

### Overview

Initial infrastructure release for the Fraction Guild Clans v2.0 ecosystem. This version establishes the project foundation, modular architecture, database layer, language system, and configuration management. No gameplay mechanics are active yet.

### Added

#### Task 0.1 — Project Setup
- Gradle project with Kotlin DSL build scripts.
- Java 21 toolchain and Paper API 1.20.6 dependency.
- Server compatibility range: **1.20.x – 1.21.11**.
- `plugin.yml` metadata, command registration, and permission definitions.
- Local development server via `run-paper` plugin.
- Self-contained fat-jar packaging (HikariCP, SQLite, MySQL, PostgreSQL drivers).

#### Task 0.2 — Module System
- `ModuleManager` with topological dependency resolution.
- 14 registered modules: `guild`, `cuboid`, `egg`, `economy`, `ranking`, `gui`, `tab`, `map`, `villagers`, `join_items`, `lang`, `database`, `backup`, `webhook`.
- Administrative command: `/guild admin module <list|enable|disable|reload> [module]`.
- Dedicated `GuildAdminCommand` handler for all `/guild admin ...` subcommands.
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
- Admin subcommands (`module`, `lang`, `reload`, `debug`) moved from `GuildCommand` to `GuildAdminCommand`.

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
