package pl.Ljimmex.fractionCore.database.entity;

import java.util.UUID;

public class GuildFlag {

    private UUID guildId;
    private String flagName;
    private String flagValue;

    public GuildFlag() {
    }

    public GuildFlag(UUID guildId, String flagName, String flagValue) {
        this.guildId = guildId;
        this.flagName = flagName;
        this.flagValue = flagValue;
    }

    public UUID getGuildId() {
        return guildId;
    }

    public void setGuildId(UUID guildId) {
        this.guildId = guildId;
    }

    public String getFlagName() {
        return flagName;
    }

    public void setFlagName(String flagName) {
        this.flagName = flagName;
    }

    public String getFlagValue() {
        return flagValue;
    }

    public void setFlagValue(String flagValue) {
        this.flagValue = flagValue;
    }
}
