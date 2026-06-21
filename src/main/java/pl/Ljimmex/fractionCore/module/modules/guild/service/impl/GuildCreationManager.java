package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildCreateResult;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.Ljimmex.fractionCore.database.entity.CuboidData;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class GuildCreationManager {

    private static final long CONFIRMATION_TIMEOUT_MS = 60_000;

    private final GuildContext context;
    private final java.util.Map<UUID, PendingCreateRequest> pendingRequests = new java.util.concurrent.ConcurrentHashMap<>();

    public GuildCreationManager(GuildContext context) {
        this.context = context;
    }

    public GuildCreateResult prepareCreation(Player founder, String rawName, String rawTag) {
        ValidationResult validation = validateAll(founder, rawName, rawTag);
        context.plugin.getLogger().info("[FractionCore] prepareCreation validation result for " + founder.getName() + ": " + validation.result());
        if (validation.result() != GuildCreateResult.SUCCESS) {
            return validation.result();
        }

        pendingRequests.put(founder.getUniqueId(), new PendingCreateRequest(
                validation.name(), validation.tag(), System.currentTimeMillis()
        ));
        return GuildCreateResult.SUCCESS;
    }

    public GuildCreateResult confirmCreation(Player founder) {
        PendingCreateRequest request = pendingRequests.remove(founder.getUniqueId());
        if (request == null || System.currentTimeMillis() - request.createdAt() > CONFIRMATION_TIMEOUT_MS) {
            return GuildCreateResult.NO_PENDING_REQUEST;
        }

        ValidationResult validation = validateAll(founder, request.name(), request.tag());
        if (validation.result() != GuildCreateResult.SUCCESS) {
            return validation.result();
        }

        return executeCreation(founder, validation.name(), validation.tag());
    }

    public Component buildPreview(Player founder, String rawName, String rawTag) {
        ValidationResult validation = validateAll(founder, rawName, rawTag);
        String name = validation.name();
        String tag = validation.tag();

        Component preview = Component.text("=== PODSUMOWANIE ZAKLADANIA GILDII ===").color(NamedTextColor.GOLD);
        preview = preview.append(Component.newline());
        preview = preview.append(Component.text("Nazwa: ").color(NamedTextColor.GRAY))
                .append(Component.text(name).color(NamedTextColor.AQUA));
        preview = preview.append(Component.newline());
        preview = preview.append(Component.text("Tag: ").color(NamedTextColor.GRAY))
                .append(parseTag(tag));
        preview = preview.append(Component.newline());

        Component status;
        if (validation.result() == GuildCreateResult.SUCCESS) {
            status = Component.text("Wszystko jest git. Wpisz /guild create confirm, aby potwierdzic.").color(NamedTextColor.GREEN);
        } else {
            status = Component.text("Blad: " + translateResult(validation.result())).color(NamedTextColor.RED);
        }
        preview = preview.append(status);

        List<CostItem> costItems = loadCostItems();
        if (!costItems.isEmpty()) {
            preview = preview.append(Component.newline())
                    .append(Component.text("Koszt:").color(NamedTextColor.YELLOW));
            for (CostItem costItem : costItems) {
                int available = founder.getInventory().all(costItem.material()).values().stream()
                        .mapToInt(ItemStack::getAmount).sum();
                NamedTextColor amountColor = available >= costItem.amount() ? NamedTextColor.GREEN : NamedTextColor.RED;
                preview = preview.append(Component.newline())
                        .append(Component.text("- " + costItem.material().name() + ": ").color(NamedTextColor.GRAY))
                        .append(Component.text(available + "/" + costItem.amount()).color(amountColor));
            }
        }

        return preview;
    }

    private ValidationResult validateAll(Player founder, String rawName, String rawTag) {
        String name = rawName.trim();
        String tag = rawTag.trim().toUpperCase();

        int minName = context.guildConfig.getInt("foundation.min-name-length", 3);
        int maxName = context.guildConfig.getInt("foundation.max-name-length", 24);
        if (name.length() < minName) {
            return new ValidationResult(GuildCreateResult.NAME_TOO_SHORT, name, tag);
        }
        if (name.length() > maxName) {
            return new ValidationResult(GuildCreateResult.NAME_TOO_LONG, name, tag);
        }
        String namePattern = context.guildConfig.getString("foundation.name-pattern", "[A-Za-z0-9_ ]+");
        if (!Pattern.matches(namePattern, name)) {
            return new ValidationResult(GuildCreateResult.INVALID_NAME, name, tag);
        }

        int minTag = context.guildConfig.getInt("foundation.min-tag-length", 2);
        int maxTag = context.guildConfig.getInt("foundation.max-tag-length", 6);
        if (tag.length() < minTag) {
            return new ValidationResult(GuildCreateResult.TAG_TOO_SHORT, name, tag);
        }
        if (tag.length() > maxTag) {
            return new ValidationResult(GuildCreateResult.TAG_TOO_LONG, name, tag);
        }
        String tagPattern = context.guildConfig.getString("foundation.tag-pattern", "[A-Za-z0-9]+");
        if (!Pattern.matches(tagPattern, tag)) {
            return new ValidationResult(GuildCreateResult.INVALID_TAG, name, tag);
        }

        GuildCreateResult locationResult = validateLocation(founder);
        if (locationResult != GuildCreateResult.SUCCESS) {
            return new ValidationResult(locationResult, name, tag);
        }

        GuildCreateResult requirementsResult = validateRequirements(founder);
        if (requirementsResult != GuildCreateResult.SUCCESS) {
            return new ValidationResult(requirementsResult, name, tag);
        }

        List<CostItem> costItems = loadCostItems();
        if (!context.hasItems(founder, costItems)) {
            return new ValidationResult(GuildCreateResult.INSUFFICIENT_ITEMS, name, tag);
        }

        try {
            if (context.guildDao.existsByName(name)) {
                return new ValidationResult(GuildCreateResult.NAME_TAKEN, name, tag);
            }
            if (context.guildDao.existsByTag(tag)) {
                return new ValidationResult(GuildCreateResult.TAG_TAKEN, name, tag);
            }
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to check guild uniqueness: " + e.getMessage());
            return new ValidationResult(GuildCreateResult.ERROR, name, tag);
        }

        return new ValidationResult(GuildCreateResult.SUCCESS, name, tag);
    }

    private GuildCreateResult executeCreation(Player founder, String name, String tag) {
        List<CostItem> costItems = loadCostItems();
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
            long now = System.currentTimeMillis() / 1000;

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
            CuboidData cuboid = new CuboidData(
                    guildId,
                    world.getName(),
                    center.getBlockX(),
                    center.getBlockY(),
                    center.getBlockZ(),
                    startingRadius,
                    1
            );
            context.cuboidDao.save(cuboid);

            createEggCube(center);
            founder.teleport(home);

            context.tagManager.updatePlayerTag(founder);

            if (context.guildConfig.getBoolean("foundation.announce-globally", false)) {
                Component announcement = Component.text("Gildia ").color(NamedTextColor.YELLOW)
                        .append(Component.text(name).color(NamedTextColor.AQUA))
                        .append(Component.text(" [" + tag + "] zostala zalozona przez ").color(NamedTextColor.YELLOW))
                        .append(Component.text(founder.getName()).color(NamedTextColor.AQUA))
                        .append(Component.text("!").color(NamedTextColor.YELLOW));
                context.plugin.getServer().broadcast(announcement);
            }

            return GuildCreateResult.SUCCESS;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to create guild: " + e.getMessage());
            e.printStackTrace();
            return GuildCreateResult.ERROR;
        }
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

    private List<CostItem> loadCostItems() {
        return context.loadCostItemsAtPath("foundation-cost.items");
    }

    private GuildCreateResult validateLocation(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return GuildCreateResult.INVALID_LOCATION;
        }

        List<String> blockedWorlds = context.guildConfig.getStringList("foundation-requirements.blocked-worlds");
        if (blockedWorlds.contains(world.getName())) {
            return GuildCreateResult.BLOCKED_WORLD;
        }

        Location spawn = world.getSpawnLocation();
        int minDistanceFromSpawn = context.guildConfig.getInt("foundation.min-distance-from-spawn", 200);
        if (location.distance(spawn) < minDistanceFromSpawn) {
            return GuildCreateResult.TOO_CLOSE_TO_SPAWN;
        }

        try {
            int startingRadius = context.guildConfig.getInt("foundation.starting-radius", 16);
            int buffer = context.guildConfig.getInt("foundation.min-distance-between-guilds", 5);
            for (CuboidData other : context.cuboidDao.findAll()) {
                if (!other.getWorld().equalsIgnoreCase(world.getName())) {
                    continue;
                }
                double dx = location.getBlockX() - other.getCenterX();
                double dz = location.getBlockZ() - other.getCenterZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                double required = startingRadius + other.getRadius() + buffer;
                if (distance < required) {
                    return GuildCreateResult.TOO_CLOSE_TO_OTHER_GUILD;
                }
            }
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to validate guild location: " + e.getMessage());
            return GuildCreateResult.ERROR;
        }

        return GuildCreateResult.SUCCESS;
    }

    private GuildCreateResult validateRequirements(Player player) {
        try {
            Optional<PlayerData> data = context.playerDao.findByUuid(player.getUniqueId());

            if (data.isPresent() && data.get().getGuildId() != null) {
                return GuildCreateResult.ALREADY_IN_GUILD;
            }

            int cooldownMinutes = context.guildConfig.getInt("foundation-requirements.cooldown-minutes", 2880);
            if (cooldownMinutes > 0 && data.isPresent()) {
                long leftAt = data.get().getLeftGuildAt();
                if (leftAt > 0) {
                    long elapsedMinutes = (System.currentTimeMillis() / 1000 - leftAt) / 60;
                    if (elapsedMinutes < cooldownMinutes) {
                        return GuildCreateResult.COOLDOWN;
                    }
                }
            }

            return GuildCreateResult.SUCCESS;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to validate foundation requirements: " + e.getMessage());
            return GuildCreateResult.ERROR;
        }
    }

    private Component parseTag(String tag) {
        String color = context.guildConfig.getString("relation-colors.neutral", "<white>");
        String full = color + "[" + tag + "] ";
        if (full.contains("&") || full.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(full);
        }
        try {
            return MiniMessage.miniMessage().deserialize(full);
        } catch (Exception e) {
            return Component.text("[" + tag + "] ");
        }
    }

    public Component getResultMessage(Player player, GuildCreateResult result, String rawName, String rawTag) {
        String name = rawName != null ? rawName.trim() : "";
        String tag = rawTag != null ? rawTag.trim().toUpperCase() : "";
        context.plugin.getLogger().info("[FractionCore] getResultMessage: result=" + result + ", name='" + name + "', tag='" + tag + "'");
        PlaceholderContext ctx = PlaceholderContext.of(player);
        String key = "guild.create.error_generic";

        switch (result) {
            case SUCCESS -> {
                ctx.with("name", name).with("tag", tag);
                key = "guild.create.success";
            }
            case NAME_TOO_SHORT -> {
                int min = context.guildConfig.getInt("foundation.min-name-length", 3);
                ctx.with("min", min).with("current", name.length());
                key = "guild.create.error_name_too_short";
            }
            case NAME_TOO_LONG -> {
                int max = context.guildConfig.getInt("foundation.max-name-length", 24);
                ctx.with("max", max).with("current", name.length());
                key = "guild.create.error_name_too_long";
            }
            case INVALID_NAME -> {
                ctx.with("name", name);
                key = "guild.create.error_name_invalid";
            }
            case TAG_TOO_SHORT -> {
                int min = context.guildConfig.getInt("foundation.min-tag-length", 2);
                ctx.with("min", min).with("current", tag.length());
                key = "guild.create.error_tag_too_short";
            }
            case TAG_TOO_LONG -> {
                int max = context.guildConfig.getInt("foundation.max-tag-length", 6);
                ctx.with("max", max).with("current", tag.length());
                key = "guild.create.error_tag_too_long";
            }
            case INVALID_TAG -> {
                ctx.with("tag", tag);
                key = "guild.create.error_tag_invalid";
            }
            case NAME_TAKEN -> {
                ctx.with("name", name);
                key = "guild.create.error_name_taken";
            }
            case TAG_TAKEN -> {
                ctx.with("tag", tag);
                key = "guild.create.error_tag_taken";
            }
            case INSUFFICIENT_ITEMS -> key = "guild.create.error_insufficient_items";
            case ALREADY_IN_GUILD -> key = "guild.create.error_already_in_guild";
            case COOLDOWN -> {
                int minutes = context.guildConfig.getInt("foundation-requirements.cooldown-minutes", 2880);
                ctx.with("minutes", minutes);
                key = "guild.create.error_cooldown";
            }
            case BLOCKED_WORLD -> {
                ctx.with("world", player.getWorld().getName());
                key = "guild.create.error_blocked_world";
            }
            case TOO_CLOSE_TO_SPAWN -> {
                int required = context.guildConfig.getInt("foundation.min-distance-from-spawn", 200);
                double current = player.getLocation().distance(player.getWorld().getSpawnLocation());
                ctx.with("required", required)
                        .with("current", String.format(Locale.US, "%.1f", current))
                        .with("remaining", Math.max(0, (int) Math.ceil(required - current)));
                key = "guild.create.error_too_close_to_spawn";
            }
            case TOO_CLOSE_TO_OTHER_GUILD -> {
                int startingRadius = context.guildConfig.getInt("foundation.starting-radius", 16);
                int buffer = context.guildConfig.getInt("foundation.min-distance-between-guilds", 5);
                double current = Double.MAX_VALUE;
                int required = 0;
                try {
                    for (CuboidData other : context.cuboidDao.findAll()) {
                        if (!other.getWorld().equalsIgnoreCase(player.getWorld().getName())) {
                            continue;
                        }
                        double dx = player.getLocation().getBlockX() - other.getCenterX();
                        double dz = player.getLocation().getBlockZ() - other.getCenterZ();
                        double distance = Math.sqrt(dx * dx + dz * dz);
                        double needed = startingRadius + other.getRadius() + buffer;
                        if (distance < current) {
                            current = distance;
                            required = (int) needed;
                        }
                    }
                } catch (SQLException e) {
                    context.plugin.getLogger().severe("Failed to compute distance to nearest guild: " + e.getMessage());
                }
                if (current == Double.MAX_VALUE) {
                    current = 0;
                }
                ctx.with("required", required)
                        .with("current", String.format(Locale.US, "%.1f", current))
                        .with("remaining", Math.max(0, (int) Math.ceil(required - current)));
                key = "guild.create.error_too_close_to_other_guild";
            }
            case INVALID_LOCATION -> key = "guild.create.error_invalid_location";
            case NO_PENDING_REQUEST -> key = "guild.create.error_no_pending_request";
            case ERROR -> key = "guild.create.error_generic";
        }

        context.plugin.getLogger().info("[FractionCore] getResultMessage resolved key: " + key);
        String rawMessage = context.langManager.getRawMessage(key);
        if (rawMessage == null) {
            context.plugin.getLogger().warning("[FractionCore] Missing lang key: " + key);
            return Component.text("[Brak tlumaczenia: " + key + "]").color(NamedTextColor.RED);
        }
        return context.langManager.getMessageWithoutPrefix(key, ctx);
    }

    private String translateResult(GuildCreateResult result) {
        return switch (result) {
            case SUCCESS -> "OK";
            case INVALID_NAME -> "nieprawidlowa nazwa";
            case NAME_TOO_SHORT -> "za krotka nazwa";
            case NAME_TOO_LONG -> "za dluga nazwa";
            case INVALID_TAG -> "nieprawidlowy tag";
            case TAG_TOO_SHORT -> "za krotki tag";
            case TAG_TOO_LONG -> "za dlugi tag";
            case NAME_TAKEN -> "nazwa zajeta";
            case TAG_TAKEN -> "tag zajety";
            case INSUFFICIENT_ITEMS -> "brak itemow";
            case ALREADY_IN_GUILD -> "nalezysz juz do gildii";
            case COOLDOWN -> "cooldown";
            case BLOCKED_WORLD -> "zablokowany swiat";
            case TOO_CLOSE_TO_SPAWN -> "za blisko spawnu";
            case TOO_CLOSE_TO_OTHER_GUILD -> "za blisko innej gildii";
            case INVALID_LOCATION -> "nieprawidlowa lokalizacja";
            case ERROR -> "blad systemu";
            case NO_PENDING_REQUEST -> "brak oczekujacego zadania";
        };
    }

    private record ValidationResult(GuildCreateResult result, String name, String tag) {
    }

    private record PendingCreateRequest(String name, String tag, long createdAt) {
    }
}
