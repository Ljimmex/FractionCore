package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildInvite;
import pl.Ljimmex.fractionCore.database.entity.GuildJoinRequest;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.util.TimeUtil;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildJoinManager {

    private final GuildContext context;

    public GuildJoinManager(GuildContext context) {
        this.context = context;
    }

    public boolean acceptInvite(Player player, String tag) {
        AcceptInviteResult result = prepareAcceptInvite(player, tag);
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return false;
        }
        if (result.requestSent()) {
            applyJoinRequestEffects(player, result.guild(), result.tagUpper());
            return true;
        }
        JoinResult joinResult = executeJoinSync(player, result.guild(), result.tagUpper());
        applyJoinEffects(player, joinResult);
        return joinResult.success();
    }

    public AcceptInviteResult prepareAcceptInvite(Player player, String tag) {
        try {
            String tagUpper = tag.trim().toUpperCase();
            Optional<Guild> guildOpt = context.guildDao.findByTag(tagUpper);
            if (guildOpt.isEmpty()) {
                return AcceptInviteResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild guild = guildOpt.get();
            Optional<GuildInvite> inviteOpt = context.guildInviteDao.find(guild.getId(), player.getUniqueId());
            boolean hasValidInvite = inviteOpt.isPresent() && !inviteOpt.get().isExpired();
            if (!hasValidInvite) {
                if (inviteOpt.isPresent() && inviteOpt.get().isExpired()) {
                    context.guildInviteDao.delete(guild.getId(), player.getUniqueId());
                }
                if (guild.isAllowJoinRequests()) {
                    JoinCheckResult check = canJoinGuild(player, guild.getId());
                    if (!check.allowed()) {
                        PlaceholderContext ctx = PlaceholderContext.of(player).with("tag", tagUpper);
                        if (check.key().equals("guild.join.error_cooldown")) {
                            int cooldownMinutes = context.guildConfig.getInt("member-management.cooldown-minutes-after-leave", 1440);
                            PlayerData data = context.getOrCreatePlayerData(player);
                            long remaining = Math.max(0, cooldownMinutes - (TimeUtil.currentEpochSeconds() - data.getLeftGuildAt()) / 60);
                            ctx.with("minutes", remaining);
                        }
                        return AcceptInviteResult.error(check.key(), ctx);
                    }
                    if (context.guildJoinRequestDao.exists(guild.getId(), player.getUniqueId())) {
                        return AcceptInviteResult.error("guild.join.error_request_exists",
                                PlaceholderContext.of(player).with("tag", tagUpper));
                    }
                    long now = TimeUtil.currentEpochSeconds();
                    context.guildJoinRequestDao.save(new GuildJoinRequest(0, guild.getId(), player.getUniqueId(), now));
                    return new AcceptInviteResult(true, true, false, guild, tagUpper, null, PlaceholderContext.empty());
                }
                return AcceptInviteResult.error("guild.join.error_no_invite",
                        PlaceholderContext.of(player).with("tag", tagUpper));
            }
            JoinCheckResult check = canJoinGuild(player, guild.getId());
            if (!check.allowed()) {
                PlaceholderContext ctx = PlaceholderContext.of(player).with("tag", tagUpper);
                if (check.key().equals("guild.join.error_cooldown")) {
                    int cooldownMinutes = context.guildConfig.getInt("member-management.cooldown-minutes-after-leave", 1440);
                    PlayerData data = context.getOrCreatePlayerData(player);
                    long remaining = Math.max(0, cooldownMinutes - (TimeUtil.currentEpochSeconds() - data.getLeftGuildAt()) / 60);
                    ctx.with("minutes", remaining);
                }
                return AcceptInviteResult.error(check.key(), ctx);
            }
            return new AcceptInviteResult(true, false, true, guild, tagUpper, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to accept invite: " + e.getMessage());
            return AcceptInviteResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applyJoinRequestEffects(Player player, Guild guild, String tagUpper) {
        context.send(player, "guild.join.request_sent", MessageType.INFO,
                PlaceholderContext.of(player, guild).with("guild", guild.getName()).with("tag", tagUpper));
        notifyLeadersOfJoinRequest(guild, player);
    }

    private void notifyLeadersOfJoinRequest(Guild guild, Player requester) {
        Component message = context.langManager.getMessage("guild.join.request_received", MessageType.INFO,
                PlaceholderContext.empty()
                        .with("player", requester.getName())
                        .with("guild", guild.getName())
                        .with("tag", guild.getTag()));
        for (Player online : Bukkit.getOnlinePlayers()) {
            try {
                Optional<PlayerData> dataOpt = context.playerDao.findByUuid(online.getUniqueId());
                if (dataOpt.isEmpty() || !guild.getId().equals(dataOpt.get().getGuildId())) {
                    continue;
                }
                if (context.isLeaderOrCoLeader(dataOpt.get().getRank())) {
                    online.sendMessage(message);
                }
            } catch (SQLException e) {
                context.plugin.getLogger().severe("Failed to notify leader of join request: " + e.getMessage());
            }
        }
    }

    public CompletableFuture<AcceptInviteResult> acceptInviteAsync(Player player, String tag) {
        return context.databaseExecutor.supplyAsync(() -> prepareAcceptInvite(player, tag));
    }

    public JoinResult executeJoinSync(Player player, Guild guild, String tagUpper) {
        if (!checkAndDeductJoinCost(player, tagUpper)) {
            return JoinResult.error("guild.join.error_insufficient_items",
                    joinCostErrorContext(player, tagUpper));
        }
        try {
            return prepareExecuteJoin(player, guild, tagUpper);
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to execute join: " + e.getMessage());
            return JoinResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public boolean checkAndDeductJoinCost(Player player, String tagUpper) {
        java.util.List<CostItem> joinCost = context.loadJoinCostItems();
        if (!context.isJoinCostEnabled() || joinCost.isEmpty()) {
            return true;
        }
        if (!context.hasItems(player, joinCost)) {
            return false;
        }
        return context.deductItems(player, joinCost);
    }

    public PlaceholderContext joinCostErrorContext(Player player, String tagUpper) {
        return PlaceholderContext.of(player)
                .with("items", context.formatCostItems(context.loadJoinCostItems()))
                .with("tag", tagUpper);
    }

    public JoinResult prepareExecuteJoin(Player player, Guild guild, String tagUpper) throws SQLException {
        context.guildInviteDao.delete(guild.getId(), player.getUniqueId());
        context.guildJoinRequestDao.delete(guild.getId(), player.getUniqueId());
        PlayerData data = context.getOrCreatePlayerData(player);
        data.setGuildId(guild.getId());
        data.setRank(GuildRank.RECRUIT);
        long now = TimeUtil.currentEpochSeconds();
        data.setJoinedGuildAt(now);
        data.setLeftGuildAt(0);
        context.playerDao.update(data);
        return new JoinResult(true, player.getUniqueId(), player.getName(), guild, tagUpper, null, PlaceholderContext.empty());
    }

    public void applyJoinEffects(Player player, JoinResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild guild = result.guild();
        String tagUpper = result.tagUpper();
        context.getPlayerGuildCache().refresh(result.playerUuid(),
                new PlayerData(result.playerUuid(), result.playerName(), guild.getId(), GuildRank.RECRUIT, 0, 0, 0, 0, 0, 0));

        context.tagManager.updatePlayerTag(player);
        context.tagManager.updateTagsForGuild(guild.getId());

        PlaceholderContext ctx = PlaceholderContext.of(player, guild)
                .with("guild", guild.getName())
                .with("tag", tagUpper);
        context.send(player, "guild.join.success", MessageType.SUCCESS, ctx);
        context.send(player, "guild.join.welcome", MessageType.INFO, ctx);

        Component broadcast = context.langManager.getMessage("guild.join.broadcast", MessageType.INFO,
                PlaceholderContext.empty().with("player", player.getName()).with("guild", guild.getName()).with("tag", tagUpper));
        context.broadcastToGuild(guild.getId(), broadcast, player);
    }

    public CompletableFuture<JoinResult> executeJoinAsync(Player player, Guild guild, String tagUpper) {
        return context.databaseExecutor.supplyAsync(() -> {
            try {
                return prepareExecuteJoin(player, guild, tagUpper);
            } catch (SQLException e) {
                context.plugin.getLogger().severe("Failed to execute join: " + e.getMessage());
                return JoinResult.error("guild.create.error_generic", PlaceholderContext.of(player));
            }
        });
    }

    JoinCheckResult canJoinGuild(Player player, UUID guildId) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() != null) {
                return new JoinCheckResult(false, "guild.join.error_already_in_guild");
            }
            if (context.guildBanDao.exists(guildId, player.getUniqueId())) {
                return new JoinCheckResult(false, "guild.join.error_banned");
            }
            long leftAt = data.getLeftGuildAt();
            int cooldownMinutes = context.guildConfig.getInt("member-management.cooldown-minutes-after-leave", 1440);
            if (leftAt > 0 && cooldownMinutes > 0) {
                long elapsed = (TimeUtil.currentEpochSeconds() - leftAt) / 60;
                if (elapsed < cooldownMinutes) {
                    return new JoinCheckResult(false, "guild.join.error_cooldown");
                }
            }
            int maxMembers = context.guildConfig.getInt("member-management.max-members", 10);
            if (context.getMemberCount(guildId) >= maxMembers) {
                return new JoinCheckResult(false, "guild.join.error_max_members");
            }
            return new JoinCheckResult(true, null);
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to validate join requirements: " + e.getMessage());
            return new JoinCheckResult(false, "guild.create.error_generic");
        }
    }

    public record AcceptInviteResult(boolean success, boolean requestSent, boolean joinReady,
                                      Guild guild, String tagUpper,
                                      String errorKey, PlaceholderContext errorContext) {
        public static AcceptInviteResult error(String key, PlaceholderContext context) {
            return new AcceptInviteResult(false, false, false, null, null, key, context);
        }
    }

    public record JoinResult(boolean success, UUID playerUuid, String playerName, Guild guild, String tagUpper,
                              String errorKey, PlaceholderContext errorContext) {
        public static JoinResult error(String key, PlaceholderContext context) {
            return new JoinResult(false, null, null, null, null, key, context);
        }
    }

    record JoinCheckResult(boolean allowed, String key) {
    }
}
