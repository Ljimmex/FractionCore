package pl.Ljimmex.fractionCore.module.modules.guild.service;

public enum GuildCreateResult {
    SUCCESS,
    INVALID_NAME,
    NAME_TOO_SHORT,
    NAME_TOO_LONG,
    INVALID_TAG,
    TAG_TOO_SHORT,
    TAG_TOO_LONG,
    NAME_TAKEN,
    TAG_TAKEN,
    INSUFFICIENT_ITEMS,
    ALREADY_IN_GUILD,
    COOLDOWN,
    BLOCKED_WORLD,
    TOO_CLOSE_TO_SPAWN,
    TOO_CLOSE_TO_OTHER_GUILD,
    INVALID_LOCATION,
    NO_PENDING_REQUEST,
    ERROR
}
