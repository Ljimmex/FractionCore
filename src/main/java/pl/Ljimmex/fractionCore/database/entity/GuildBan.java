package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class GuildBan {

    private long id;
    private UUID guildId;
    private UUID playerUuid;
    private String reason;
    private UUID bannedBy;
    private long bannedAt;

    public GuildBan() {
    }

    public GuildBan(long id, UUID guildId, UUID playerUuid, String reason, UUID bannedBy, long bannedAt) {
        this.id = id;
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.reason = reason;
        this.bannedBy = bannedBy;
        this.bannedAt = bannedAt;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getBannedBy() {
        return bannedBy;
    }

    public void setBannedBy(UUID bannedBy) {
        this.bannedBy = bannedBy;
    }

    public long getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(long bannedAt) {
        this.bannedAt = bannedAt;
    }
}
