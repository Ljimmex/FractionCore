package pl.Ljimmex.fractionCore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.database.config.DatabaseConfig;
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

    public void loadConfiguration(DatabaseConfig config) {
        this.config = config;
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
