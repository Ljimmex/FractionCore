package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import pl.Ljimmex.fractionCore.util.TimeUtil;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GuildDisbandManager {

    private final GuildContext context;
    private final Map<UUID, PendingDisband> pendingDisbands = new ConcurrentHashMap<>();

    public GuildDisbandManager(GuildContext context) {
        this.context = context;
    }

    public boolean prepareDisband(Player player) {
        PrepareDisbandResult result = prepareDisbandData(player);
        applyPrepareDisbandEffects(player, result);
        return result.success();
    }

    public PrepareDisbandResult prepareDisbandData(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return PrepareDisbandResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (data.getRank() != GuildRank.LEADER) {
                return PrepareDisbandResult.error("guild.disband.error_no_permission", PlaceholderContext.of(player));
            }
            Optional<Guild> guildOpt = context.guildDao.findById(data.getGuildId());
            if (guildOpt.isEmpty()) {
                return PrepareDisbandResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            long timeoutSeconds = context.guildConfig.getLong("disband.timeout-seconds", 60);
            if (timeoutSeconds <= 0) {
                timeoutSeconds = 60;
            }
            long expiresAt = TimeUtil.currentEpochSeconds() + timeoutSeconds;
            return new PrepareDisbandResult(true, guildOpt.get().getId(), expiresAt, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to prepare guild disband: " + e.getMessage());
            return PrepareDisbandResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applyPrepareDisbandEffects(Player player, PrepareDisbandResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        PendingDisband existing = pendingDisbands.get(player.getUniqueId());
        if (existing != null) {
            Bukkit.getScheduler().cancelTask(existing.taskId());
        }
        int taskId = startActionBarTask(player, result.expiresAt());
        pendingDisbands.put(player.getUniqueId(), new PendingDisband(result.guildId(), result.expiresAt(), taskId));
    }

    public CompletableFuture<PrepareDisbandResult> prepareDisbandAsync(Player player) {
        return context.databaseExecutor.supplyAsync(() -> prepareDisbandData(player));
    }

    public record PrepareDisbandResult(boolean success, UUID guildId, long expiresAt,
                                        String errorKey, PlaceholderContext errorContext) {
        public static PrepareDisbandResult error(String key, PlaceholderContext context) {
            return new PrepareDisbandResult(false, null, 0, key, context);
        }
    }

    public boolean confirmDisband(Player player) {
        ConfirmDisbandResult result = prepareConfirmDisbandData(player);
        applyConfirmDisbandEffects(player, result);
        return result.success();
    }

    public ConfirmDisbandResult prepareConfirmDisbandData(Player player) {
        PendingDisband pending = pendingDisbands.get(player.getUniqueId());
        if (pending == null || pending.expiresAt < TimeUtil.currentEpochSeconds()) {
            if (pending != null) {
                Bukkit.getScheduler().cancelTask(pending.taskId());
                pendingDisbands.remove(player.getUniqueId());
            }
            return ConfirmDisbandResult.error("guild.disband.error_expired", PlaceholderContext.of(player));
        }
        Bukkit.getScheduler().cancelTask(pending.taskId());
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null || !data.getGuildId().equals(pending.guildId)) {
                return ConfirmDisbandResult.error("guild.disband.error_changed", PlaceholderContext.of(player));
            }
            Optional<Guild> guildOpt = context.guildDao.findById(pending.guildId);
            if (guildOpt.isEmpty()) {
                return ConfirmDisbandResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild guild = guildOpt.get();
            if (data.getRank() != GuildRank.LEADER) {
                return ConfirmDisbandResult.error("guild.disband.error_no_permission", PlaceholderContext.of(player));
            }

            UUID guildId = guild.getId();
            long now = TimeUtil.currentEpochSeconds();

            List<CostItem> refund = calculateRefund();
            List<PlayerData> members = clearGuildMembersData(guildId, now);
            deleteGuildData(guildId);
            archiveGuild(guild, now, player.getUniqueId());

            PlaceholderContext ctx = PlaceholderContext.empty()
                    .with("player", player.getName())
                    .with("guild", guild.getName())
                    .with("tag", guild.getTag());
            Component broadcast = context.langManager.getMessage("guild.disband.broadcast", MessageType.INFO, ctx);
            return new ConfirmDisbandResult(true, guild, refund, members, broadcast, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to disband guild: " + e.getMessage());
            return ConfirmDisbandResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applyConfirmDisbandEffects(Player player, ConfirmDisbandResult result) {
        if (!result.success()) {
            pendingDisbands.remove(player.getUniqueId());
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild guild = result.guild();
        UUID guildId = guild.getId();
        pendingDisbands.remove(player.getUniqueId());

        Optional<CuboidData> cuboidOpt = context.cuboidManager.findCuboidByGuild(guildId);
        cuboidOpt.ifPresent(this::removeGuildEgg);
        context.cuboidManager.deleteCuboid(guildId);

        context.tagManager.updateAllTags();

        if (!result.refund().isEmpty()) {
            context.giveItems(player, result.refund());
            context.send(player, "guild.disband.refund", MessageType.INFO,
                    PlaceholderContext.of(player)
                            .with("items", context.formatCostItems(result.refund()))
                            .with("percentage", (int) (Math.min(1.0, Math.max(0.0, context.guildConfig.getDouble("disband.refund.percentage", 0.5))) * 100)));
        }

        for (PlayerData member : result.members()) {
            context.getPlayerGuildCache().remove(member.getUuid());
            Player online = Bukkit.getPlayer(member.getUuid());
            if (online != null) {
                context.tagManager.clearPlayerTag(online);
                context.send(online, "guild.disband.member_message", MessageType.INFO,
                        PlaceholderContext.of(online).with("guild", ""));
            }
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {
                online.sendMessage(result.broadcast());
            }
        }
        context.send(player, "guild.disband.success", MessageType.SUCCESS,
                PlaceholderContext.empty().with("player", player.getName()).with("guild", guild.getName()).with("tag", guild.getTag()));
    }

    public CompletableFuture<ConfirmDisbandResult> confirmDisbandAsync(Player player) {
        return context.databaseExecutor.supplyAsync(() -> prepareConfirmDisbandData(player));
    }

    public record ConfirmDisbandResult(boolean success, Guild guild, List<CostItem> refund,
                                        List<PlayerData> members, Component broadcast,
                                        String errorKey, PlaceholderContext errorContext) {
        public static ConfirmDisbandResult error(String key, PlaceholderContext context) {
            return new ConfirmDisbandResult(false, null, List.of(), List.of(), null, key, context);
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
                if (current == null || current.expiresAt < TimeUtil.currentEpochSeconds()) {
                    this.cancel();
                    return;
                }
                long remaining = current.expiresAt - TimeUtil.currentEpochSeconds();
                Component message = context.langManager.getMessageWithoutPrefix("guild.disband.action_bar",
                        PlaceholderContext.of(player).with("seconds", remaining));
                player.sendActionBar(message);
            }
        };
        return runnable.runTaskTimer(context.plugin, 0L, 20L).getTaskId();
    }

    private List<CostItem> calculateRefund() {
        boolean refundEnabled = context.guildConfig.getBoolean("disband.refund.enabled", false);
        if (!refundEnabled) {
            return List.of();
        }
        double percentage = Math.max(0.0, Math.min(1.0, context.guildConfig.getDouble("disband.refund.percentage", 0.5)));
        List<CostItem> foundationCost = context.loadFoundationCostItems();
        if (foundationCost.isEmpty()) {
            return List.of();
        }
        return foundationCost.stream()
                .map(item -> new CostItem(item.material(), (int) Math.floor(item.amount() * percentage)))
                .filter(item -> item.amount() > 0)
                .toList();
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

    private List<PlayerData> clearGuildMembersData(UUID guildId, long now) throws SQLException {
        List<PlayerData> members = context.playerDao.findByGuild(guildId);
        for (PlayerData member : members) {
            member.setGuildId(null);
            member.setRank(null);
            member.setJoinedGuildAt(0);
            member.setLeftGuildAt(now);
            context.playerDao.update(member);
        }
        return members;
    }

    private void deleteGuildData(UUID guildId) throws SQLException {
        context.guildInviteDao.deleteByGuild(guildId);
        context.guildJoinRequestDao.deleteByGuild(guildId);
        context.guildBanDao.deleteByGuild(guildId);
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
