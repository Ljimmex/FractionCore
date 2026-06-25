package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildInvite;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.util.TimeUtil;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildInviteManager {

    private final GuildContext context;

    public GuildInviteManager(GuildContext context) {
        this.context = context;
    }

    public boolean invitePlayer(Player inviter, Player target) {
        InviteResult result = prepareInvitePlayer(inviter, target);
        applyInviteEffects(inviter, target, result);
        return result.success();
    }

    public InviteResult prepareInvitePlayer(Player inviter, Player target) {
        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            return InviteResult.error("guild.invite.error_self", PlaceholderContext.of(inviter));
        }
        try {
            PlayerData inviterData = context.getOrCreatePlayerData(inviter);
            if (inviterData.getGuildId() == null) {
                return InviteResult.error("guild.no_guild", PlaceholderContext.of(inviter));
            }
            GuildRank inviterRank = inviterData.getRank();
            if (!context.isModeratorOrHigher(inviterRank)) {
                return InviteResult.error("guild.invite.error_no_permission", PlaceholderContext.of(inviter));
            }
            UUID guildId = inviterData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            if (guildOpt.isEmpty()) {
                return InviteResult.error("guild.not_found", PlaceholderContext.of(inviter));
            }
            Guild guild = guildOpt.get();

            PlayerData targetData = context.getOrCreatePlayerData(target);
            if (targetData.getGuildId() != null) {
                if (targetData.getGuildId().equals(guildId)) {
                    return InviteResult.error("guild.invite.error_already_member",
                            PlaceholderContext.of(inviter).with("player", target.getName()));
                } else {
                    return InviteResult.error("guild.invite.error_has_guild",
                            PlaceholderContext.of(inviter).with("player", target.getName()));
                }
            }
            if (context.guildBanDao.exists(guildId, target.getUniqueId())) {
                return InviteResult.error("guild.invite.error_banned",
                        PlaceholderContext.of(inviter).with("player", target.getName()));
            }
            int maxMembers = context.guildConfig.getInt("member-management.max-members", 10);
            if (context.getMemberCount(guildId) >= maxMembers) {
                return InviteResult.error("guild.invite.error_max_members", PlaceholderContext.of(inviter));
            }
            int maxInvites = context.guildConfig.getInt("member-management.max-invites-per-guild", 5);
            if (context.guildInviteDao.countByGuild(guildId) >= maxInvites) {
                return InviteResult.error("guild.invite.error_max_invites", PlaceholderContext.of(inviter));
            }
            context.guildInviteDao.delete(guildId, target.getUniqueId());
            context.guildJoinRequestDao.delete(guildId, target.getUniqueId());

            long now = TimeUtil.currentEpochSeconds();
            int timeoutMinutes = context.guildConfig.getInt("member-management.invite-timeout-minutes", 10);
            long expiresAt = now + timeoutMinutes * 60L;
            GuildInvite invite = new GuildInvite(0, guildId, target.getUniqueId(), inviter.getUniqueId(), now, expiresAt);
            context.guildInviteDao.save(invite);

            return new InviteResult(true, guild, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to invite player: " + e.getMessage());
            return InviteResult.error("guild.create.error_generic", PlaceholderContext.of(inviter));
        }
    }

    public void applyInviteEffects(Player inviter, Player target, InviteResult result) {
        if (!result.success()) {
            context.send(inviter, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild guild = result.guild();
        context.send(inviter, "guild.invite.sent", MessageType.SUCCESS,
                PlaceholderContext.of(inviter).with("player", target.getName()));

        if (!target.isOnline()) {
            return;
        }
        PlaceholderContext targetContext = PlaceholderContext.of(target, guild)
                .with("sender", inviter.getName())
                .with("guild", guild.getName())
                .with("tag", guild.getTag());
        context.send(target, "guild.invite.received", MessageType.INFO, targetContext);
        context.send(target, "guild.invite.hint", MessageType.INFO, targetContext.with("command", "/guild join " + guild.getTag()));
    }

    public CompletableFuture<InviteResult> invitePlayerAsync(Player inviter, Player target) {
        return context.databaseExecutor.supplyAsync(() -> prepareInvitePlayer(inviter, target));
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

    public record InviteResult(boolean success, Guild guild, String errorKey, PlaceholderContext errorContext) {
        public static InviteResult error(String key, PlaceholderContext context) {
            return new InviteResult(false, null, key, context);
        }
    }
}
