package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class GuildActivityLog {

    private long id;
    private UUID guildId;
    private UUID actorUuid;
    private String action;
    private String details;
    private long timestamp;

    public GuildActivityLog() {
    }

    public GuildActivityLog(long id, UUID guildId, UUID actorUuid, String action, String details, long timestamp) {
        this.id = id;
        this.guildId = guildId;
        this.actorUuid = actorUuid;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
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

    public UUID getActorUuid() {
        return actorUuid;
    }

    public void setActorUuid(UUID actorUuid) {
        this.actorUuid = actorUuid;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
