package pl.Ljimmex.fractionCore.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class PoolSettings {

    @Comment("Maximum size of the connection pool")
    private int maxSize = 10;

    @Comment("Connection timeout in milliseconds")
    private long connectionTimeout = 30000L;

    @Comment("Idle timeout in milliseconds")
    private long idleTimeout = 600000L;

    @Comment("Maximum lifetime of a connection in milliseconds")
    private long maxLifetime = 1800000L;

    public PoolSettings() {
    }

    public int getMaxSize() {
        return maxSize;
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
