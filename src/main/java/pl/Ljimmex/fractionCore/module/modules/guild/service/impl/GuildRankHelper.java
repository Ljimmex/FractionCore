package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import pl.Ljimmex.fractionCore.database.entity.GuildRank;

final class GuildRankHelper {

    private final GuildContext context;

    GuildRankHelper(GuildContext context) {
        this.context = context;
    }

    int getRankWeight(GuildRank rank) {
        if (rank == null) {
            return 0;
        }
        String key = switch (rank) {
            case LEADER -> "leader";
            case CO_LEADER -> "coleader";
            case MODERATOR -> "moderator";
            case MEMBER -> "member";
            case RECRUIT -> "recruit";
        };
        return context.guildConfig.getInt("ranks." + key + ".weight", rank.ordinal() * 20);
    }

    boolean canManageRank(GuildRank actor, GuildRank target) {
        return getRankWeight(actor) > getRankWeight(target);
    }
}
