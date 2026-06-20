package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class GuildMember {

    private UUID uuid;
    private String name;
    private GuildRank rank;
    private long joinedAt;

    public GuildMember() {
    }

    public GuildMember(UUID uuid, String name, GuildRank rank, long joinedAt) {
        this.uuid = uuid;
        this.name = name;
        this.rank = rank;
        this.joinedAt = joinedAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GuildRank getRank() {
        return rank;
    }

    public void setRank(GuildRank rank) {
        this.rank = rank;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }
}
