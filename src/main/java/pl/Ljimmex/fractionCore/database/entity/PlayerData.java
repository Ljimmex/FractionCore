package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class PlayerData {

    private UUID uuid;
    private String name;
    private UUID guildId;
    private String rank;
    private int kills;
    private int deaths;
    private int assists;
    private int points;
    private long joinedGuildAt;

    public PlayerData() {
    }

    public PlayerData(UUID uuid, String name, UUID guildId, String rank, int kills, int deaths, int assists, int points, long joinedGuildAt) {
        this.uuid = uuid;
        this.name = name;
        this.guildId = guildId;
        this.rank = rank;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.points = points;
        this.joinedGuildAt = joinedGuildAt;
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

    public UUID getGuildId() {
        return guildId;
    }

    public void setGuildId(UUID guildId) {
        this.guildId = guildId;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public long getJoinedGuildAt() {
        return joinedGuildAt;
    }

    public void setJoinedGuildAt(long joinedGuildAt) {
        this.joinedGuildAt = joinedGuildAt;
    }
}
