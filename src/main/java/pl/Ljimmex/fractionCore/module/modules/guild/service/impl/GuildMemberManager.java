package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildBan;
import pl.Ljimmex.fractionCore.database.entity.GuildInvite;
import pl.Ljimmex.fractionCore.database.entity.GuildJoinRequest;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuildMemberManager {

    private final GuildContext context;

    public GuildMemberManager(GuildContext context) {
        this.context = context;
    }

    public boolean invitePlayer(Player inviter, Player target) {
        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            context.send(inviter, "guild.invite.error_self", MessageType.ERROR, PlaceholderContext.of(inviter));
            return false;
        }
        try {
            PlayerData inviterData = context.getOrCreatePlayerData(inviter);
            if (inviterData.getGuildId() == null) {
                context.send(inviter, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            GuildRank inviterRank = inviterData.getRank();
            if (!context.isModeratorOrHigher(inviterRank)) {
                context.send(inviter, "guild.invite.error_no_permission", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            UUID guildId = inviterData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            if (guildOpt.isEmpty()) {
                context.send(inviter, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            Guild guild = guildOpt.get();

            PlayerData targetData = context.getOrCreatePlayerData(target);
            if (targetData.getGuildId() != null) {
                if (targetData.getGuildId().equals(guildId)) {
                    context.send(inviter, "guild.invite.error_already_member", MessageType.ERROR,
                            PlaceholderContext.of(inviter).with("player", target.getName()));
                } else {
                    context.send(inviter, "guild.invite.error_has_guild", MessageType.ERROR,
                            PlaceholderContext.of(inviter).with("player", target.getName()));
                }
                return false;
            }
            if (context.guildBanDao.exists(guildId, target.getUniqueId())) {
                context.send(inviter, "guild.invite.error_banned", MessageType.ERROR,
                        PlaceholderContext.of(inviter).with("player", target.getName()));
                return false;
            }
            int maxMembers = context.guildConfig.getInt("member-management.max-members", 10);
            if (context.getMemberCount(guildId) >= maxMembers) {
                context.send(inviter, "guild.invite.error_max_members", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            int maxInvites = context.guildConfig.getInt("member-management.max-invites-per-guild", 5);
            if (context.guildInviteDao.countByGuild(guildId) >= maxInvites) {
                context.send(inviter, "guild.invite.error_max_invites", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            context.guildInviteDao.delete(guildId, target.getUniqueId());
            context.guildJoinRequestDao.delete(guildId, target.getUniqueId());

            long now = System.currentTimeMillis() / 1000;
            int timeoutMinutes = context.guildConfig.getInt("member-management.invite-timeout-minutes", 10);
            long expiresAt = now + timeoutMinutes * 60L;
            GuildInvite invite = new GuildInvite(0, guildId, target.getUniqueId(), inviter.getUniqueId(), now, expiresAt);
            context.guildInviteDao.save(invite);

            context.send(inviter, "guild.invite.sent", MessageType.SUCCESS,
                    PlaceholderContext.of(inviter).with("player", target.getName()));

            PlaceholderContext targetContext = PlaceholderContext.of(target, guild)
                    .with("sender", inviter.getName())
                    .with("guild", guild.getName())
                    .with("tag", guild.getTag());
            context.send(target, "guild.invite.received", MessageType.INFO, targetContext);
            context.send(target, "guild.invite.hint", MessageType.INFO, targetContext.with("command", "/guild join " + guild.getTag()));
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to invite player: " + e.getMessage());
            context.send(inviter, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(inviter));
            return false;
        }
    }

    public boolean acceptInvite(Player player, String tag) {
        try {
            String tagUpper = tag.trim().toUpperCase();
            Optional<Guild> guildOpt = context.guildDao.findByTag(tagUpper);
            if (guildOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild guild = guildOpt.get();
            Optional<GuildInvite> inviteOpt = context.guildInviteDao.find(guild.getId(), player.getUniqueId());
            boolean hasValidInvite = inviteOpt.isPresent() && !inviteOpt.get().isExpired();
            if (!hasValidInvite) {
                if (inviteOpt.isPresent() && inviteOpt.get().isExpired()) {
                    context.guildInviteDao.delete(guild.getId(), player.getUniqueId());
                }
                if (guild.isAllowJoinRequests()) {
                    return sendJoinRequest(player, guild, tagUpper);
                }
                context.send(player, "guild.join.error_no_invite", MessageType.ERROR,
                        PlaceholderContext.of(player).with("tag", tagUpper));
                return false;
            }
            JoinCheckResult check = canJoinGuild(player, guild.getId());
            if (!check.allowed()) {
                PlaceholderContext ctx = PlaceholderContext.of(player).with("tag", tagUpper);
                if (check.key().equals("guild.join.error_cooldown")) {
                    int cooldownMinutes = context.guildConfig.getInt("member-management.cooldown-minutes-after-leave", 1440);
                    PlayerData data = context.getOrCreatePlayerData(player);
                    long remaining = Math.max(0, cooldownMinutes - (System.currentTimeMillis() / 1000 - data.getLeftGuildAt()) / 60);
                    ctx.with("minutes", remaining);
                }
                context.send(player, check.key(), MessageType.ERROR, ctx);
                return false;
            }
            return executeJoin(player, guild, tagUpper);
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to accept invite: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    private boolean sendJoinRequest(Player player, Guild guild, String tagUpper) throws SQLException {
        JoinCheckResult check = canJoinGuild(player, guild.getId());
        if (!check.allowed()) {
            PlaceholderContext ctx = PlaceholderContext.of(player).with("tag", tagUpper);
            if (check.key().equals("guild.join.error_cooldown")) {
                int cooldownMinutes = context.guildConfig.getInt("member-management.cooldown-minutes-after-leave", 1440);
                PlayerData data = context.getOrCreatePlayerData(player);
                long remaining = Math.max(0, cooldownMinutes - (System.currentTimeMillis() / 1000 - data.getLeftGuildAt()) / 60);
                ctx.with("minutes", remaining);
            }
            context.send(player, check.key(), MessageType.ERROR, ctx);
            return false;
        }
        if (context.guildJoinRequestDao.exists(guild.getId(), player.getUniqueId())) {
            context.send(player, "guild.join.error_request_exists", MessageType.ERROR,
                    PlaceholderContext.of(player).with("tag", tagUpper));
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        context.guildJoinRequestDao.save(new GuildJoinRequest(0, guild.getId(), player.getUniqueId(), now));
        context.send(player, "guild.join.request_sent", MessageType.INFO,
                PlaceholderContext.of(player, guild).with("guild", guild.getName()).with("tag", tagUpper));
        notifyLeadersOfJoinRequest(guild, player);
        return true;
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

    public boolean sendRequestList(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                context.send(player, "guild.requests.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            UUID guildId = data.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");
            List<GuildJoinRequest> requests = context.guildJoinRequestDao.findByGuild(guildId);
            if (requests.isEmpty()) {
                context.send(player, "guild.requests.empty", MessageType.INFO,
                        PlaceholderContext.of(player).with("guild", guildName).with("tag", tag));
                return true;
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
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to list join requests: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
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
            JoinCheckResult check = canJoinGuild(targetPlayer, guildId);
            if (!check.allowed()) {
                PlaceholderContext ctx = PlaceholderContext.of(player).with("player", targetData.getName());
                if (check.key().equals("guild.join.error_cooldown")) {
                    int cooldownMinutes = context.guildConfig.getInt("member-management.cooldown-minutes-after-leave", 1440);
                    long remaining = Math.max(0, cooldownMinutes - (System.currentTimeMillis() / 1000 - targetData.getLeftGuildAt()) / 60);
                    ctx.with("minutes", remaining);
                }
                context.send(player, check.key(), MessageType.ERROR, ctx);
                return false;
            }
            context.guildJoinRequestDao.delete(guildId, targetData.getUuid());
            boolean joined = executeJoin(targetPlayer, guild, guild.getTag());
            if (joined) {
                context.send(player, "guild.joinaccept.success", MessageType.SUCCESS,
                        PlaceholderContext.of(player).with("player", targetData.getName()));
            }
            return joined;
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

    private boolean executeJoin(Player player, Guild guild, String tagUpper) throws SQLException {
        List<CostItem> joinCost = context.loadJoinCostItems();
        if (context.isJoinCostEnabled() && !joinCost.isEmpty()) {
            if (!context.hasItems(player, joinCost)) {
                context.send(player, "guild.join.error_insufficient_items", MessageType.ERROR,
                        PlaceholderContext.of(player).with("items", context.formatCostItems(joinCost)).with("tag", tagUpper));
                return false;
            }
            if (!context.deductItems(player, joinCost)) {
                context.send(player, "guild.join.error_insufficient_items", MessageType.ERROR,
                        PlaceholderContext.of(player).with("items", context.formatCostItems(joinCost)).with("tag", tagUpper));
                return false;
            }
        }

        context.guildInviteDao.delete(guild.getId(), player.getUniqueId());
        context.guildJoinRequestDao.delete(guild.getId(), player.getUniqueId());
        PlayerData data = context.getOrCreatePlayerData(player);
        data.setGuildId(guild.getId());
        data.setRank(GuildRank.RECRUIT);
        long now = System.currentTimeMillis() / 1000;
        data.setJoinedGuildAt(now);
        data.setLeftGuildAt(0);
        context.playerDao.update(data);

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
        return true;
    }

    public boolean declineInvite(Player player, String tag) {
        try {
            String tagUpper = tag.trim().toUpperCase();
            Optional<Guild> guildOpt = context.guildDao.findByTag(tagUpper);
            if (guildOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild guild = guildOpt.get();
            Optional<GuildInvite> inviteOpt = context.guildInviteDao.find(guild.getId(), player.getUniqueId());
            if (inviteOpt.isEmpty()) {
                context.send(player, "guild.invite.error_no_invite", MessageType.ERROR,
                        PlaceholderContext.of(player).with("tag", tagUpper));
                return false;
            }
            context.guildInviteDao.delete(guild.getId(), player.getUniqueId());
            context.send(player, "guild.invite.declined", MessageType.INFO,
                    PlaceholderContext.of(player).with("guild", guild.getName()).with("tag", tagUpper));
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to decline invite: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean cancelInvites(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            GuildRank rank = data.getRank();
            if (!context.isModeratorOrHigher(rank)) {
                context.send(player, "guild.invite.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            context.guildInviteDao.deleteByGuild(data.getGuildId());
            context.send(player, "guild.invite.cancel", MessageType.SUCCESS, PlaceholderContext.of(player));
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to cancel invites: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean kickPlayer(Player kicker, String targetName) {
        if (kicker.getName().equalsIgnoreCase(targetName)) {
            context.send(kicker, "guild.kick.error_self", MessageType.ERROR, PlaceholderContext.of(kicker));
            return false;
        }
        try {
            PlayerData kickerData = context.getOrCreatePlayerData(kicker);
            if (kickerData.getGuildId() == null) {
                context.send(kicker, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(kicker));
                return false;
            }
            GuildRank kickerRank = kickerData.getRank();
            if (!context.isModeratorOrHigher(kickerRank)) {
                context.send(kicker, "guild.kick.error_no_permission", MessageType.ERROR, PlaceholderContext.of(kicker));
                return false;
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(kickerData.getGuildId())) {
                context.send(kicker, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(kicker).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            if (targetData.getRank() == GuildRank.LEADER) {
                context.send(kicker, "guild.kick.error_leader_kick", MessageType.ERROR,
                        PlaceholderContext.of(kicker).with("player", targetData.getName()));
                return false;
            }
            if (!canManageRank(kickerRank, targetData.getRank())) {
                context.send(kicker, "guild.kick.error_target_higher_rank", MessageType.ERROR,
                        PlaceholderContext.of(kicker).with("player", targetData.getName()));
                return false;
            }
            UUID guildId = kickerData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            context.removeFromGuild(targetData);

            context.send(kicker, "guild.kick.success", MessageType.SUCCESS,
                    PlaceholderContext.of(kicker).with("player", targetData.getName()));

            Component broadcast = context.langManager.getMessage("guild.kick.broadcast", MessageType.INFO,
                    PlaceholderContext.empty().with("player", targetData.getName()).with("guild", guildName).with("tag", tag));
            context.broadcastToGuild(guildId, broadcast, kicker);

            Player targetOnline = Bukkit.getPlayer(targetData.getUuid());
            if (targetOnline != null) {
                context.tagManager.clearPlayerTag(targetOnline);
                context.send(targetOnline, "guild.kick.target_message", MessageType.ERROR,
                        PlaceholderContext.of(targetOnline).with("guild", guildName).with("tag", tag));
            }
            context.tagManager.updateTagsForGuild(guildId);
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to kick player: " + e.getMessage());
            context.send(kicker, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(kicker));
            return false;
        }
    }

    public boolean leaveGuild(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (data.getRank() == GuildRank.LEADER) {
                context.send(player, "guild.leave.error_leader_cannot_leave", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            UUID guildId = data.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            context.removeFromGuild(data);
            context.tagManager.clearPlayerTag(player);
            context.tagManager.updateTagsForGuild(guildId);

            context.send(player, "guild.leave.success", MessageType.INFO,
                    PlaceholderContext.of(player).with("guild", guildName).with("tag", tag));
            Component broadcast = context.langManager.getMessage("guild.leave.broadcast", MessageType.INFO,
                    PlaceholderContext.empty().with("player", player.getName()).with("guild", guildName).with("tag", tag));
            context.broadcastToGuild(guildId, broadcast, player);
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to leave guild: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean banPlayer(Player banner, String targetName, String reason) {
        try {
            PlayerData bannerData = context.getOrCreatePlayerData(banner);
            if (bannerData.getGuildId() == null) {
                context.send(banner, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(banner));
                return false;
            }
            GuildRank bannerRank = bannerData.getRank();
            if (!context.isLeaderOrCoLeader(bannerRank)) {
                context.send(banner, "guild.ban.error_no_permission", MessageType.ERROR, PlaceholderContext.of(banner));
                return false;
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            UUID guildId = bannerData.getGuildId();
            if (targetOpt.isEmpty()) {
                context.send(banner, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(banner).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            UUID targetUuid = targetData.getUuid();
            String targetDisplayName = targetData.getName();
            GuildRank targetRank = targetData.getRank();
            if (targetData.getGuildId() != null && targetData.getGuildId().equals(guildId)) {
                if (targetRank == GuildRank.LEADER || !canManageRank(bannerRank, targetRank)) {
                    context.send(banner, "guild.ban.error_target_higher_rank", MessageType.ERROR,
                            PlaceholderContext.of(banner).with("player", targetDisplayName));
                    return false;
                }
            }
            if (context.guildBanDao.exists(guildId, targetUuid)) {
                context.send(banner, "guild.ban.error_already_banned", MessageType.ERROR,
                        PlaceholderContext.of(banner).with("player", targetDisplayName));
                return false;
            }
            if (targetData.getGuildId() != null && targetData.getGuildId().equals(guildId)) {
                context.removeFromGuild(targetData);
                Player targetOnline = Bukkit.getPlayer(targetUuid);
                if (targetOnline != null) {
                    context.tagManager.clearPlayerTag(targetOnline);
                }
                context.tagManager.updateTagsForGuild(guildId);
            }
            long now = System.currentTimeMillis() / 1000;
            GuildBan ban = new GuildBan(0, guildId, targetUuid, reason, banner.getUniqueId(), now);
            context.guildBanDao.save(ban);

            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            context.send(banner, "guild.ban.success", MessageType.SUCCESS,
                    PlaceholderContext.of(banner).with("player", targetDisplayName));
            Component broadcast = context.langManager.getMessage("guild.ban.broadcast", MessageType.INFO,
                    PlaceholderContext.empty().with("player", targetDisplayName).with("guild", guildName).with("tag", tag));
            context.broadcastToGuild(guildId, broadcast, banner);
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to ban player: " + e.getMessage());
            context.send(banner, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(banner));
            return false;
        }
    }

    public boolean unbanPlayer(Player banner, String targetName) {
        try {
            PlayerData bannerData = context.getOrCreatePlayerData(banner);
            if (bannerData.getGuildId() == null) {
                context.send(banner, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(banner));
                return false;
            }
            if (!context.isLeaderOrCoLeader(bannerData.getRank())) {
                context.send(banner, "guild.unban.error_no_permission", MessageType.ERROR, PlaceholderContext.of(banner));
                return false;
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty()) {
                context.send(banner, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(banner).with("player", targetName));
                return false;
            }
            UUID guildId = bannerData.getGuildId();
            UUID targetUuid = targetOpt.get().getUuid();
            String targetDisplayName = targetOpt.get().getName();
            if (!context.guildBanDao.exists(guildId, targetUuid)) {
                context.send(banner, "guild.unban.error_not_banned", MessageType.ERROR,
                        PlaceholderContext.of(banner).with("player", targetDisplayName));
                return false;
            }
            context.guildBanDao.delete(guildId, targetUuid);
            context.send(banner, "guild.unban.success", MessageType.SUCCESS,
                    PlaceholderContext.of(banner).with("player", targetDisplayName));
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to unban player: " + e.getMessage());
            context.send(banner, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(banner));
            return false;
        }
    }

    public boolean sendBanList(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                context.send(player, "guild.banlist.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            UUID guildId = data.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            List<GuildBan> bans = context.guildBanDao.findByGuild(guildId);
            if (bans.isEmpty()) {
                context.send(player, "guild.banlist.empty", MessageType.INFO,
                        PlaceholderContext.of(player).with("guild", guildName).with("tag", tag));
                return true;
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
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to show ban list: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    private JoinCheckResult canJoinGuild(Player player, UUID guildId) {
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
                long elapsed = (System.currentTimeMillis() / 1000 - leftAt) / 60;
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

    private record JoinCheckResult(boolean allowed, String key) {
    }


    private int getRankWeight(GuildRank rank) {
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

    private boolean canManageRank(GuildRank actor, GuildRank target) {
        return getRankWeight(actor) > getRankWeight(target);
    }
}
