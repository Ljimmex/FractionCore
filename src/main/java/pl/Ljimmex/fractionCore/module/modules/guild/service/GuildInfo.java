package pl.Ljimmex.fractionCore.module.modules.guild.service;

import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;

import java.util.List;

/**
 * Immutable snapshot of guild data used to render the /guild info output.
 * Separating data fetching from rendering allows the DB work to happen
 * asynchronously while the final messages are sent from the main thread.
 */
public record GuildInfo(
        Guild guild,
        List<PlayerData> members,
        boolean viewerIsMember
) {
}
