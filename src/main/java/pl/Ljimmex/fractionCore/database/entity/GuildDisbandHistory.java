package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class GuildDisbandHistory {

    private long id;
    private UUID originalGuildId;
    private String name;
    private String tag;
    private String color;
    private UUID leaderUuid;
    private int points;
    private int level;
    private long createdAt;
    private long disbandedAt;
    private UUID disbandedBy;

    public GuildDisbandHistory() {
    }

    public GuildDisbandHistory(long id, UUID originalGuildId, String name, String tag, String color,
                               UUID leaderUuid, int points, int level, long createdAt,
                               long disbandedAt, UUID disbandedBy) {
        this.id = id;
        this.originalGuildId = originalGuildId;
        this.name = name;
        this.tag = tag;
        this.color = color;
        this.leaderUuid = leaderUuid;
        this.points = points;
        this.level = level;
        this.createdAt = createdAt;
        this.disbandedAt = disbandedAt;
        this.disbandedBy = disbandedBy;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getOriginalGuildId() {
        return originalGuildId;
    }

    public void setOriginalGuildId(UUID originalGuildId) {
        this.originalGuildId = originalGuildId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getDisbandedAt() {
        return disbandedAt;
    }

    public void setDisbandedAt(long disbandedAt) {
        this.disbandedAt = disbandedAt;
    }

    public UUID getDisbandedBy() {
        return disbandedBy;
    }

    public void setDisbandedBy(UUID disbandedBy) {
        this.disbandedBy = disbandedBy;
    }
}
