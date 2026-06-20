package pl.Ljimmex.fractionCore.database.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Guild {

    private UUID id;
    private String name;
    private String tag;
    private String color;
    private UUID leaderUuid;
    private int points;
    private int level;
    private long createdAt;
    private String homeWorld;
    private double homeX;
    private double homeY;
    private double homeZ;
    private List<GuildMember> members;

    public Guild() {
        this.members = new ArrayList<>();
    }

    public Guild(UUID id, String name, String tag, String color, UUID leaderUuid, int points, int level, long createdAt) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.color = color;
        this.leaderUuid = leaderUuid;
        this.points = points;
        this.level = level;
        this.createdAt = createdAt;
        this.members = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getHomeWorld() {
        return homeWorld;
    }

    public void setHomeWorld(String homeWorld) {
        this.homeWorld = homeWorld;
    }

    public double getHomeX() {
        return homeX;
    }

    public void setHomeX(double homeX) {
        this.homeX = homeX;
    }

    public double getHomeY() {
        return homeY;
    }

    public void setHomeY(double homeY) {
        this.homeY = homeY;
    }

    public double getHomeZ() {
        return homeZ;
    }

    public void setHomeZ(double homeZ) {
        this.homeZ = homeZ;
    }

    public List<GuildMember> getMembers() {
        return members;
    }

    public void setMembers(List<GuildMember> members) {
        this.members = members != null ? members : new ArrayList<>();
    }
}
