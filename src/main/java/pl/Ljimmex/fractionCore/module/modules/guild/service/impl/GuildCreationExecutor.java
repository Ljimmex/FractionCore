package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildCreateResult;
import pl.Ljimmex.fractionCore.util.TimeUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildCreationExecutor {

    private final GuildContext context;
    private final GuildCreationHelper helper;

    public GuildCreationExecutor(GuildContext context) {
        this.context = context;
        this.helper = new GuildCreationHelper(context);
    }

    public GuildCreateResult executeCreation(Player founder, String name, String tag) {
        List<CostItem> costItems = helper.loadCostItems();
        if (!context.deductItems(founder, costItems)) {
            return GuildCreateResult.INSUFFICIENT_ITEMS;
        }

        try {
            World world = founder.getWorld();
            if (world == null) {
                return GuildCreateResult.INVALID_LOCATION;
            }

            int eggCenterY = context.guildConfig.getInt("foundation.egg-center-y", 20);
            Location center = new Location(
                    world,
                    founder.getLocation().getBlockX(),
                    eggCenterY,
                    founder.getLocation().getBlockZ()
            );
            long now = TimeUtil.currentEpochSeconds();

            UUID guildId = UUID.randomUUID();
            String defaultColor = context.guildConfig.getString("relation-colors.neutral", "<white>");
            Guild guild = new Guild(guildId, name, tag, defaultColor, founder.getUniqueId(), 0, 1, now);
            Location home = center.clone().add(0.5, 1, 0.5);
            guild.setHomeWorld(world.getName());
            guild.setHomeX(home.getX());
            guild.setHomeY(home.getY());
            guild.setHomeZ(home.getZ());
            guild.setPublic(context.guildConfig.getBoolean("settings.flags.is-public", true));
            guild.setAllowJoinRequests(context.guildConfig.getBoolean("settings.flags.allow-join-requests", false));
            guild.setShowHome(context.guildConfig.getBoolean("settings.flags.show-home", false));
            context.guildDao.save(guild);

            Optional<PlayerData> existing = context.playerDao.findByUuid(founder.getUniqueId());
            PlayerData founderData = existing.orElseGet(() -> new PlayerData(
                    founder.getUniqueId(),
                    founder.getName(),
                    null,
                    null,
                    0, 0, 0, 1000, 0, 0
            ));
            founderData.setName(founder.getName());
            founderData.setGuildId(guildId);
            founderData.setRank(GuildRank.LEADER);
            founderData.setJoinedGuildAt(now);
            founderData.setLeftGuildAt(0);
            if (existing.isPresent()) {
                context.playerDao.update(founderData);
            } else {
                context.playerDao.save(founderData);
            }

            int startingRadius = context.guildConfig.getInt("foundation.starting-radius", 16);
            context.cuboidManager.createDefaultCuboid(guildId, world, center, startingRadius);

            createEggCube(center);
            founder.teleport(home);

            context.tagManager.updatePlayerTag(founder);

            if (context.guildConfig.getBoolean("foundation.announce-globally", false)) {
                PlaceholderContext announceCtx = PlaceholderContext.of(founder)
                        .with("guild", name)
                        .with("tag", tag)
                        .with("player", founder.getName());
                Component announcement = context.langManager.getMessageWithoutPrefix("guild.create.announcement", announceCtx);
                context.plugin.getServer().broadcast(announcement);
            }

            return GuildCreateResult.SUCCESS;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to create guild: " + e.getMessage());
            context.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Full stack trace", e);
            return GuildCreateResult.ERROR;
        }
    }

    public CompletableFuture<GuildCreateResult> executeCreationAsync(Player founder, String name, String tag) {
        List<CostItem> costItems = helper.loadCostItems();
        if (!context.deductItems(founder, costItems)) {
            return CompletableFuture.completedFuture(GuildCreateResult.INSUFFICIENT_ITEMS);
        }

        World world = founder.getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(GuildCreateResult.INVALID_LOCATION);
        }

        int eggCenterY = context.guildConfig.getInt("foundation.egg-center-y", 20);
        Location center = new Location(
                world,
                founder.getLocation().getBlockX(),
                eggCenterY,
                founder.getLocation().getBlockZ()
        );
        Location home = center.clone().add(0.5, 1, 0.5);
        long now = TimeUtil.currentEpochSeconds();

        UUID founderUuid = founder.getUniqueId();
        String founderName = founder.getName();
        String worldName = world.getName();
        int startingRadius = context.guildConfig.getInt("foundation.starting-radius", 16);
        String defaultColor = context.guildConfig.getString("relation-colors.neutral", "<white>");
        boolean announceGlobally = context.guildConfig.getBoolean("foundation.announce-globally", false);

        return context.databaseExecutor.supplyAsync(() -> {
            try {
                UUID guildId = UUID.randomUUID();
                Guild guild = new Guild(guildId, name, tag, defaultColor, founderUuid, 0, 1, now);
                guild.setHomeWorld(worldName);
                guild.setHomeX(home.getX());
                guild.setHomeY(home.getY());
                guild.setHomeZ(home.getZ());
                guild.setPublic(context.guildConfig.getBoolean("settings.flags.is-public", true));
                guild.setAllowJoinRequests(context.guildConfig.getBoolean("settings.flags.allow-join-requests", false));
                guild.setShowHome(context.guildConfig.getBoolean("settings.flags.show-home", false));
                context.guildDao.save(guild);

                Optional<PlayerData> existing = context.playerDao.findByUuid(founderUuid);
                PlayerData founderData = existing.orElseGet(() -> new PlayerData(
                        founderUuid,
                        founderName,
                        null,
                        null,
                        0, 0, 0, 1000, 0, 0
                ));
                founderData.setName(founderName);
                founderData.setGuildId(guildId);
                founderData.setRank(GuildRank.LEADER);
                founderData.setJoinedGuildAt(now);
                founderData.setLeftGuildAt(0);
                if (existing.isPresent()) {
                    context.playerDao.update(founderData);
                } else {
                    context.playerDao.save(founderData);
                }

                context.cuboidManager.createDefaultCuboid(guildId, world, center, startingRadius);

                return guildId;
            } catch (SQLException e) {
                context.plugin.getLogger().severe("Failed to create guild: " + e.getMessage());
                context.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Full stack trace", e);
                throw new RuntimeException(e);
            }
        }).thenCompose(guildId -> {
            CompletableFuture<GuildCreateResult> syncFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                try {
                    createEggCube(center);
                    founder.teleport(home);
                    context.tagManager.updatePlayerTag(founder);

                    if (announceGlobally) {
                        PlaceholderContext announceCtx = PlaceholderContext.of(founder)
                                .with("guild", name)
                                .with("tag", tag)
                                .with("player", founderName);
                        Component announcement = context.langManager.getMessageWithoutPrefix("guild.create.announcement", announceCtx);
                        context.plugin.getServer().broadcast(announcement);
                    }

                    syncFuture.complete(GuildCreateResult.SUCCESS);
                } catch (Throwable t) {
                    syncFuture.completeExceptionally(t);
                }
            });
            return syncFuture;
        }).exceptionally(ex -> {
            Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
            if (cause instanceof SQLException) {
                return GuildCreateResult.ERROR;
            }
            context.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unexpected error during guild creation", cause);
            return GuildCreateResult.ERROR;
        });
    }

    private void createEggCube(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        Block support = world.getBlockAt(cx, cy - 1, cz);
        if (support.getType().isAir() || support.isLiquid()) {
            support.setType(Material.STONE_BRICKS);
        }

        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int y = cy; y <= cy + 4; y++) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }

        Material eggMaterial = resolveEggBlock();
        center.getBlock().setType(eggMaterial);
    }

    private Material resolveEggBlock() {
        String blockName = context.guildConfig.getString("foundation.egg-block", "DRAGON_EGG");
        try {
            return Material.valueOf(blockName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.DRAGON_EGG;
        }
    }
}
