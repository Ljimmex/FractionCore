package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class GuildJoinRequest {

    private long id;
    private UUID guildId;
    private UUID playerUuid;
    private long requestedAt;

    public GuildJoinRequest() {
    }

    public GuildJoinRequest(long id, UUID guildId, UUID playerUuid, long requestedAt) {
        this.id = id;
        this.guildId = guildId;
        this.playerUuid = playerUuid;
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

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public long getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(long requestedAt) {
        this.requestedAt = requestedAt;
    }
}
