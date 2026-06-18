package pl.Ljimmex.fractionCore.database.migration;

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
            sql = "INSERT INTO schema_version (version, applied_at) VALUES (" + version + ", " + System.currentTimeMillis() / 1000 + ") " +
                    "ON CONFLICT (version) DO UPDATE SET applied_at = EXCLUDED.applied_at";
        } else {
            sql = dialect.insertOrReplace("schema_version", "version, applied_at") +
                    "(" + version + ", " + System.currentTimeMillis() / 1000 + ")";
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
                    "leader_uuid VARCHAR(36) NOT NULL, " +
                    "points INTEGER DEFAULT 0, " +
                    "level INTEGER DEFAULT 1, " +
                    "created_at INTEGER NOT NULL" +
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
        return migrations;
    }
}
