package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.Ljimmex.fractionCore.database.entity.CuboidData;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildDisbandHistory;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuildDisbandManager {

    private final GuildContext context;
    private final Map<UUID, PendingDisband> pendingDisbands = new ConcurrentHashMap<>();

    public GuildDisbandManager(GuildContext context) {
        this.context = context;
    }

    public boolean prepareDisband(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (data.getRank() != GuildRank.LEADER) {
                context.send(player, "guild.disband.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Optional<Guild> guildOpt = context.guildDao.findById(data.getGuildId());
            if (guildOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild guild = guildOpt.get();
            long timeoutSeconds = context.guildConfig.getLong("disband.timeout-seconds", 60);
            if (timeoutSeconds <= 0) {
                timeoutSeconds = 60;
            }
            long expiresAt = System.currentTimeMillis() / 1000 + timeoutSeconds;

            PendingDisband existing = pendingDisbands.get(player.getUniqueId());
            if (existing != null) {
                Bukkit.getScheduler().cancelTask(existing.taskId());
            }

            int taskId = startActionBarTask(player, expiresAt);
            pendingDisbands.put(player.getUniqueId(), new PendingDisband(guild.getId(), expiresAt, taskId));
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to prepare guild disband: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean confirmDisband(Player player) {
        PendingDisband pending = pendingDisbands.get(player.getUniqueId());
        if (pending == null || pending.expiresAt < System.currentTimeMillis() / 1000) {
            if (pending != null) {
                Bukkit.getScheduler().cancelTask(pending.taskId());
                pendingDisbands.remove(player.getUniqueId());
            }
            context.send(player, "guild.disband.error_expired", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
        Bukkit.getScheduler().cancelTask(pending.taskId());
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null || !data.getGuildId().equals(pending.guildId)) {
                pendingDisbands.remove(player.getUniqueId());
                context.send(player, "guild.disband.error_changed", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Optional<Guild> guildOpt = context.guildDao.findById(pending.guildId);
            if (guildOpt.isEmpty()) {
                pendingDisbands.remove(player.getUniqueId());
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild guild = guildOpt.get();
            if (data.getRank() != GuildRank.LEADER) {
                pendingDisbands.remove(player.getUniqueId());
                context.send(player, "guild.disband.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            executeDisband(player, guild);
            pendingDisbands.remove(player.getUniqueId());
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to disband guild: " + e.getMessage());
            context.send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    private int startActionBarTask(Player player, long expiresAt) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    pendingDisbands.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                PendingDisband current = pendingDisbands.get(player.getUniqueId());
                if (current == null || current.expiresAt < System.currentTimeMillis() / 1000) {
                    this.cancel();
                    return;
                }
                long remaining = current.expiresAt - System.currentTimeMillis() / 1000;
                Component message = context.langManager.getMessageWithoutPrefix("guild.disband.action_bar",
                        PlaceholderContext.of(player).with("seconds", remaining));
                player.sendActionBar(message);
            }
        };
        return runnable.runTaskTimer(context.plugin, 0L, 20L).getTaskId();
    }

    private void executeDisband(Player player, Guild guild) throws SQLException {
        UUID guildId = guild.getId();
        long now = System.currentTimeMillis() / 1000;

        processRefund(player, guild);
        archiveGuild(guild, now, player.getUniqueId());
        clearGuildMembers(guildId, now);
        deleteGuildData(guildId);

        context.tagManager.updateAllTags();

        PlaceholderContext ctx = PlaceholderContext.empty()
                .with("player", player.getName())
                .with("guild", guild.getName())
                .with("tag", guild.getTag());
        Component broadcast = context.langManager.getMessage("guild.disband.broadcast", MessageType.INFO, ctx);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {
                online.sendMessage(broadcast);
            }
        }
        context.send(player, "guild.disband.success", MessageType.SUCCESS, ctx);
    }

    private void processRefund(Player player, Guild guild) {
        boolean refundEnabled = context.guildConfig.getBoolean("disband.refund.enabled", false);
        if (!refundEnabled) {
            return;
        }
        double percentage = Math.max(0.0, Math.min(1.0, context.guildConfig.getDouble("disband.refund.percentage", 0.5)));
        List<CostItem> foundationCost = context.loadFoundationCostItems();
        if (foundationCost.isEmpty()) {
            return;
        }
        List<CostItem> refund = foundationCost.stream()
                .map(item -> new CostItem(item.material(), (int) Math.floor(item.amount() * percentage)))
                .filter(item -> item.amount() > 0)
                .toList();
        if (!refund.isEmpty()) {
            context.giveItems(player, refund);
            context.send(player, "guild.disband.refund", MessageType.INFO,
                    PlaceholderContext.of(player)
                            .with("items", context.formatCostItems(refund))
                            .with("percentage", (int) (percentage * 100)));
        }
    }

    private void archiveGuild(Guild guild, long now, UUID disbandedBy) throws SQLException {
        GuildDisbandHistory history = new GuildDisbandHistory(
                0,
                guild.getId(),
                guild.getName(),
                guild.getTag(),
                guild.getColor(),
                guild.getLeaderUuid(),
                guild.getPoints(),
                guild.getLevel(),
                guild.getCreatedAt(),
                now,
                disbandedBy
        );
        context.guildDisbandHistoryDao.save(history);
    }

    private void clearGuildMembers(UUID guildId, long now) throws SQLException {
        List<PlayerData> members = context.playerDao.findByGuild(guildId);
        for (PlayerData member : members) {
            member.setGuildId(null);
            member.setRank(null);
            member.setJoinedGuildAt(0);
            member.setLeftGuildAt(now);
            context.playerDao.update(member);
            Player online = Bukkit.getPlayer(member.getUuid());
            if (online != null) {
                context.tagManager.clearPlayerTag(online);
                context.send(online, "guild.disband.member_message", MessageType.INFO,
                        PlaceholderContext.of(online).with("guild", ""));
            }
        }
    }

    private void deleteGuildData(UUID guildId) throws SQLException {
        Optional<CuboidData> cuboidOpt = context.cuboidManager.findCuboidByGuild(guildId);
        cuboidOpt.ifPresent(this::removeGuildEgg);

        context.guildInviteDao.deleteByGuild(guildId);
        context.guildJoinRequestDao.deleteByGuild(guildId);
        context.guildBanDao.deleteByGuild(guildId);
        context.cuboidManager.deleteCuboid(guildId);
        context.guildDao.delete(guildId);
    }

    private void removeGuildEgg(CuboidData cuboid) {
        World world = Bukkit.getWorld(cuboid.getWorld());
        if (world == null) {
            return;
        }
        int cx = cuboid.getCenterX();
        int cy = cuboid.getCenterY();
        int cz = cuboid.getCenterZ();

        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int y = cy - 1; y <= cy + 4; y++) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
    }

    private record PendingDisband(UUID guildId, long expiresAt, int taskId) {
    }
}
