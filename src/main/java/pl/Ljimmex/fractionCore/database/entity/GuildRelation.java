package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class GuildRelation {

    private long id;
    private UUID guild1Id;
    private UUID guild2Id;
    private RelationType type;
    private long createdAt;

    public GuildRelation() {
    }

    public GuildRelation(long id, UUID guild1Id, UUID guild2Id, RelationType type, long createdAt) {
        this.id = id;
        this.guild1Id = guild1Id;
        this.guild2Id = guild2Id;
        this.type = type;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getGuild1Id() {
        return guild1Id;
    }

    public void setGuild1Id(UUID guild1Id) {
        this.guild1Id = guild1Id;
    }

    public UUID getGuild2Id() {
        return guild2Id;
    }

    public void setGuild2Id(UUID guild2Id) {
        this.guild2Id = guild2Id;
    }

    public RelationType getType() {
        return type;
    }

    public void setType(RelationType type) {
        this.type = type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
