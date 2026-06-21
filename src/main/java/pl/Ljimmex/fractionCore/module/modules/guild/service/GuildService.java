package pl.Ljimmex.fractionCore.module.modules.guild.service;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.database.dao.CuboidDao;
import pl.Ljimmex.fractionCore.database.dao.GuildAllyRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildBanDao;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.GuildDisbandHistoryDao;
import pl.Ljimmex.fractionCore.database.dao.GuildInviteDao;
import pl.Ljimmex.fractionCore.database.dao.GuildJoinRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildRelationDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.lang.LangManager;
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

    public GuildService(JavaPlugin plugin, GuildDao guildDao, PlayerDao playerDao, CuboidDao cuboidDao,
                        GuildBanDao guildBanDao, GuildInviteDao guildInviteDao, GuildJoinRequestDao guildJoinRequestDao,
                        GuildRelationDao guildRelationDao, GuildAllyRequestDao guildAllyRequestDao,
                        GuildDisbandHistoryDao guildDisbandHistoryDao,
                        FileConfiguration guildConfig, LangManager langManager) {
        this.context = new GuildContext(plugin, guildDao, playerDao, cuboidDao, guildBanDao, guildInviteDao, guildJoinRequestDao,
                guildRelationDao, guildAllyRequestDao, guildDisbandHistoryDao, guildConfig, langManager);
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

    public PlayerDao getPlayerDao() {
        return context.getPlayerDao();
    }

    public GuildDao getGuildDao() {
        return context.getGuildDao();
    }

    public FileConfiguration getGuildConfig() {
        return context.getGuildConfig();
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

    public boolean acceptInvite(Player player, String tag) {
        return memberManager.acceptInvite(player, tag);
    }

    public boolean sendRequestList(Player player) {
        return memberManager.sendRequestList(player);
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

    public boolean leaveGuild(Player player) {
        return memberManager.leaveGuild(player);
    }

    public boolean banPlayer(Player banner, String targetName, String reason) {
        return memberManager.banPlayer(banner, targetName, reason);
    }

    public boolean unbanPlayer(Player banner, String targetName) {
        return memberManager.unbanPlayer(banner, targetName);
    }

    public boolean sendBanList(Player player) {
        return memberManager.sendBanList(player);
    }

    // ============================================================
    // Rank management
    // ============================================================

    public boolean promotePlayer(Player promoter, String targetName, String targetRankName) {
        return rankManager.promotePlayer(promoter, targetName, targetRankName);
    }

    public boolean demotePlayer(Player demoter, String targetName, String targetRankName) {
        return rankManager.demotePlayer(demoter, targetName, targetRankName);
    }

    public boolean transferLeadership(Player leader, String targetName) {
        return rankManager.transferLeadership(leader, targetName);
    }

    // ============================================================
    // Guild settings
    // ============================================================

    public boolean setGuildHome(Player player) {
        return settingsManager.setGuildHome(player);
    }

    public boolean teleportHome(Player player) {
        return settingsManager.teleportHome(player);
    }

    public void cancelHomeTeleport(Player player) {
        settingsManager.cancelHomeTeleport(player);
    }

    public boolean setGuildDescription(Player player, String text) {
        return settingsManager.setGuildDescription(player, text);
    }

    public boolean setGuildFlag(Player player, String flagName, String value) {
        return settingsManager.setGuildFlag(player, flagName, value);
    }

    public boolean sendGuildInfo(Player player, String tag) {
        return settingsManager.sendGuildInfo(player, tag);
    }

    public boolean prepareDisband(Player player) {
        return disbandManager.prepareDisband(player);
    }

    public boolean confirmDisband(Player player) {
        return disbandManager.confirmDisband(player);
    }

    // ============================================================
    // Relations
    // ============================================================

    public boolean sendAllyRequest(Player player, String tag) {
        return context.getRelationManager().sendAllyRequest(player, tag);
    }

    public boolean acceptAllyRequest(Player player, String tag) {
        return context.getRelationManager().acceptAllyRequest(player, tag);
    }

    public boolean declineAllyRequest(Player player, String tag) {
        return context.getRelationManager().declineAllyRequest(player, tag);
    }

    public boolean setEnemy(Player player, String tag) {
        return context.getRelationManager().setEnemy(player, tag);
    }

    public boolean setNeutral(Player player, String tag) {
        return context.getRelationManager().setNeutral(player, tag);
    }

    public boolean sendRelationsList(Player player) {
        return context.getRelationManager().sendRelationsList(player);
    }
}
