package pl.Ljimmex.fractionCore.database.entity;

public enum GuildRank {
    LEADER("Lider"),
    CO_LEADER("Co-Lider"),
    MODERATOR("Moderator"),
    MEMBER("Członek"),
    RECRUIT("Rekrut");

    private final String displayName;

    GuildRank(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
