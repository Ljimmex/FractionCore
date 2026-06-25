package pl.Ljimmex.fractionCore.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class DatabaseSettings {

    @Comment("Database type: SQLITE, MYSQL, POSTGRESQL")
    private String type = "SQLITE";

    private SQLiteSettings sqlite = new SQLiteSettings();

    private ConnectionSettings mysql = new ConnectionSettings("localhost", 3306, "fractioncore", "root", "");

    private ConnectionSettings postgresql = new ConnectionSettings("localhost", 5432, "fractioncore", "postgres", "");

    private PoolSettings pool = new PoolSettings();

    public String getType() {
        return type;
    }

    public SQLiteSettings getSqlite() {
        return sqlite;
    }

    public ConnectionSettings getMysql() {
        return mysql;
    }

    public ConnectionSettings getPostgresql() {
        return postgresql;
    }

    public PoolSettings getPool() {
        return pool;
    }
}
