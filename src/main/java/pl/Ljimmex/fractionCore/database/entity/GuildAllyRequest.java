package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class GuildAllyRequest {

    private long id;
    private UUID guildId;
    private UUID targetGuildId;
    private long requestedAt;

    public GuildAllyRequest() {
    }

    public GuildAllyRequest(long id, UUID guildId, UUID targetGuildId, long requestedAt) {
        this.id = id;
        this.guildId = guildId;
        this.targetGuildId = targetGuildId;
        this.requestedAt = requestedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getGuildId() {
        return guildId;
    }

    public void setGuildId(UUID guildId) {
        this.guildId = guildId;
    }

    public UUID getTargetGuildId() {
        return targetGuildId;
    }

    public void setTargetGuildId(UUID targetGuildId) {
        this.targetGuildId = targetGuildId;
    }

    public long getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(long requestedAt) {
        this.requestedAt = requestedAt;
    }
}
