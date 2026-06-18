package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class Season {

    private long id;
    private int number;
    private long startDate;
    private Long endDate;
    private UUID winnerGuildId;
    private String winnerName;
    private String winnerTag;
    private UUID winnerLeaderUuid;
    private Integer winnerPoints;
    private Integer winnerMembers;

    public Season() {
    }

    public Season(long id, int number, long startDate, Long endDate, UUID winnerGuildId, String winnerName,
                  String winnerTag, UUID winnerLeaderUuid, Integer winnerPoints, Integer winnerMembers) {
        this.id = id;
        this.number = number;
        this.startDate = startDate;
        this.endDate = endDate;
        this.winnerGuildId = winnerGuildId;
        this.winnerName = winnerName;
        this.winnerTag = winnerTag;
        this.winnerLeaderUuid = winnerLeaderUuid;
        this.winnerPoints = winnerPoints;
        this.winnerMembers = winnerMembers;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public UUID getWinnerGuildId() {
        return winnerGuildId;
    }

    public void setWinnerGuildId(UUID winnerGuildId) {
        this.winnerGuildId = winnerGuildId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public String getWinnerTag() {
        return winnerTag;
    }

    public void setWinnerTag(String winnerTag) {
        this.winnerTag = winnerTag;
    }

    public UUID getWinnerLeaderUuid() {
        return winnerLeaderUuid;
    }

    public void setWinnerLeaderUuid(UUID winnerLeaderUuid) {
        this.winnerLeaderUuid = winnerLeaderUuid;
    }

    public Integer getWinnerPoints() {
        return winnerPoints;
    }

    public void setWinnerPoints(Integer winnerPoints) {
        this.winnerPoints = winnerPoints;
    }

    public Integer getWinnerMembers() {
        return winnerMembers;
    }

    public void setWinnerMembers(Integer winnerMembers) {
        this.winnerMembers = winnerMembers;
    }
}
