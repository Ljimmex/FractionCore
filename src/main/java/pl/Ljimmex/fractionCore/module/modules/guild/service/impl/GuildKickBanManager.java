package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildBan;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.util.TimeUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildKickBanManager {

    private final GuildContext context;
    private final GuildRankHelper rankHelper;

    public GuildKickBanManager(GuildContext context) {
        this.context = context;
        this.rankHelper = new GuildRankHelper(context);
    }

    public boolean kickPlayer(Player kicker, String targetName) {
        KickResult result = prepareKickPlayer(kicker, targetName);
        applyKickEffects(kicker, result);
        return result.success();
    }

    public KickResult prepareKickPlayer(Player kicker, String targetName) {
        if (kicker.getName().equalsIgnoreCase(targetName)) {
            return KickResult.error("guild.kick.error_self", PlaceholderContext.of(kicker));
        }
        try {
            PlayerData kickerData = context.getOrCreatePlayerData(kicker);
            if (kickerData.getGuildId() == null) {
                return KickResult.error("guild.no_guild", PlaceholderContext.of(kicker));
            }
            GuildRank kickerRank = kickerData.getRank();
            if (!context.isModeratorOrHigher(kickerRank)) {
                return KickResult.error("guild.kick.error_no_permission", PlaceholderContext.of(kicker));
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(kickerData.getGuildId())) {
                return KickResult.error("guild.not_member", PlaceholderContext.of(kicker).with("player", targetName));
            }
            PlayerData targetData = targetOpt.get();
            if (targetData.getRank() == GuildRank.LEADER) {
                return KickResult.error("guild.kick.error_leader_kick",
                        PlaceholderContext.of(kicker).with("player", targetData.getName()));
            }
            if (!rankHelper.canManageRank(kickerRank, targetData.getRank())) {
                return KickResult.error("guild.kick.error_target_higher_rank",
                        PlaceholderContext.of(kicker).with("player", targetData.getName()));
            }
            UUID guildId = kickerData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            removeFromGuildData(targetData);

            return new KickResult(true, targetData.getUuid(), targetData.getName(), guildId, guildName, tag, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to kick player: " + e.getMessage());
            return KickResult.error("guild.create.error_generic", PlaceholderContext.of(kicker));
        }
    }

    public void applyKickEffects(Player kicker, KickResult result) {
        if (!result.success()) {
            context.send(kicker, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        UUID guildId = result.guildId();
        context.getPlayerGuildCache().remove(result.targetUuid());
        context.send(kicker, "guild.kick.success", MessageType.SUCCESS,
                PlaceholderContext.of(kicker).with("player", result.targetName()));

        Component broadcast = context.langManager.getMessage("guild.kick.broadcast", MessageType.INFO,
                PlaceholderContext.empty().with("player", result.targetName()).with("guild", result.guildName()).with("tag", result.tag()));
        context.broadcastToGuild(guildId, broadcast, kicker);

        Player targetOnline = Bukkit.getPlayer(result.targetUuid());
        if (targetOnline != null) {
            context.tagManager.clearPlayerTag(targetOnline);
            context.send(targetOnline, "guild.kick.target_message", MessageType.ERROR,
                    PlaceholderContext.of(targetOnline).with("guild", result.guildName()).with("tag", result.tag()));
        }
        context.tagManager.updateTagsForGuild(guildId);
    }

    public CompletableFuture<KickResult> kickPlayerAsync(Player kicker, String targetName) {
        return context.databaseExecutor.supplyAsync(() -> prepareKickPlayer(kicker, targetName));
    }

    public boolean banPlayer(Player banner, String targetName, String reason) {
        BanResult result = prepareBanPlayer(banner, targetName, reason);
        applyBanEffects(banner, result);
        return result.success();
    }

    public BanResult prepareBanPlayer(Player banner, String targetName, String reason) {
        try {
            PlayerData bannerData = context.getOrCreatePlayerData(banner);
            if (bannerData.getGuildId() == null) {
                return BanResult.error("guild.no_guild", PlaceholderContext.of(banner));
            }
            GuildRank bannerRank = bannerData.getRank();
            if (!context.isLeaderOrCoLeader(bannerRank)) {
                return BanResult.error("guild.ban.error_no_permission", PlaceholderContext.of(banner));
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            UUID guildId = bannerData.getGuildId();
            if (targetOpt.isEmpty()) {
                return BanResult.error("guild.not_member", PlaceholderContext.of(banner).with("player", targetName));
            }
            PlayerData targetData = targetOpt.get();
            UUID targetUuid = targetData.getUuid();
            String targetDisplayName = targetData.getName();
            GuildRank targetRank = targetData.getRank();
            if (targetData.getGuildId() != null && targetData.getGuildId().equals(guildId)) {
                if (targetRank == GuildRank.LEADER || !rankHelper.canManageRank(bannerRank, targetRank)) {
                    return BanResult.error("guild.ban.error_target_higher_rank",
                            PlaceholderContext.of(banner).with("player", targetDisplayName));
                }
            }
            if (context.guildBanDao.exists(guildId, targetUuid)) {
                return BanResult.error("guild.ban.error_already_banned",
                        PlaceholderContext.of(banner).with("player", targetDisplayName));
            }
            boolean wasMember = targetData.getGuildId() != null && targetData.getGuildId().equals(guildId);
            if (wasMember) {
                removeFromGuildData(targetData);
            }
            long now = TimeUtil.currentEpochSeconds();
            GuildBan ban = new GuildBan(0, guildId, targetUuid, reason, banner.getUniqueId(), now);
            context.guildBanDao.save(ban);

            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            return new BanResult(true, targetUuid, targetDisplayName, guildId, guildName, tag, wasMember, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to ban player: " + e.getMessage());
            return BanResult.error("guild.create.error_generic", PlaceholderContext.of(banner));
        }
    }

    public void applyBanEffects(Player banner, BanResult result) {
        if (!result.success()) {
            context.send(banner, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        UUID guildId = result.guildId();
        context.getPlayerGuildCache().remove(result.targetUuid());
        if (result.wasMember()) {
            Player targetOnline = Bukkit.getPlayer(result.targetUuid());
            if (targetOnline != null) {
                context.tagManager.clearPlayerTag(targetOnline);
            }
            context.tagManager.updateTagsForGuild(guildId);
        }
        context.send(banner, "guild.ban.success", MessageType.SUCCESS,
                PlaceholderContext.of(banner).with("player", result.targetName()));
        Component broadcast = context.langManager.getMessage("guild.ban.broadcast", MessageType.INFO,
                PlaceholderContext.empty().with("player", result.targetName()).with("guild", result.guildName()).with("tag", result.tag()));
        context.broadcastToGuild(guildId, broadcast, banner);
    }

    public CompletableFuture<BanResult> banPlayerAsync(Player banner, String targetName, String reason) {
        return context.databaseExecutor.supplyAsync(() -> prepareBanPlayer(banner, targetName, reason));
    }

    public boolean unbanPlayer(Player banner, String targetName) {
        UnbanResult result = prepareUnbanPlayer(banner, targetName);
        applyUnbanEffects(banner, result);
        return result.success();
    }

    public UnbanResult prepareUnbanPlayer(Player banner, String targetName) {
        try {
            PlayerData bannerData = context.getOrCreatePlayerData(banner);
            if (bannerData.getGuildId() == null) {
                return UnbanResult.error("guild.no_guild", PlaceholderContext.of(banner));
            }
            if (!context.isLeaderOrCoLeader(bannerData.getRank())) {
                return UnbanResult.error("guild.unban.error_no_permission", PlaceholderContext.of(banner));
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty()) {
                return UnbanResult.error("guild.not_member", PlaceholderContext.of(banner).with("player", targetName));
            }
            UUID guildId = bannerData.getGuildId();
            UUID targetUuid = targetOpt.get().getUuid();
            String targetDisplayName = targetOpt.get().getName();
            if (!context.guildBanDao.exists(guildId, targetUuid)) {
                return UnbanResult.error("guild.unban.error_not_banned",
                        PlaceholderContext.of(banner).with("player", targetDisplayName));
            }
            context.guildBanDao.delete(guildId, targetUuid);
            return new UnbanResult(true, targetDisplayName, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to unban player: " + e.getMessage());
            return UnbanResult.error("guild.create.error_generic", PlaceholderContext.of(banner));
        }
    }

    public void applyUnbanEffects(Player banner, UnbanResult result) {
        if (!result.success()) {
            context.send(banner, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        context.send(banner, "guild.unban.success", MessageType.SUCCESS,
                PlaceholderContext.of(banner).with("player", result.targetName()));
    }

    public CompletableFuture<UnbanResult> unbanPlayerAsync(Player banner, String targetName) {
        return context.databaseExecutor.supplyAsync(() -> prepareUnbanPlayer(banner, targetName));
    }

    public boolean sendBanList(Player player) {
        BanListResult result = fetchBanList(player);
        renderBanList(player, result);
        return result.success();
    }

    public BanListResult fetchBanList(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return BanListResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return BanListResult.error("guild.banlist.error_no_permission", PlaceholderContext.of(player));
            }
            UUID guildId = data.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            List<GuildBan> bans = context.guildBanDao.findByGuild(guildId);
            return new BanListResult(true, guildName, tag, bans, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to show ban list: " + e.getMessage());
            return BanListResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public void renderBanList(Player player, BanListResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        try {
            String guildName = result.guildName();
            String tag = result.tag();
            List<GuildBan> bans = result.bans();
            if (bans.isEmpty()) {
                context.send(player, "guild.banlist.empty", MessageType.INFO,
                        PlaceholderContext.of(player).with("guild", guildName).with("tag", tag));
                return;
            }

            context.send(player, "guild.banlist.header", MessageType.INFO,
                    PlaceholderContext.of(player).with("guild", guildName).with("tag", tag).with("count", bans.size()));
            for (GuildBan ban : bans) {
                Optional<PlayerData> targetOpt = context.playerDao.findByUuid(ban.getPlayerUuid());
                String targetName = targetOpt.map(PlayerData::getName).orElse("?");
                PlayerData bannerData = context.playerDao.findByUuid(ban.getBannedBy()).orElse(null);
                String bannerName = bannerData != null ? bannerData.getName() : "?";
                String reason = ban.getReason() != null && !ban.getReason().isEmpty() ? ban.getReason() : context.langManager.getRawMessage("placeholders.not_available");
                context.send(player, "guild.banlist.entry", MessageType.INFO,
                        PlaceholderContext.of(player)
                                .with("player", targetName)
                                .with("banner", bannerName)
                                .with("reason", reason != null ? reason : "N/A")
                                .with("date", context.formatDate(ban.getBannedAt())));
            }
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to render ban list: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
        }
    }

    public CompletableFuture<BanListResult> sendBanListAsync(Player player) {
        return context.databaseExecutor.supplyAsync(() -> fetchBanList(player));
    }

    private void removeFromGuildData(PlayerData data) throws SQLException {
        data.setGuildId(null);
        data.setRank(null);
        data.setJoinedGuildAt(0);
        data.setLeftGuildAt(TimeUtil.currentEpochSeconds());
        context.playerDao.update(data);
    }

    public record KickResult(boolean success, UUID targetUuid, String targetName, UUID guildId,
                              String guildName, String tag,
                              String errorKey, PlaceholderContext errorContext) {
        public static KickResult error(String key, PlaceholderContext context) {
            return new KickResult(false, null, null, null, null, null, key, context);
        }
    }

    public record BanResult(boolean success, UUID targetUuid, String targetName, UUID guildId,
                             String guildName, String tag, boolean wasMember,
                             String errorKey, PlaceholderContext errorContext) {
        public static BanResult error(String key, PlaceholderContext context) {
            return new BanResult(false, null, null, null, null, null, false, key, context);
        }
    }

    public record UnbanResult(boolean success, String targetName,
                               String errorKey, PlaceholderContext errorContext) {
        public static UnbanResult error(String key, PlaceholderContext context) {
            return new UnbanResult(false, null, key, context);
        }
    }

    public record BanListResult(boolean success, String guildName, String tag, List<GuildBan> bans,
                                 String errorKey, PlaceholderContext errorContext) {
        public static BanListResult error(String key, PlaceholderContext context) {
            return new BanListResult(false, "?", "?", List.of(), key, context);
        }
    }
}
