package pl.Ljimmex.fractionCore.database.config;

public class DatabaseConfig {

    private final DatabaseType type;
    private final String sqliteFile;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;
    private final long connectionTimeout;
    private final long idleTimeout;
    private final long maxLifetime;

    public DatabaseConfig(DatabaseType type, String sqliteFile, String host, int port, String database,
                          String username, String password, int poolSize, long connectionTimeout,
                          long idleTimeout, long maxLifetime) {
        this.type = type;
        this.sqliteFile = sqliteFile;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
    }

    public DatabaseType getType() {
        return type;
    }

    public String getSqliteFile() {
        return sqliteFile;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }
}
