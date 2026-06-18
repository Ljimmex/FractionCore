package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class GuildEggLog {

    private long id;
    private UUID guildId;
    private UUID playerUuid;
    private String eventType;
    private Integer oldState;
    private Integer newState;
    private String metadata;
    private long timestamp;

    public GuildEggLog() {
    }

    public GuildEggLog(long id, UUID guildId, UUID playerUuid, String eventType, Integer oldState, Integer newState, String metadata, long timestamp) {
        this.id = id;
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.eventType = eventType;
        this.oldState = oldState;
        this.newState = newState;
        this.metadata = metadata;
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

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getOldState() {
        return oldState;
    }

    public void setOldState(Integer oldState) {
        this.oldState = oldState;
    }

    public Integer getNewState() {
        return newState;
    }

    public void setNewState(Integer newState) {
        this.newState = newState;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
