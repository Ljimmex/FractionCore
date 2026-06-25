package pl.Ljimmex.fractionCore.module.modules.guild.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.async.DatabaseExecutor;
import pl.Ljimmex.fractionCore.config.ModuleConfig;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.database.dao.CuboidDao;
import pl.Ljimmex.fractionCore.database.dao.GuildAllyRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildBanDao;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.GuildDisbandHistoryDao;
import pl.Ljimmex.fractionCore.database.dao.GuildFlagDao;
import pl.Ljimmex.fractionCore.database.dao.GuildInviteDao;
import pl.Ljimmex.fractionCore.database.dao.GuildJoinRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildRelationDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.CuboidManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.GuildContext;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.GuildCreationManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.GuildDisbandManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.GuildMemberManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.GuildRankManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.GuildRelationManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.GuildSettingsManager;

public class GuildService {

    private final GuildContext context;
    private final GuildCreationManager creationManager;
    private final GuildMemberManager memberManager;
    private final GuildRankManager rankManager;
    private final GuildSettingsManager settingsManager;
    private final GuildDisbandManager disbandManager;

    public GuildService(JavaPlugin plugin, DatabaseExecutor databaseExecutor, GuildDao guildDao, PlayerDao playerDao, CuboidDao cuboidDao,
                        GuildBanDao guildBanDao, GuildInviteDao guildInviteDao, GuildJoinRequestDao guildJoinRequestDao,
                        GuildRelationDao guildRelationDao, GuildAllyRequestDao guildAllyRequestDao,
                        GuildDisbandHistoryDao guildDisbandHistoryDao,
                        GuildFlagDao guildFlagDao,
                        ModuleConfig guildConfig, LangManager langManager) {
        this.context = new GuildContext(plugin, databaseExecutor, guildDao, playerDao, cuboidDao, guildBanDao, guildInviteDao, guildJoinRequestDao,
                guildRelationDao, guildAllyRequestDao, guildDisbandHistoryDao, guildFlagDao, guildConfig, langManager);
        this.creationManager = new GuildCreationManager(context);
        this.memberManager = new GuildMemberManager(context);
        this.rankManager = new GuildRankManager(context);
        this.settingsManager = new GuildSettingsManager(context);
        this.disbandManager = new GuildDisbandManager(context);
    }

    public GuildTagManager getTagManager() {
        return context.getTagManager();
    }

    public GuildRelationManager getRelationManager() {
        return context.getRelationManager();
    }

    public CuboidManager getCuboidManager() {
        return context.getCuboidManager();
    }

    public PlayerDao getPlayerDao() {
        return context.getPlayerDao();
    }

    public GuildDao getGuildDao() {
        return context.getGuildDao();
    }

    public ModuleConfig getGuildConfig() {
        return context.getGuildConfig();
    }

    public LangManager getLangManager() {
        return context.getLangManager();
    }

    public GuildContext getContext() {
        return context;
    }

    public void refreshPlayerCache(java.util.UUID playerUuid) {
        try {
            context.getPlayerDao().findByUuid(playerUuid)
                    .ifPresent(data -> context.getPlayerGuildCache().refresh(playerUuid, data));
        } catch (java.sql.SQLException e) {
            context.getPlugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "Failed to refresh player cache for " + playerUuid, e);
        }
    }

    public void invalidatePlayerCache(java.util.UUID playerUuid) {
        context.getPlayerGuildCache().remove(playerUuid);
    }

    private void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(context.getPlugin(), task);
    }

    private boolean isOnline(Player player) {
        return player.isOnline();
    }

    // ============================================================
    // Guild creation
    // ============================================================

    public GuildCreateResult prepareCreation(Player founder, String rawName, String rawTag) {
        return creationManager.prepareCreation(founder, rawName, rawTag);
    }

    public GuildCreateResult confirmCreation(Player founder) {
        return creationManager.confirmCreation(founder);
    }

    public java.util.concurrent.CompletableFuture<GuildCreateResult> prepareCreationAsync(Player founder, String rawName, String rawTag) {
        return creationManager.prepareCreationAsync(founder, rawName, rawTag);
    }

    public java.util.concurrent.CompletableFuture<GuildCreateResult> confirmCreationAsync(Player founder) {
        return creationManager.confirmCreationAsync(founder);
    }

    public Component buildPreview(Player founder, String rawName, String rawTag) {
        return creationManager.buildPreview(founder, rawName, rawTag);
    }

    public Component getResultMessage(Player player, GuildCreateResult result, String rawName, String rawTag) {
        return creationManager.getResultMessage(player, result, rawName, rawTag);
    }

    // ============================================================
    // Member management
    // ============================================================

    public boolean invitePlayer(Player inviter, Player target) {
        return memberManager.invitePlayer(inviter, target);
    }

    public void invitePlayerAsync(Player inviter, Player target) {
        memberManager.invitePlayerAsync(inviter, target)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(inviter)) {
                        return;
                    }
                    memberManager.applyInviteEffects(inviter, target, result);
                }));
    }

    public boolean acceptInvite(Player player, String tag) {
        return memberManager.acceptInvite(player, tag);
    }

    public void acceptInviteAsync(Player player, String tag) {
        memberManager.acceptInviteAsync(player, tag)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    if (!result.success()) {
                        context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
                        return;
                    }
                    if (result.requestSent()) {
                        memberManager.applyJoinRequestEffects(player, result.guild(), result.tagUpper());
                        return;
                    }
                    if (!memberManager.checkAndDeductJoinCost(player, result.tagUpper())) {
                        context.send(player, "guild.join.error_insufficient_items", MessageType.ERROR,
                                memberManager.joinCostErrorContext(player, result.tagUpper()));
                        return;
                    }
                    memberManager.executeJoinAsync(player, result.guild(), result.tagUpper())
                            .thenAccept(joinResult -> runSync(() -> {
                                if (!isOnline(player)) {
                                    return;
                                }
                                memberManager.applyJoinEffects(player, joinResult);
                            }));
                }));
    }

    public boolean sendRequestList(Player player) {
        return memberManager.sendRequestList(player);
    }

    public void sendRequestListAsync(Player player) {
        memberManager.sendRequestListAsync(player)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    memberManager.renderRequestList(player, result);
                }));
    }

    public boolean acceptJoinRequest(Player player, String targetName) {
        return memberManager.acceptJoinRequest(player, targetName);
    }

    public boolean declineJoinRequest(Player player, String targetName) {
        return memberManager.declineJoinRequest(player, targetName);
    }

    public boolean declineInvite(Player player, String tag) {
        return memberManager.declineInvite(player, tag);
    }

    public boolean cancelInvites(Player player) {
        return memberManager.cancelInvites(player);
    }

    public boolean kickPlayer(Player kicker, String targetName) {
        return memberManager.kickPlayer(kicker, targetName);
    }

    public void kickPlayerAsync(Player kicker, String targetName) {
        memberManager.kickPlayerAsync(kicker, targetName)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(kicker)) {
                        return;
                    }
                    memberManager.applyKickEffects(kicker, result);
                }));
    }

    public boolean leaveGuild(Player player) {
        return memberManager.leaveGuild(player);
    }

    public void leaveGuildAsync(Player player) {
        memberManager.leaveGuildAsync(player)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    memberManager.applyLeaveEffects(player, result);
                }));
    }

    public boolean banPlayer(Player banner, String targetName, String reason) {
        return memberManager.banPlayer(banner, targetName, reason);
    }

    public void banPlayerAsync(Player banner, String targetName, String reason) {
        memberManager.banPlayerAsync(banner, targetName, reason)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(banner)) {
                        return;
                    }
                    memberManager.applyBanEffects(banner, result);
                }));
    }

    public boolean unbanPlayer(Player banner, String targetName) {
        return memberManager.unbanPlayer(banner, targetName);
    }

    public void unbanPlayerAsync(Player banner, String targetName) {
        memberManager.unbanPlayerAsync(banner, targetName)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(banner)) {
                        return;
                    }
                    memberManager.applyUnbanEffects(banner, result);
                }));
    }

    public boolean sendBanList(Player player) {
        return memberManager.sendBanList(player);
    }

    public void sendBanListAsync(Player player) {
        memberManager.sendBanListAsync(player)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    memberManager.renderBanList(player, result);
                }));
    }

    // ============================================================
    // Rank management
    // ============================================================

    public boolean promotePlayer(Player promoter, String targetName, String targetRankName) {
        return rankManager.promotePlayer(promoter, targetName, targetRankName);
    }

    public void promotePlayerAsync(Player promoter, String targetName, String targetRankName) {
        rankManager.promotePlayerAsync(promoter, targetName, targetRankName)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(promoter)) {
                        return;
                    }
                    rankManager.applyRankChangeEffects(promoter, result, "guild.promote");
                }));
    }

    public boolean demotePlayer(Player demoter, String targetName, String targetRankName) {
        return rankManager.demotePlayer(demoter, targetName, targetRankName);
    }

    public void demotePlayerAsync(Player demoter, String targetName, String targetRankName) {
        rankManager.demotePlayerAsync(demoter, targetName, targetRankName)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(demoter)) {
                        return;
                    }
                    rankManager.applyRankChangeEffects(demoter, result, "guild.demote");
                }));
    }

    public boolean transferLeadership(Player leader, String targetName) {
        return rankManager.transferLeadership(leader, targetName);
    }

    public void transferLeadershipAsync(Player leader, String targetName) {
        rankManager.transferLeadershipAsync(leader, targetName)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(leader)) {
                        return;
                    }
                    rankManager.applyTransferLeadershipEffects(leader, result);
                }));
    }

    // ============================================================
    // Guild settings
    // ============================================================

    public boolean setGuildHome(Player player) {
        return settingsManager.setGuildHome(player);
    }

    public void setGuildHomeAsync(Player player) {
        Location loc = player.getLocation();
        settingsManager.setGuildHomeAsync(player, loc)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    settingsManager.applySetGuildHomeEffects(player, result);
                }));
    }

    public boolean teleportHome(Player player) {
        return settingsManager.teleportHome(player);
    }

    public void teleportHomeAsync(Player player) {
        settingsManager.teleportHomeAsync(player)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    settingsManager.applyTeleportHomeEffects(player, result);
                }));
    }

    public void cancelHomeTeleport(Player player) {
        settingsManager.cancelHomeTeleport(player);
    }

    public boolean setGuildDescription(Player player, String text) {
        return settingsManager.setGuildDescription(player, text);
    }

    public void setGuildDescriptionAsync(Player player, String text) {
        settingsManager.setGuildDescriptionAsync(player, text)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    settingsManager.applySetDescriptionEffects(player, result);
                }));
    }

    public boolean setGuildFlag(Player player, String flagName, String value) {
        return settingsManager.setGuildFlag(player, flagName, value);
    }

    public void setGuildFlagAsync(Player player, String flagName, String value) {
        settingsManager.setGuildFlagAsync(player, flagName, value)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    settingsManager.applySetFlagEffects(player, result);
                }));
    }

    public boolean sendGuildInfo(Player player, String tag) {
        return settingsManager.sendGuildInfo(player, tag);
    }

    /**
     * Fetches guild info asynchronously and renders the result on the main thread.
     */
    public void sendGuildInfoAsync(Player player, String tag) {
        context.getDatabaseExecutor().supplyAsync(() -> settingsManager.fetchGuildInfo(player, tag))
                .thenAccept(result -> Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (result.info() == null) {
                        if (result.errorKey() != null) {
                            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
                        }
                        return;
                    }
                    settingsManager.renderGuildInfo(player, result.info(), tag);
                }));
    }

    public boolean prepareDisband(Player player) {
        return disbandManager.prepareDisband(player);
    }

    public void prepareDisbandAsync(Player player) {
        disbandManager.prepareDisbandAsync(player)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    disbandManager.applyPrepareDisbandEffects(player, result);
                }));
    }

    public boolean confirmDisband(Player player) {
        return disbandManager.confirmDisband(player);
    }

    public void confirmDisbandAsync(Player player) {
        disbandManager.confirmDisbandAsync(player)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    disbandManager.applyConfirmDisbandEffects(player, result);
                }));
    }

    // ============================================================
    // Relations
    // ============================================================

    public boolean sendAllyRequest(Player player, String tag) {
        return context.getRelationManager().sendAllyRequest(player, tag);
    }

    public void sendAllyRequestAsync(Player player, String tag) {
        context.getRelationManager().sendAllyRequestAsync(player, tag)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    context.getRelationManager().applySendAllyRequestEffects(player, result);
                }));
    }

    public boolean acceptAllyRequest(Player player, String tag) {
        return context.getRelationManager().acceptAllyRequest(player, tag);
    }

    public void acceptAllyRequestAsync(Player player, String tag) {
        context.getRelationManager().acceptAllyRequestAsync(player, tag)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    context.getRelationManager().applyAcceptAllyEffects(player, result);
                }));
    }

    public boolean declineAllyRequest(Player player, String tag) {
        return context.getRelationManager().declineAllyRequest(player, tag);
    }

    public void declineAllyRequestAsync(Player player, String tag) {
        context.getRelationManager().declineAllyRequestAsync(player, tag)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    context.getRelationManager().applyDeclineAllyEffects(player, result);
                }));
    }

    public boolean setEnemy(Player player, String tag) {
        return context.getRelationManager().setEnemy(player, tag);
    }

    public void setEnemyAsync(Player player, String tag) {
        context.getRelationManager().setEnemyAsync(player, tag)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    context.getRelationManager().applyRelationChangeEffects(player, result, "guild.enemy");
                }));
    }

    public boolean setNeutral(Player player, String tag) {
        return context.getRelationManager().setNeutral(player, tag);
    }

    public void setNeutralAsync(Player player, String tag) {
        context.getRelationManager().setNeutralAsync(player, tag)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    context.getRelationManager().applyRelationChangeEffects(player, result, "guild.neutral");
                }));
    }

    public boolean sendRelationsList(Player player) {
        return context.getRelationManager().sendRelationsList(player);
    }

    public void sendRelationsListAsync(Player player) {
        context.getRelationManager().sendRelationsListAsync(player)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    context.getRelationManager().renderRelationsList(player, result);
                }));
    }

    // ============================================================
    // Cuboid management
    // ============================================================

    public boolean setCuboidFlag(Player player, String flagName, String valueName) {
        return context.getCuboidManager().setCuboidFlag(player, flagName, valueName);
    }

    public void setCuboidFlagAsync(Player player, String flagName, String valueName) {
        context.getCuboidManager().setCuboidFlagAsync(player, flagName, valueName)
                .thenAccept(result -> runSync(() -> {
                    if (!isOnline(player)) {
                        return;
                    }
                    context.getCuboidManager().applySetCuboidFlagEffects(player, result);
                }));
    }

    public boolean sendCuboidFlagList(Player player) {
        return context.getCuboidManager().sendCuboidFlagList(player);
    }

}
