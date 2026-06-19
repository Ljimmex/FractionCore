# FractionCore — Database Schema

This document describes the database schema used by FractionCore. The plugin supports SQLite (default), MySQL, and PostgreSQL.

## Engines

| Engine | Recommended For | Driver |
|--------|-----------------|--------|
| SQLite | Local servers, small communities | `org.xerial:sqlite-jdbc` |
| MySQL | Production servers, networks | `com.mysql:mysql-connector-j` |
| PostgreSQL | Production servers, networks | `org.postgresql:postgresql` |

## Schema Version

Schema migrations are tracked in the `schema_version` table:

```sql
CREATE TABLE schema_version (
    version INTEGER PRIMARY KEY,
    applied_at INTEGER
);
```

## Tables

### `guilds`

Core guild data.

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(36) | Guild UUID (primary key) |
| `name` | VARCHAR(64) | Full guild name |
| `tag` | VARCHAR(16) | Short guild tag |
| `leader_uuid` | VARCHAR(36) | UUID of the guild leader |
| `points` | INTEGER | Ranking points |
| `level` | INTEGER | Guild level |
| `created_at` | INTEGER | Unix timestamp |

### `players`

Player data and guild membership.

| Column | Type | Description |
|--------|------|-------------|
| `uuid` | VARCHAR(36) | Player UUID (primary key) |
| `name` | VARCHAR(32) | Player name |
| `guild_id` | VARCHAR(36) | Guild UUID (nullable) |
| `rank` | VARCHAR(32) | Guild rank |
| `kills` | INTEGER | Kill count |
| `deaths` | INTEGER | Death count |
| `assists` | INTEGER | Assist count |
| `points` | INTEGER | Player points |
| `joined_guild_at` | INTEGER | Unix timestamp |

### `cuboids`

Guild territory definitions.

| Column | Type | Description |
|--------|------|-------------|
| `guild_id` | VARCHAR(36) | Guild UUID (primary key) |
| `world` | VARCHAR(64) | World name |
| `center_x` | INTEGER | Center X coordinate |
| `center_y` | INTEGER | Center Y coordinate |
| `center_z` | INTEGER | Center Z coordinate |
| `radius` | INTEGER | Cuboid radius |
| `level` | INTEGER | Cuboid level |

### `ranking_history`

Historical ranking events.

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `guild_id` | VARCHAR(36) | Guild UUID |
| `player_uuid` | VARCHAR(36) | Player UUID |
| `type` | VARCHAR(32) | Event type (kill, death, etc.) |
| `value` | INTEGER | Points delta |
| `created_at` | INTEGER | Unix timestamp |

### `transactions`

Economic transactions.

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `guild_id` | VARCHAR(36) | Guild UUID |
| `player_uuid` | VARCHAR(36) | Player UUID |
| `type` | VARCHAR(32) | Transaction type |
| `amount` | DOUBLE | Amount |
| `balance_after` | DOUBLE | Balance after transaction |
| `created_at` | INTEGER | Unix timestamp |

### `guild_bans`

Banned players per guild.

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `guild_id` | VARCHAR(36) | Guild UUID |
| `player_uuid` | VARCHAR(36) | Banned player UUID |
| `reason` | TEXT | Ban reason |
| `banned_by` | VARCHAR(36) | Banner UUID |
| `banned_at` | INTEGER | Unix timestamp |

### `guild_activity_log`

Guild activity log entries.

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `guild_id` | VARCHAR(36) | Guild UUID |
| `actor_uuid` | VARCHAR(36) | Actor UUID (nullable) |
| `action` | VARCHAR(64) | Action type |
| `details` | TEXT | JSON or text details |
| `timestamp` | INTEGER | Unix timestamp |

### `guild_egg_logs`

Guild egg event history.

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `guild_id` | VARCHAR(36) | Guild UUID |
| `player_uuid` | VARCHAR(36) | Player UUID (nullable) |
| `event_type` | VARCHAR(64) | Event type |
| `old_state` | INTEGER | Previous egg state |
| `new_state` | INTEGER | New egg state |
| `metadata` | TEXT | Additional data |
| `timestamp` | INTEGER | Unix timestamp |

### `seasons`

Season ranking archive / Hall of Fame.

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `number` | INTEGER | Season number (unique) |
| `start_date` | INTEGER | Unix timestamp |
| `end_date` | INTEGER | Unix timestamp (nullable) |
| `winner_guild_id` | VARCHAR(36) | Winner guild UUID |
| `winner_name` | VARCHAR(64) | Winner guild name |
| `winner_tag` | VARCHAR(16) | Winner guild tag |
| `winner_leader_uuid` | VARCHAR(36) | Winner leader UUID |
| `winner_points` | INTEGER | Winner points |
| `winner_members` | INTEGER | Winner member count |

## Migrations

Migrations are defined in `MigrationManager`. Each migration has a version number and applies schema changes idempotently.

| Version | Description |
|---------|-------------|
| 1 | Initial schema — all core tables |

## Configuration

Database engine and connection settings are configured in `config.yml` under the `database:` section.
