package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildJoinRequest;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.util.TimeUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildRequestManager {

    private final GuildContext context;
    private final GuildJoinManager joinManager;

    public GuildRequestManager(GuildContext context, GuildJoinManager joinManager) {
        this.context = context;
        this.joinManager = joinManager;
    }

    public boolean sendRequestList(Player player) {
        RequestListResult result = fetchRequestList(player);
        renderRequestList(player, result);
        return result.success();
    }

    public RequestListResult fetchRequestList(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return RequestListResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return RequestListResult.error("guild.requests.error_no_permission", PlaceholderContext.of(player));
            }
            UUID guildId = data.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");
            List<GuildJoinRequest> requests = context.guildJoinRequestDao.findByGuild(guildId);
            return new RequestListResult(true, guildName, tag, requests, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to list join requests: " + e.getMessage());
            return RequestListResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public void renderRequestList(Player player, RequestListResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        try {
            String guildName = result.guildName();
            String tag = result.tag();
            List<GuildJoinRequest> requests = result.requests();
            if (requests.isEmpty()) {
                context.send(player, "guild.requests.empty", MessageType.INFO,
                        PlaceholderContext.of(player).with("guild", guildName).with("tag", tag));
                return;
            }
            context.send(player, "guild.requests.header", MessageType.INFO,
                    PlaceholderContext.of(player).with("guild", guildName).with("tag", tag).with("count", requests.size()));
            for (GuildJoinRequest request : requests) {
                Optional<PlayerData> requesterOpt = context.playerDao.findByUuid(request.getPlayerUuid());
                String requesterName = requesterOpt.map(PlayerData::getName).orElse("?");
                context.send(player, "guild.requests.entry", MessageType.INFO,
                        PlaceholderContext.of(player)
                                .with("player", requesterName)
                                .with("date", context.formatDate(request.getRequestedAt())));
            }
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to render request list: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
        }
    }

    public CompletableFuture<RequestListResult> sendRequestListAsync(Player player) {
        return context.databaseExecutor.supplyAsync(() -> fetchRequestList(player));
    }

    public boolean acceptJoinRequest(Player player, String targetName) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                context.send(player, "guild.joinaccept.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            UUID guildId = data.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            if (guildOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild guild = guildOpt.get();
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty()) {
                context.send(player, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(player).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            Optional<GuildJoinRequest> requestOpt = context.guildJoinRequestDao.find(guildId, targetData.getUuid());
            if (requestOpt.isEmpty()) {
                context.send(player, "guild.joinaccept.error_no_request", MessageType.ERROR,
                        PlaceholderContext.of(player).with("player", targetData.getName()));
                return false;
            }
            Player targetPlayer = Bukkit.getPlayer(targetData.getUuid());
            if (targetPlayer == null) {
                context.send(player, "guild.joinaccept.error_offline", MessageType.ERROR,
                        PlaceholderContext.of(player).with("player", targetData.getName()));
                return false;
            }
            GuildJoinManager.JoinCheckResult check = joinManager.canJoinGuild(targetPlayer, guildId);
            if (!check.allowed()) {
                PlaceholderContext ctx = PlaceholderContext.of(player).with("player", targetData.getName());
                if (check.key().equals("guild.join.error_cooldown")) {
                    int cooldownMinutes = context.guildConfig.getInt("member-management.cooldown-minutes-after-leave", 1440);
                    long remaining = Math.max(0, cooldownMinutes - (TimeUtil.currentEpochSeconds() - targetData.getLeftGuildAt()) / 60);
                    ctx.with("minutes", remaining);
                }
                context.send(player, check.key(), MessageType.ERROR, ctx);
                return false;
            }
            context.guildJoinRequestDao.delete(guildId, targetData.getUuid());
            GuildJoinManager.JoinResult joinResult = joinManager.executeJoinSync(targetPlayer, guild, guild.getTag());
            joinManager.applyJoinEffects(targetPlayer, joinResult);
            if (joinResult.success()) {
                context.send(player, "guild.joinaccept.success", MessageType.SUCCESS,
                        PlaceholderContext.of(player).with("player", targetData.getName()));
            }
            return joinResult.success();
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to accept join request: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean declineJoinRequest(Player player, String targetName) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                context.send(player, "guild.joindecline.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            UUID guildId = data.getGuildId();
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty()) {
                context.send(player, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(player).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            Optional<GuildJoinRequest> requestOpt = context.guildJoinRequestDao.find(guildId, targetData.getUuid());
            if (requestOpt.isEmpty()) {
                context.send(player, "guild.joindecline.error_no_request", MessageType.ERROR,
                        PlaceholderContext.of(player).with("player", targetData.getName()));
                return false;
            }
            context.guildJoinRequestDao.delete(guildId, targetData.getUuid());
            context.send(player, "guild.joindecline.success", MessageType.SUCCESS,
                    PlaceholderContext.of(player).with("player", targetData.getName()));
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to decline join request: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public record RequestListResult(boolean success, String guildName, String tag, List<GuildJoinRequest> requests,
                                     String errorKey, PlaceholderContext errorContext) {
        public static RequestListResult error(String key, PlaceholderContext context) {
            return new RequestListResult(false, "?", "?", List.of(), key, context);
        }
    }
}
