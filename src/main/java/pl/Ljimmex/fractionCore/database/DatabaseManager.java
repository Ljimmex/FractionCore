package pl.Ljimmex.fractionCore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.database.config.DatabaseConfig;
import pl.Ljimmex.fractionCore.database.config.DatabaseType;
import pl.Ljimmex.fractionCore.database.migration.MigrationManager;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private DatabaseConfig config;
    private MigrationManager migrationManager;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfiguration(FileConfiguration fileConfiguration) {
        String typeName = fileConfiguration.getString("database.type", "SQLITE").toUpperCase();
        DatabaseType type;
        try {
            type = DatabaseType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown database type '" + typeName + "', falling back to SQLITE.");
            type = DatabaseType.SQLITE;
        }

        String host;
        int port;
        String database;
        String username;
        String password;

        if (type == DatabaseType.POSTGRESQL) {
            host = fileConfiguration.getString("database.postgresql.host", "localhost");
            port = fileConfiguration.getInt("database.postgresql.port", 5432);
            database = fileConfiguration.getString("database.postgresql.database", "fractioncore");
            username = fileConfiguration.getString("database.postgresql.username", "postgres");
            password = fileConfiguration.getString("database.postgresql.password", "");
        } else {
            host = fileConfiguration.getString("database.mysql.host", "localhost");
            port = fileConfiguration.getInt("database.mysql.port", 3306);
            database = fileConfiguration.getString("database.mysql.database", "fractioncore");
            username = fileConfiguration.getString("database.mysql.username", "root");
            password = fileConfiguration.getString("database.mysql.password", "");
        }

        this.config = new DatabaseConfig(
                type,
                fileConfiguration.getString("database.sqlite.file", "data/database.db"),
                host,
                port,
                database,
                username,
                password,
                fileConfiguration.getInt("database.pool.max-size", 10),
                fileConfiguration.getLong("database.pool.connection-timeout", 30000L),
                fileConfiguration.getLong("database.pool.idle-timeout", 600000L),
                fileConfiguration.getLong("database.pool.max-lifetime", 1800000L)
        );
    }

    public void connect() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();

        switch (config.getType()) {
            case SQLITE:
                File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                File dbFile = new File(dataFolder, config.getSqliteFile());
                dbFile.getParentFile().mkdirs();
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                hikariConfig.setDriverClassName("org.sqlite.JDBC");
                hikariConfig.setMaximumPoolSize(1);
                break;
            case MYSQL:
                String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&autoReconnect=true",
                        config.getHost(), config.getPort(), config.getDatabase());
                hikariConfig.setJdbcUrl(jdbcUrl);
                hikariConfig.setUsername(config.getUsername());
                hikariConfig.setPassword(config.getPassword());
                hikariConfig.setMaximumPoolSize(config.getPoolSize());
                break;
            case POSTGRESQL:
                String pgUrl = String.format("jdbc:postgresql://%s:%d/%s",
                        config.getHost(), config.getPort(), config.getDatabase());
                hikariConfig.setJdbcUrl(pgUrl);
                hikariConfig.setUsername(config.getUsername());
                hikariConfig.setPassword(config.getPassword());
                hikariConfig.setMaximumPoolSize(config.getPoolSize());
                break;
        }

        hikariConfig.setPoolName("FractionCorePool");
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());

        dataSource = new HikariDataSource(hikariConfig);
        plugin.getLogger().info("Connected to " + config.getType() + " database.");

        migrationManager = new MigrationManager(plugin, this);
        migrationManager.runMigrations();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed.");
        }
    }

    public Connection getConnection() throws SQLException {
        Objects.requireNonNull(dataSource, "DataSource is not initialized.");
        return dataSource.getConnection();
    }

    public DatabaseConfig getConfig() {
        return config;
    }

    public MigrationManager getMigrationManager() {
        return migrationManager;
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
