package pl.Ljimmex.fractionCore.database.entity;

import pl.Ljimmex.fractionCore.util.TimeUtil;

import java.util.UUID;

public class GuildInvite {

    private long id;
    private UUID guildId;
    private UUID playerUuid;
    private UUID invitedBy;
    private long invitedAt;
    private long expiresAt;

    public GuildInvite() {
    }

    public GuildInvite(long id, UUID guildId, UUID playerUuid, UUID invitedBy, long invitedAt, long expiresAt) {
        this.id = id;
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.invitedBy = invitedBy;
        this.invitedAt = invitedAt;
        this.expiresAt = expiresAt;
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

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(UUID invitedBy) {
        this.invitedBy = invitedBy;
    }

    public long getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(long invitedAt) {
        this.invitedAt = invitedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return TimeUtil.currentEpochSeconds() >= expiresAt;
    }
}
