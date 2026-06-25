package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildInfo;

/**
 * Result of an asynchronous guild info fetch.
 */
public record GuildInfoFetchResult(
        GuildInfo info,
        String errorKey,
        PlaceholderContext errorContext
) {

    public static GuildInfoFetchResult success(GuildInfo info) {
        return new GuildInfoFetchResult(info, null, null);
    }

    public static GuildInfoFetchResult error(String errorKey, PlaceholderContext errorContext) {
        return new GuildInfoFetchResult(null, errorKey, errorContext);
    }
}
