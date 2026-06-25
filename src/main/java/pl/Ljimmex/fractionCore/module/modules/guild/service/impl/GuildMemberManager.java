package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.util.TimeUtil;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildMemberManager {

    private final GuildContext context;
    private final GuildInviteManager inviteManager;
    private final GuildJoinManager joinManager;
    private final GuildKickBanManager kickBanManager;
    private final GuildRequestManager requestManager;

    public GuildMemberManager(GuildContext context) {
        this.context = context;
        this.inviteManager = new GuildInviteManager(context);
        this.joinManager = new GuildJoinManager(context);
        this.kickBanManager = new GuildKickBanManager(context);
        this.requestManager = new GuildRequestManager(context, joinManager);
    }

    public GuildInviteManager getInviteManager() {
        return inviteManager;
    }

    public GuildJoinManager getJoinManager() {
        return joinManager;
    }

    public GuildKickBanManager getKickBanManager() {
        return kickBanManager;
    }

    public GuildRequestManager getRequestManager() {
        return requestManager;
    }

    // ============================================================
    // Invites
    // ============================================================

    public boolean invitePlayer(Player inviter, Player target) {
        return inviteManager.invitePlayer(inviter, target);
    }

    public GuildInviteManager.InviteResult prepareInvitePlayer(Player inviter, Player target) {
        return inviteManager.prepareInvitePlayer(inviter, target);
    }

    public void applyInviteEffects(Player inviter, Player target, GuildInviteManager.InviteResult result) {
        inviteManager.applyInviteEffects(inviter, target, result);
    }

    public CompletableFuture<GuildInviteManager.InviteResult> invitePlayerAsync(Player inviter, Player target) {
        return inviteManager.invitePlayerAsync(inviter, target);
    }

    public boolean declineInvite(Player player, String tag) {
        return inviteManager.declineInvite(player, tag);
    }

    public boolean cancelInvites(Player player) {
        return inviteManager.cancelInvites(player);
    }

    // ============================================================
    // Join
    // ============================================================

    public boolean acceptInvite(Player player, String tag) {
        return joinManager.acceptInvite(player, tag);
    }

    public GuildJoinManager.AcceptInviteResult prepareAcceptInvite(Player player, String tag) {
        return joinManager.prepareAcceptInvite(player, tag);
    }

    public void applyJoinRequestEffects(Player player, Guild guild, String tagUpper) {
        joinManager.applyJoinRequestEffects(player, guild, tagUpper);
    }

    public CompletableFuture<GuildJoinManager.AcceptInviteResult> acceptInviteAsync(Player player, String tag) {
        return joinManager.acceptInviteAsync(player, tag);
    }

    public GuildJoinManager.JoinResult executeJoinSync(Player player, Guild guild, String tagUpper) {
        return joinManager.executeJoinSync(player, guild, tagUpper);
    }

    public CompletableFuture<GuildJoinManager.JoinResult> executeJoinAsync(Player player, Guild guild, String tagUpper) {
        return joinManager.executeJoinAsync(player, guild, tagUpper);
    }

    public boolean checkAndDeductJoinCost(Player player, String tagUpper) {
        return joinManager.checkAndDeductJoinCost(player, tagUpper);
    }

    public PlaceholderContext joinCostErrorContext(Player player, String tagUpper) {
        return joinManager.joinCostErrorContext(player, tagUpper);
    }

    public GuildJoinManager.JoinResult prepareExecuteJoin(Player player, Guild guild, String tagUpper) throws SQLException {
        return joinManager.prepareExecuteJoin(player, guild, tagUpper);
    }

    public void applyJoinEffects(Player player, GuildJoinManager.JoinResult result) {
        joinManager.applyJoinEffects(player, result);
    }

    // ============================================================
    // Kick / ban / banlist
    // ============================================================

    public boolean kickPlayer(Player kicker, String targetName) {
        return kickBanManager.kickPlayer(kicker, targetName);
    }

    public GuildKickBanManager.KickResult prepareKickPlayer(Player kicker, String targetName) {
        return kickBanManager.prepareKickPlayer(kicker, targetName);
    }

    public void applyKickEffects(Player kicker, GuildKickBanManager.KickResult result) {
        kickBanManager.applyKickEffects(kicker, result);
    }

    public CompletableFuture<GuildKickBanManager.KickResult> kickPlayerAsync(Player kicker, String targetName) {
        return kickBanManager.kickPlayerAsync(kicker, targetName);
    }

    public boolean banPlayer(Player banner, String targetName, String reason) {
        return kickBanManager.banPlayer(banner, targetName, reason);
    }

    public GuildKickBanManager.BanResult prepareBanPlayer(Player banner, String targetName, String reason) {
        return kickBanManager.prepareBanPlayer(banner, targetName, reason);
    }

    public void applyBanEffects(Player banner, GuildKickBanManager.BanResult result) {
        kickBanManager.applyBanEffects(banner, result);
    }

    public CompletableFuture<GuildKickBanManager.BanResult> banPlayerAsync(Player banner, String targetName, String reason) {
        return kickBanManager.banPlayerAsync(banner, targetName, reason);
    }

    public boolean unbanPlayer(Player banner, String targetName) {
        return kickBanManager.unbanPlayer(banner, targetName);
    }

    public GuildKickBanManager.UnbanResult prepareUnbanPlayer(Player banner, String targetName) {
        return kickBanManager.prepareUnbanPlayer(banner, targetName);
    }

    public void applyUnbanEffects(Player banner, GuildKickBanManager.UnbanResult result) {
        kickBanManager.applyUnbanEffects(banner, result);
    }

    public CompletableFuture<GuildKickBanManager.UnbanResult> unbanPlayerAsync(Player banner, String targetName) {
        return kickBanManager.unbanPlayerAsync(banner, targetName);
    }

    public boolean sendBanList(Player player) {
        return kickBanManager.sendBanList(player);
    }

    public GuildKickBanManager.BanListResult fetchBanList(Player player) {
        return kickBanManager.fetchBanList(player);
    }

    public void renderBanList(Player player, GuildKickBanManager.BanListResult result) {
        kickBanManager.renderBanList(player, result);
    }

    public CompletableFuture<GuildKickBanManager.BanListResult> sendBanListAsync(Player player) {
        return kickBanManager.sendBanListAsync(player);
    }

    // ============================================================
    // Join requests
    // ============================================================

    public boolean sendRequestList(Player player) {
        return requestManager.sendRequestList(player);
    }

    public GuildRequestManager.RequestListResult fetchRequestList(Player player) {
        return requestManager.fetchRequestList(player);
    }

    public void renderRequestList(Player player, GuildRequestManager.RequestListResult result) {
        requestManager.renderRequestList(player, result);
    }

    public CompletableFuture<GuildRequestManager.RequestListResult> sendRequestListAsync(Player player) {
        return requestManager.sendRequestListAsync(player);
    }

    public boolean acceptJoinRequest(Player player, String targetName) {
        return requestManager.acceptJoinRequest(player, targetName);
    }

    public boolean declineJoinRequest(Player player, String targetName) {
        return requestManager.declineJoinRequest(player, targetName);
    }

    // ============================================================
    // Leave (kept in facade; not part of the four split managers)
    // ============================================================

    public boolean leaveGuild(Player player) {
        LeaveResult result = prepareLeaveGuild(player);
        applyLeaveEffects(player, result);
        return result.success();
    }

    public LeaveResult prepareLeaveGuild(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return LeaveResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (data.getRank() == GuildRank.LEADER) {
                return LeaveResult.error("guild.leave.error_leader_cannot_leave", PlaceholderContext.of(player));
            }
            UUID guildId = data.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            removeFromGuildData(data);

            return new LeaveResult(true, guildId, guildName, tag, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to leave guild: " + e.getMessage());
            return LeaveResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applyLeaveEffects(Player player, LeaveResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        UUID guildId = result.guildId();
        context.getPlayerGuildCache().remove(player.getUniqueId());
        context.tagManager.clearPlayerTag(player);
        context.tagManager.updateTagsForGuild(guildId);

        context.send(player, "guild.leave.success", MessageType.INFO,
                PlaceholderContext.of(player).with("guild", result.guildName()).with("tag", result.tag()));
        Component broadcast = context.langManager.getMessage("guild.leave.broadcast", MessageType.INFO,
                PlaceholderContext.empty().with("player", player.getName()).with("guild", result.guildName()).with("tag", result.tag()));
        context.broadcastToGuild(guildId, broadcast, player);
    }

    public CompletableFuture<LeaveResult> leaveGuildAsync(Player player) {
        return context.databaseExecutor.supplyAsync(() -> prepareLeaveGuild(player));
    }

    public record LeaveResult(boolean success, UUID guildId, String guildName, String tag,
                               String errorKey, PlaceholderContext errorContext) {
        public static LeaveResult error(String key, PlaceholderContext context) {
            return new LeaveResult(false, null, null, null, key, context);
        }
    }

    private void removeFromGuildData(PlayerData data) throws SQLException {
        data.setGuildId(null);
        data.setRank(null);
        data.setJoinedGuildAt(0);
        data.setLeftGuildAt(TimeUtil.currentEpochSeconds());
        context.playerDao.update(data);
    }
}
