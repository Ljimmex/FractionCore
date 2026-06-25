package pl.Ljimmex.fractionCore.database.migration;

import pl.Ljimmex.fractionCore.util.TimeUtil;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.config.DatabaseType;
import pl.Ljimmex.fractionCore.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MigrationManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final SqlDialect dialect;

    public MigrationManager(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.dialect = new SqlDialect(databaseManager.getConfig().getType());
    }

    public void runMigrations() throws SQLException {
        createSchemaVersionTable();
        int currentVersion = getCurrentVersion();
        plugin.getLogger().info("Current database schema version: " + currentVersion);

        List<Migration> migrations = getMigrations();
        for (Migration migration : migrations) {
            if (migration.getVersion() > currentVersion) {
                plugin.getLogger().info("Applying migration " + migration.getVersion() + ": " + migration.getDescription());
                migration.apply(databaseManager);
                setVersion(migration.getVersion());
            }
        }
    }

    private void createSchemaVersionTable() throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, applied_at INTEGER)");
        }
    }

    private int getCurrentVersion() throws SQLException {
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT MAX(version) AS version FROM schema_version")) {
            if (resultSet.next()) {
                return resultSet.getInt("version");
            }
        }
        return 0;
    }

    private void setVersion(int version) throws SQLException {
        DatabaseType type = databaseManager.getConfig().getType();
        String sql;
        if (type == DatabaseType.POSTGRESQL) {
            sql = "INSERT INTO schema_version (version, applied_at) VALUES (" + version + ", " + TimeUtil.currentEpochSeconds() + ") " +
                    "ON CONFLICT (version) DO UPDATE SET applied_at = EXCLUDED.applied_at";
        } else {
            sql = dialect.insertOrReplace("schema_version", "version, applied_at") +
                    "(" + version + ", " + TimeUtil.currentEpochSeconds() + ")";
        }
        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private List<Migration> getMigrations() {
        List<Migration> migrations = new ArrayList<>();
        migrations.add(new Migration(1, "Initial schema", connection -> {
            Statement statement = connection.createStatement();

            statement.execute("CREATE TABLE IF NOT EXISTS guilds (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(64) NOT NULL UNIQUE, " +
                    "tag VARCHAR(16) NOT NULL UNIQUE, " +
                    "color VARCHAR(32), " +
                    "leader_uuid VARCHAR(36) NOT NULL, " +
                    "points INTEGER DEFAULT 0, " +
                    "level INTEGER DEFAULT 1, " +
                    "created_at INTEGER NOT NULL, " +
                    "home_world VARCHAR(64), " +
                    "home_x DOUBLE, " +
                    "home_y DOUBLE, " +
                    "home_z DOUBLE" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(32) NOT NULL, " +
                    "guild_id VARCHAR(36), " +
                    "rank VARCHAR(32), " +
                    "kills INTEGER DEFAULT 0, " +
                    "deaths INTEGER DEFAULT 0, " +
                    "assists INTEGER DEFAULT 0, " +
                    "points INTEGER DEFAULT 1000, " +
                    "joined_guild_at INTEGER, " +
                    "left_guild_at INTEGER, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE SET NULL" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS cuboids (" +
                    "guild_id VARCHAR(36) PRIMARY KEY, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "center_x INTEGER NOT NULL, " +
                    "center_y INTEGER NOT NULL, " +
                    "center_z INTEGER NOT NULL, " +
                    "radius INTEGER NOT NULL, " +
                    "level INTEGER DEFAULT 1, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS ranking_history (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "guild_id VARCHAR(36), " +
                    "player_uuid VARCHAR(36), " +
                    "type VARCHAR(32) NOT NULL, " +
                    "value INTEGER NOT NULL, " +
                    "created_at INTEGER NOT NULL" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "guild_id VARCHAR(36), " +
                    "player_uuid VARCHAR(36), " +
                    "type VARCHAR(32) NOT NULL, " +
                    "amount DOUBLE NOT NULL, " +
                    "balance_after DOUBLE NOT NULL, " +
                    "created_at INTEGER NOT NULL" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS guild_bans (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "guild_id VARCHAR(36) NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "reason " + dialect.textType() + ", " +
                    "banned_by VARCHAR(36) NOT NULL, " +
                    "banned_at INTEGER NOT NULL, " +
                    "UNIQUE (guild_id, player_uuid), " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS guild_activity_log (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "guild_id VARCHAR(36) NOT NULL, " +
                    "actor_uuid VARCHAR(36), " +
                    "action VARCHAR(64) NOT NULL, " +
                    "details " + dialect.longTextType() + ", " +
                    "timestamp INTEGER NOT NULL, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS guild_egg_logs (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "guild_id VARCHAR(36) NOT NULL, " +
                    "player_uuid VARCHAR(36), " +
                    "event_type VARCHAR(64) NOT NULL, " +
                    "old_state INTEGER, " +
                    "new_state INTEGER, " +
                    "metadata " + dialect.textType() + ", " +
                    "timestamp INTEGER NOT NULL, " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS seasons (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "number INTEGER NOT NULL UNIQUE, " +
                    "start_date INTEGER NOT NULL, " +
                    "end_date INTEGER, " +
                    "winner_guild_id VARCHAR(36), " +
                    "winner_name VARCHAR(64), " +
                    "winner_tag VARCHAR(16), " +
                    "winner_leader_uuid VARCHAR(36), " +
                    "winner_points INTEGER, " +
                    "winner_members INTEGER" +
                    ")");

            statement.close();
        }));
        migrations.add(new Migration(2, "Add guild color column", connection -> {
            Statement statement = connection.createStatement();
            DatabaseType type = databaseManager.getConfig().getType();
            String sql = type == DatabaseType.SQLITE
                    ? "ALTER TABLE guilds ADD COLUMN color VARCHAR(32)"
                    : "ALTER TABLE guilds ADD COLUMN IF NOT EXISTS color VARCHAR(32)";
            try {
                statement.execute(sql);
            } catch (SQLException e) {
                // SQLite doesn't support IF NOT EXISTS; ignore if column already exists
                if (type == DatabaseType.SQLITE && e.getMessage() != null && e.getMessage().contains("duplicate column name")) {
                    return;
                }
                throw e;
            } finally {
                statement.close();
            }
        }));
        migrations.add(new Migration(3, "Add guild home and player leave timestamp columns", connection -> {
            Statement statement = connection.createStatement();
            DatabaseType type = databaseManager.getConfig().getType();
            try {
                if (type == DatabaseType.SQLITE) {
                    statement.execute("ALTER TABLE guilds ADD COLUMN home_world VARCHAR(64)");
                    statement.execute("ALTER TABLE guilds ADD COLUMN home_x DOUBLE");
                    statement.execute("ALTER TABLE guilds ADD COLUMN home_y DOUBLE");
                    statement.execute("ALTER TABLE guilds ADD COLUMN home_z DOUBLE");
                    statement.execute("ALTER TABLE players ADD COLUMN left_guild_at INTEGER");
                } else {
                    statement.execute("ALTER TABLE guilds ADD COLUMN IF NOT EXISTS home_world VARCHAR(64)");
                    statement.execute("ALTER TABLE guilds ADD COLUMN IF NOT EXISTS home_x DOUBLE");
                    statement.execute("ALTER TABLE guilds ADD COLUMN IF NOT EXISTS home_y DOUBLE");
                    statement.execute("ALTER TABLE guilds ADD COLUMN IF NOT EXISTS home_z DOUBLE");
                    statement.execute("ALTER TABLE players ADD COLUMN IF NOT EXISTS left_guild_at INTEGER");
                }
            } catch (SQLException e) {
                if (type == DatabaseType.SQLITE && e.getMessage() != null && e.getMessage().contains("duplicate column name")) {
                    return;
                }
                throw e;
            } finally {
                statement.close();
            }
        }));
        migrations.add(new Migration(4, "Add guild invites table", connection -> {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS guild_invites (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "guild_id VARCHAR(36) NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "invited_by VARCHAR(36) NOT NULL, " +
                    "invited_at INTEGER NOT NULL, " +
                    "expires_at INTEGER NOT NULL, " +
                    "UNIQUE (guild_id, player_uuid), " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");
            statement.close();
        }));
        migrations.add(new Migration(5, "Add guild settings columns", connection -> {
            Statement statement = connection.createStatement();
            DatabaseType type = databaseManager.getConfig().getType();
            try {
                if (type == DatabaseType.SQLITE) {
                    statement.execute("ALTER TABLE guilds ADD COLUMN description " + dialect.textType());
                    statement.execute("ALTER TABLE guilds ADD COLUMN is_public INTEGER DEFAULT 1");
                    statement.execute("ALTER TABLE guilds ADD COLUMN allow_join_requests INTEGER DEFAULT 0");
                    statement.execute("ALTER TABLE guilds ADD COLUMN show_home INTEGER DEFAULT 0");
                } else {
                    statement.execute("ALTER TABLE guilds ADD COLUMN IF NOT EXISTS description " + dialect.textType());
                    statement.execute("ALTER TABLE guilds ADD COLUMN IF NOT EXISTS is_public INTEGER DEFAULT 1");
                    statement.execute("ALTER TABLE guilds ADD COLUMN IF NOT EXISTS allow_join_requests INTEGER DEFAULT 0");
                    statement.execute("ALTER TABLE guilds ADD COLUMN IF NOT EXISTS show_home INTEGER DEFAULT 0");
                }
            } catch (SQLException e) {
                if (type == DatabaseType.SQLITE && e.getMessage() != null && e.getMessage().contains("duplicate column name")) {
                    return;
                }
                throw e;
            } finally {
                statement.close();
            }
        }));
        migrations.add(new Migration(6, "Add guild join requests table", connection -> {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS guild_join_requests (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "guild_id VARCHAR(36) NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "requested_at INTEGER NOT NULL, " +
                    "UNIQUE (guild_id, player_uuid), " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");
            statement.close();
        }));
        migrations.add(new Migration(7, "Add guild disband history table", connection -> {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS guild_disband_history (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "original_guild_id VARCHAR(36) NOT NULL, " +
                    "name VARCHAR(64) NOT NULL, " +
                    "tag VARCHAR(16) NOT NULL, " +
                    "color VARCHAR(32), " +
                    "leader_uuid VARCHAR(36) NOT NULL, " +
                    "points INTEGER DEFAULT 0, " +
                    "level INTEGER DEFAULT 1, " +
                    "created_at INTEGER NOT NULL, " +
                    "disbanded_at INTEGER NOT NULL, " +
                    "disbanded_by VARCHAR(36) NOT NULL" +
                    ")");
            statement.close();
        }));
        migrations.add(new Migration(8, "Add guild relations and ally request tables", connection -> {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS guild_relations (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "guild1_id VARCHAR(36) NOT NULL, " +
                    "guild2_id VARCHAR(36) NOT NULL, " +
                    "type VARCHAR(16) NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "UNIQUE (guild1_id, guild2_id), " +
                    "FOREIGN KEY (guild1_id) REFERENCES guilds(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (guild2_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS guild_ally_requests (" +
                    "id " + dialect.autoIncrementPrimaryKey() + ", " +
                    "guild_id VARCHAR(36) NOT NULL, " +
                    "target_guild_id VARCHAR(36) NOT NULL, " +
                    "requested_at INTEGER NOT NULL, " +
                    "UNIQUE (guild_id, target_guild_id), " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (target_guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");
            statement.close();
        }));
        migrations.add(new Migration(9, "Add guild cuboid flags table", connection -> {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS guild_flags (" +
                    "guild_id VARCHAR(36) NOT NULL, " +
                    "flag_name VARCHAR(32) NOT NULL, " +
                    "flag_value VARCHAR(16) NOT NULL, " +
                    "PRIMARY KEY (guild_id, flag_name), " +
                    "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ")");
            statement.close();
        }));
        return migrations;
    }
}
