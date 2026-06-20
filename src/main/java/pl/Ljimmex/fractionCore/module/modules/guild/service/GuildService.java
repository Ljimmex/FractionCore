package pl.Ljimmex.fractionCore.module.modules.guild.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.database.dao.CuboidDao;
import pl.Ljimmex.fractionCore.database.dao.GuildBanDao;
import pl.Ljimmex.fractionCore.database.dao.GuildInviteDao;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.database.entity.CuboidData;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildBan;
import pl.Ljimmex.fractionCore.database.entity.GuildInvite;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class GuildService {

    private static final long CONFIRMATION_TIMEOUT_MS = 60_000;

    private final JavaPlugin plugin;
    private final GuildDao guildDao;
    private final PlayerDao playerDao;
    private final CuboidDao cuboidDao;
    private final GuildBanDao guildBanDao;
    private final GuildInviteDao guildInviteDao;
    private final FileConfiguration guildConfig;
    private final LangManager langManager;
    private final GuildTagManager tagManager;
    private final Map<UUID, PendingCreateRequest> pendingRequests = new ConcurrentHashMap<>();

    public GuildService(JavaPlugin plugin, GuildDao guildDao, PlayerDao playerDao, CuboidDao cuboidDao,
                        GuildBanDao guildBanDao, GuildInviteDao guildInviteDao,
                        FileConfiguration guildConfig, LangManager langManager) {
        this.plugin = plugin;
        this.guildDao = guildDao;
        this.playerDao = playerDao;
        this.cuboidDao = cuboidDao;
        this.guildBanDao = guildBanDao;
        this.guildInviteDao = guildInviteDao;
        this.guildConfig = guildConfig;
        this.langManager = langManager;
        this.tagManager = new GuildTagManager(playerDao, guildDao, guildConfig);
    }

    public GuildTagManager getTagManager() {
        return tagManager;
    }

    public PlayerDao getPlayerDao() {
        return playerDao;
    }

    public GuildDao getGuildDao() {
        return guildDao;
    }

    public FileConfiguration getGuildConfig() {
        return guildConfig;
    }

    public GuildCreateResult prepareCreation(Player founder, String rawName, String rawTag, String rawColor) {
        ValidationResult validation = validateAll(founder, rawName, rawTag, rawColor);
        plugin.getLogger().info("[FractionCore] prepareCreation validation result for " + founder.getName() + ": " + validation.result());
        if (validation.result() != GuildCreateResult.SUCCESS) {
            return validation.result();
        }

        pendingRequests.put(founder.getUniqueId(), new PendingCreateRequest(
                validation.name(), validation.tag(), validation.color(), System.currentTimeMillis()
        ));
        return GuildCreateResult.SUCCESS;
    }

    public GuildCreateResult confirmCreation(Player founder) {
        PendingCreateRequest request = pendingRequests.remove(founder.getUniqueId());
        if (request == null || System.currentTimeMillis() - request.createdAt() > CONFIRMATION_TIMEOUT_MS) {
            return GuildCreateResult.NO_PENDING_REQUEST;
        }

        ValidationResult validation = validateAll(founder, request.name(), request.tag(), request.color());
        if (validation.result() != GuildCreateResult.SUCCESS) {
            return validation.result();
        }

        return executeCreation(founder, validation.name(), validation.tag(), validation.color());
    }

    public Component buildPreview(Player founder, String rawName, String rawTag, String rawColor) {
        ValidationResult validation = validateAll(founder, rawName, rawTag, rawColor);
        String name = validation.name();
        String tag = validation.tag();
        String color = validation.color();

        Component preview = Component.text("=== PODSUMOWANIE ZAKLADANIA GILDII ===").color(NamedTextColor.GOLD);
        preview = preview.append(Component.newline());
        preview = preview.append(Component.text("Nazwa: ").color(NamedTextColor.GRAY))
                .append(Component.text(name).color(NamedTextColor.AQUA));
        preview = preview.append(Component.newline());
        preview = preview.append(Component.text("Tag: ").color(NamedTextColor.GRAY))
                .append(parseTag(tag, color));
        preview = preview.append(Component.newline());
        preview = preview.append(Component.text("Kolor: ").color(NamedTextColor.GRAY))
                .append(Component.text(color).color(NamedTextColor.AQUA));
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

    private ValidationResult validateAll(Player founder, String rawName, String rawTag, String rawColor) {
        String name = rawName.trim();
        String tag = rawTag.trim().toUpperCase();

        int minName = guildConfig.getInt("foundation.min-name-length", 3);
        int maxName = guildConfig.getInt("foundation.max-name-length", 24);
        if (name.length() < minName) {
            return new ValidationResult(GuildCreateResult.NAME_TOO_SHORT, name, tag, null);
        }
        if (name.length() > maxName) {
            return new ValidationResult(GuildCreateResult.NAME_TOO_LONG, name, tag, null);
        }
        String namePattern = guildConfig.getString("foundation.name-pattern", "[A-Za-z0-9_ ]+");
        if (!Pattern.matches(namePattern, name)) {
            return new ValidationResult(GuildCreateResult.INVALID_NAME, name, tag, null);
        }

        int minTag = guildConfig.getInt("foundation.min-tag-length", 2);
        int maxTag = guildConfig.getInt("foundation.max-tag-length", 6);
        if (tag.length() < minTag) {
            return new ValidationResult(GuildCreateResult.TAG_TOO_SHORT, name, tag, null);
        }
        if (tag.length() > maxTag) {
            return new ValidationResult(GuildCreateResult.TAG_TOO_LONG, name, tag, null);
        }
        String tagPattern = guildConfig.getString("foundation.tag-pattern", "[A-Za-z0-9]+");
        if (!Pattern.matches(tagPattern, tag)) {
            return new ValidationResult(GuildCreateResult.INVALID_TAG, name, tag, null);
        }

        String color = resolveColor(rawColor);

        GuildCreateResult locationResult = validateLocation(founder);
        if (locationResult != GuildCreateResult.SUCCESS) {
            return new ValidationResult(locationResult, name, tag, color);
        }

        GuildCreateResult requirementsResult = validateRequirements(founder);
        if (requirementsResult != GuildCreateResult.SUCCESS) {
            return new ValidationResult(requirementsResult, name, tag, color);
        }

        List<CostItem> costItems = loadCostItems();
        if (!hasItems(founder, costItems)) {
            return new ValidationResult(GuildCreateResult.INSUFFICIENT_ITEMS, name, tag, color);
        }

        try {
            if (guildDao.existsByName(name)) {
                return new ValidationResult(GuildCreateResult.NAME_TAKEN, name, tag, color);
            }
            if (guildDao.existsByTag(tag)) {
                return new ValidationResult(GuildCreateResult.TAG_TAKEN, name, tag, color);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check guild uniqueness: " + e.getMessage());
            return new ValidationResult(GuildCreateResult.ERROR, name, tag, color);
        }

        return new ValidationResult(GuildCreateResult.SUCCESS, name, tag, color);
    }

    private GuildCreateResult executeCreation(Player founder, String name, String tag, String color) {
        List<CostItem> costItems = loadCostItems();
        if (!deductItems(founder, costItems)) {
            return GuildCreateResult.INSUFFICIENT_ITEMS;
        }

        try {
            World world = founder.getWorld();
            if (world == null) {
                return GuildCreateResult.INVALID_LOCATION;
            }

            int eggCenterY = guildConfig.getInt("foundation.egg-center-y", 20);
            Location center = new Location(
                    world,
                    founder.getLocation().getBlockX(),
                    eggCenterY,
                    founder.getLocation().getBlockZ()
            );
            long now = System.currentTimeMillis() / 1000;

            UUID guildId = UUID.randomUUID();
            Guild guild = new Guild(guildId, name, tag, color, founder.getUniqueId(), 0, 1, now);
            Location home = center.clone().add(0.5, 1, 0.5);
            guild.setHomeWorld(world.getName());
            guild.setHomeX(home.getX());
            guild.setHomeY(home.getY());
            guild.setHomeZ(home.getZ());
            guildDao.save(guild);

            Optional<PlayerData> existing = playerDao.findByUuid(founder.getUniqueId());
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
                playerDao.update(founderData);
            } else {
                playerDao.save(founderData);
            }

            int startingRadius = guildConfig.getInt("foundation.starting-radius", 16);
            CuboidData cuboid = new CuboidData(
                    guildId,
                    world.getName(),
                    center.getBlockX(),
                    center.getBlockY(),
                    center.getBlockZ(),
                    startingRadius,
                    1
            );
            cuboidDao.save(cuboid);

            createEggCube(center);
            founder.teleport(home);

            tagManager.updatePlayerTag(founder);

            if (guildConfig.getBoolean("foundation.announce-globally", false)) {
                Component announcement = Component.text("Gildia ").color(NamedTextColor.YELLOW)
                        .append(Component.text(name).color(NamedTextColor.AQUA))
                        .append(Component.text(" [" + tag + "] zostala zalozona przez ").color(NamedTextColor.YELLOW))
                        .append(Component.text(founder.getName()).color(NamedTextColor.AQUA))
                        .append(Component.text("!").color(NamedTextColor.YELLOW));
                plugin.getServer().broadcast(announcement);
            }

            return GuildCreateResult.SUCCESS;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create guild: " + e.getMessage());
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

        // Ensure the egg has solid support directly below the room (outside the 6x6x6 cube).
        Block support = world.getBlockAt(cx, cy - 1, cz);
        if (support.getType().isAir() || support.isLiquid()) {
            support.setType(Material.STONE_BRICKS);
        }

        // Clear a 5x5x5 cube starting at Y = egg-center-y (inclusive), with the egg on the floor.
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

    private String resolveColor(String rawColor) {
        if (rawColor != null && !rawColor.isBlank()) {
            return rawColor.trim();
        }
        return guildConfig.getString("foundation.default-color", "<gray>");
    }

    private Material resolveEggBlock() {
        String blockName = guildConfig.getString("foundation.egg-block", "DRAGON_EGG");
        try {
            return Material.valueOf(blockName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.DRAGON_EGG;
        }
    }

    private GuildCreateResult validateLocation(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return GuildCreateResult.INVALID_LOCATION;
        }

        List<String> blockedWorlds = guildConfig.getStringList("foundation-requirements.blocked-worlds");
        if (blockedWorlds.contains(world.getName())) {
            return GuildCreateResult.BLOCKED_WORLD;
        }

        Location spawn = world.getSpawnLocation();
        int minDistanceFromSpawn = guildConfig.getInt("foundation.min-distance-from-spawn", 200);
        if (location.distance(spawn) < minDistanceFromSpawn) {
            return GuildCreateResult.TOO_CLOSE_TO_SPAWN;
        }

        try {
            int startingRadius = guildConfig.getInt("foundation.starting-radius", 16);
            int buffer = guildConfig.getInt("foundation.min-distance-between-guilds", 5);
            for (CuboidData other : cuboidDao.findAll()) {
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
            plugin.getLogger().severe("Failed to validate guild location: " + e.getMessage());
            return GuildCreateResult.ERROR;
        }

        return GuildCreateResult.SUCCESS;
    }

    private GuildCreateResult validateRequirements(Player player) {
        try {
            Optional<PlayerData> data = playerDao.findByUuid(player.getUniqueId());

            if (data.isPresent() && data.get().getGuildId() != null) {
                return GuildCreateResult.ALREADY_IN_GUILD;
            }

            int cooldownMinutes = guildConfig.getInt("foundation-requirements.cooldown-minutes", 2880);
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
            plugin.getLogger().severe("Failed to validate foundation requirements: " + e.getMessage());
            return GuildCreateResult.ERROR;
        }
    }

    private List<CostItem> loadCostItems() {
        return loadCostItemsAtPath("foundation-cost.items");
    }

    private List<CostItem> loadJoinCostItems() {
        return loadCostItemsAtPath("member-management.join-cost.items");
    }

    @SuppressWarnings("unchecked")
    private List<CostItem> loadCostItemsAtPath(String path) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) guildConfig.getList(path);
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(map -> {
                    String materialName = String.valueOf(map.get("material")).toUpperCase();
                    Object amountObj = map.get("amount");
                    int amount = amountObj instanceof Number number ? number.intValue() : 1;
                    Material material;
                    try {
                        material = Material.valueOf(materialName);
                    } catch (IllegalArgumentException e) {
                        material = Material.AIR;
                    }
                    return new CostItem(material, amount);
                })
                .filter(item -> item.material() != Material.AIR && item.amount() > 0)
                .toList();
    }

    private boolean hasItems(Player player, List<CostItem> costItems) {
        for (CostItem costItem : costItems) {
            if (!player.getInventory().containsAtLeast(new ItemStack(costItem.material()), costItem.amount())) {
                return false;
            }
        }
        return true;
    }

    private boolean deductItems(Player player, List<CostItem> costItems) {
        if (!hasItems(player, costItems)) {
            return false;
        }
        plugin.getLogger().info("[FractionCore] Deducting creation cost from " + player.getName() + ": " + costItems.stream()
                .map(costItem -> costItem.amount() + "x " + costItem.material())
                .toList());
        for (CostItem costItem : costItems) {
            int remaining = costItem.amount();
            ItemStack[] storage = player.getInventory().getStorageContents();
            for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
                ItemStack stack = storage[slot];
                if (stack == null || stack.getType() != costItem.material()) {
                    continue;
                }
                int remove = Math.min(remaining, stack.getAmount());
                int newAmount = stack.getAmount() - remove;
                if (newAmount <= 0) {
                    player.getInventory().setItem(slot, new ItemStack(Material.AIR));
                } else {
                    ItemStack newStack = stack.clone();
                    newStack.setAmount(newAmount);
                    player.getInventory().setItem(slot, newStack);
                }
                remaining -= remove;
            }
            if (remaining > 0) {
                ItemStack offHand = player.getInventory().getItemInOffHand();
                if (offHand.getType() == costItem.material()) {
                    int remove = Math.min(remaining, offHand.getAmount());
                    int newAmount = offHand.getAmount() - remove;
                    if (newAmount <= 0) {
                        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                    } else {
                        ItemStack newStack = offHand.clone();
                        newStack.setAmount(newAmount);
                        player.getInventory().setItemInOffHand(newStack);
                    }
                    remaining -= remove;
                }
            }
            plugin.getLogger().info("[FractionCore] After deducting " + costItem.material() + " remaining=" + remaining);
            if (remaining > 0) {
                return false;
            }
        }
        player.updateInventory();
        return true;
    }

    private Component parseTag(String tag, String color) {
        String full = color + "[" + tag + "] ";
        if (color.contains("&") || color.contains("§")) {
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
        plugin.getLogger().info("[FractionCore] getResultMessage: result=" + result + ", name='" + name + "', tag='" + tag + "'");
        PlaceholderContext context = PlaceholderContext.of(player);
        String key = "guild.create.error_generic";

        switch (result) {
            case SUCCESS -> {
                context.with("name", name).with("tag", tag);
                key = "guild.create.success";
            }
            case NAME_TOO_SHORT -> {
                int min = guildConfig.getInt("foundation.min-name-length", 3);
                context.with("min", min).with("current", name.length());
                key = "guild.create.error_name_too_short";
            }
            case NAME_TOO_LONG -> {
                int max = guildConfig.getInt("foundation.max-name-length", 24);
                context.with("max", max).with("current", name.length());
                key = "guild.create.error_name_too_long";
            }
            case INVALID_NAME -> {
                context.with("name", name);
                key = "guild.create.error_name_invalid";
            }
            case TAG_TOO_SHORT -> {
                int min = guildConfig.getInt("foundation.min-tag-length", 2);
                context.with("min", min).with("current", tag.length());
                key = "guild.create.error_tag_too_short";
            }
            case TAG_TOO_LONG -> {
                int max = guildConfig.getInt("foundation.max-tag-length", 6);
                context.with("max", max).with("current", tag.length());
                key = "guild.create.error_tag_too_long";
            }
            case INVALID_TAG -> {
                context.with("tag", tag);
                key = "guild.create.error_tag_invalid";
            }
            case NAME_TAKEN -> {
                context.with("name", name);
                key = "guild.create.error_name_taken";
            }
            case TAG_TAKEN -> {
                context.with("tag", tag);
                key = "guild.create.error_tag_taken";
            }
            case INSUFFICIENT_ITEMS -> key = "guild.create.error_insufficient_items";
            case ALREADY_IN_GUILD -> key = "guild.create.error_already_in_guild";
            case COOLDOWN -> {
                int minutes = guildConfig.getInt("foundation-requirements.cooldown-minutes", 2880);
                context.with("minutes", minutes);
                key = "guild.create.error_cooldown";
            }
            case BLOCKED_WORLD -> {
                context.with("world", player.getWorld().getName());
                key = "guild.create.error_blocked_world";
            }
            case TOO_CLOSE_TO_SPAWN -> {
                int required = guildConfig.getInt("foundation.min-distance-from-spawn", 200);
                double current = player.getLocation().distance(player.getWorld().getSpawnLocation());
                context.with("required", required)
                        .with("current", String.format(Locale.US, "%.1f", current))
                        .with("remaining", Math.max(0, (int) Math.ceil(required - current)));
                key = "guild.create.error_too_close_to_spawn";
            }
            case TOO_CLOSE_TO_OTHER_GUILD -> {
                int startingRadius = guildConfig.getInt("foundation.starting-radius", 16);
                int buffer = guildConfig.getInt("foundation.min-distance-between-guilds", 5);
                double current = Double.MAX_VALUE;
                int required = 0;
                try {
                    for (CuboidData other : cuboidDao.findAll()) {
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
                    plugin.getLogger().severe("Failed to compute distance to nearest guild: " + e.getMessage());
                }
                if (current == Double.MAX_VALUE) {
                    current = 0;
                }
                context.with("required", required)
                        .with("current", String.format(Locale.US, "%.1f", current))
                        .with("remaining", Math.max(0, (int) Math.ceil(required - current)));
                key = "guild.create.error_too_close_to_other_guild";
            }
            case INVALID_LOCATION -> key = "guild.create.error_invalid_location";
            case NO_PENDING_REQUEST -> key = "guild.create.error_no_pending_request";
            case ERROR -> key = "guild.create.error_generic";
        }

        plugin.getLogger().info("[FractionCore] getResultMessage resolved key: " + key);
        String rawMessage = langManager.getRawMessage(key);
        if (rawMessage == null) {
            plugin.getLogger().warning("[FractionCore] Missing lang key: " + key);
            return Component.text("[Brak tlumaczenia: " + key + "]").color(NamedTextColor.RED);
        }
        return langManager.getMessageWithoutPrefix(key, context);
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

    private record CostItem(Material material, int amount) {
    }

    private record ValidationResult(GuildCreateResult result, String name, String tag, String color) {
    }

    private record PendingCreateRequest(String name, String tag, String color, long createdAt) {
    }

    // ============================================================
    // Member management
    // ============================================================

    public boolean invitePlayer(Player inviter, Player target) {
        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            send(inviter, "guild.invite.error_self", MessageType.ERROR, PlaceholderContext.of(inviter));
            return false;
        }
        try {
            PlayerData inviterData = getOrCreatePlayerData(inviter);
            if (inviterData.getGuildId() == null) {
                send(inviter, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            GuildRank inviterRank = inviterData.getRank();
            if (!isModeratorOrHigher(inviterRank)) {
                send(inviter, "guild.invite.error_no_permission", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            UUID guildId = inviterData.getGuildId();
            Optional<Guild> guildOpt = guildDao.findById(guildId);
            if (guildOpt.isEmpty()) {
                send(inviter, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            Guild guild = guildOpt.get();

            PlayerData targetData = getOrCreatePlayerData(target);
            if (targetData.getGuildId() != null) {
                if (targetData.getGuildId().equals(guildId)) {
                    send(inviter, "guild.invite.error_already_member", MessageType.ERROR,
                            PlaceholderContext.of(inviter).with("player", target.getName()));
                } else {
                    send(inviter, "guild.invite.error_has_guild", MessageType.ERROR,
                            PlaceholderContext.of(inviter).with("player", target.getName()));
                }
                return false;
            }
            if (guildBanDao.exists(guildId, target.getUniqueId())) {
                send(inviter, "guild.invite.error_banned", MessageType.ERROR,
                        PlaceholderContext.of(inviter).with("player", target.getName()));
                return false;
            }
            int maxMembers = guildConfig.getInt("member-management.max-members", 10);
            if (getMemberCount(guildId) >= maxMembers) {
                send(inviter, "guild.invite.error_max_members", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            int maxInvites = guildConfig.getInt("member-management.max-invites-per-guild", 5);
            if (guildInviteDao.countByGuild(guildId) >= maxInvites) {
                send(inviter, "guild.invite.error_max_invites", MessageType.ERROR, PlaceholderContext.of(inviter));
                return false;
            }
            guildInviteDao.delete(guildId, target.getUniqueId());

            long now = System.currentTimeMillis() / 1000;
            int timeoutMinutes = guildConfig.getInt("member-management.invite-timeout-minutes", 10);
            long expiresAt = now + timeoutMinutes * 60L;
            GuildInvite invite = new GuildInvite(0, guildId, target.getUniqueId(), inviter.getUniqueId(), now, expiresAt);
            guildInviteDao.save(invite);

            send(inviter, "guild.invite.sent", MessageType.SUCCESS,
                    PlaceholderContext.of(inviter).with("player", target.getName()));

            PlaceholderContext targetContext = PlaceholderContext.of(target, guild)
                    .with("sender", inviter.getName())
                    .with("guild", guild.getName())
                    .with("tag", guild.getTag());
            send(target, "guild.invite.received", MessageType.INFO, targetContext);
            send(target, "guild.invite.hint", MessageType.INFO, targetContext.with("command", "/guild join " + guild.getTag()));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to invite player: " + e.getMessage());
            send(inviter, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(inviter));
            return false;
        }
    }

    public boolean acceptInvite(Player player, String tag) {
        try {
            String tagUpper = tag.trim().toUpperCase();
            Optional<Guild> guildOpt = guildDao.findByTag(tagUpper);
            if (guildOpt.isEmpty()) {
                send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild guild = guildOpt.get();
            Optional<GuildInvite> inviteOpt = guildInviteDao.find(guild.getId(), player.getUniqueId());
            if (inviteOpt.isEmpty() || inviteOpt.get().isExpired()) {
                if (inviteOpt.isPresent() && inviteOpt.get().isExpired()) {
                    guildInviteDao.delete(guild.getId(), player.getUniqueId());
                }
                send(player, "guild.join.error_no_invite", MessageType.ERROR,
                        PlaceholderContext.of(player).with("tag", tagUpper));
                return false;
            }
            JoinCheckResult check = canJoinGuild(player, guild.getId());
            if (!check.allowed()) {
                PlaceholderContext ctx = PlaceholderContext.of(player).with("tag", tagUpper);
                if (check.key().equals("guild.join.error_cooldown")) {
                    int cooldownMinutes = guildConfig.getInt("member-management.cooldown-minutes-after-leave", 1440);
                    PlayerData data = getOrCreatePlayerData(player);
                    long remaining = Math.max(0, cooldownMinutes - (System.currentTimeMillis() / 1000 - data.getLeftGuildAt()) / 60);
                    ctx.with("minutes", remaining);
                }
                send(player, check.key(), MessageType.ERROR, ctx);
                return false;
            }
            List<CostItem> joinCost = loadJoinCostItems();
            if (isJoinCostEnabled() && !joinCost.isEmpty()) {
                if (!hasItems(player, joinCost)) {
                    send(player, "guild.join.error_insufficient_items", MessageType.ERROR,
                            PlaceholderContext.of(player).with("items", formatCostItems(joinCost)).with("tag", tagUpper));
                    return false;
                }
                if (!deductItems(player, joinCost)) {
                    send(player, "guild.join.error_insufficient_items", MessageType.ERROR,
                            PlaceholderContext.of(player).with("items", formatCostItems(joinCost)).with("tag", tagUpper));
                    return false;
                }
            }

            guildInviteDao.delete(guild.getId(), player.getUniqueId());
            PlayerData data = getOrCreatePlayerData(player);
            data.setGuildId(guild.getId());
            data.setRank(GuildRank.RECRUIT);
            long now = System.currentTimeMillis() / 1000;
            data.setJoinedGuildAt(now);
            data.setLeftGuildAt(0);
            playerDao.update(data);

            tagManager.updatePlayerTag(player);
            tagManager.updateTagsForGuild(guild.getId());

            PlaceholderContext ctx = PlaceholderContext.of(player, guild)
                    .with("guild", guild.getName())
                    .with("tag", tagUpper);
            send(player, "guild.join.success", MessageType.SUCCESS, ctx);
            send(player, "guild.join.welcome", MessageType.INFO, ctx);

            Component broadcast = langManager.getMessage("guild.join.broadcast", MessageType.INFO,
                    PlaceholderContext.empty().with("player", player.getName()).with("guild", guild.getName()).with("tag", tagUpper));
            broadcastToGuild(guild.getId(), broadcast, player);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to accept invite: " + e.getMessage());
            send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean declineInvite(Player player, String tag) {
        try {
            String tagUpper = tag.trim().toUpperCase();
            Optional<Guild> guildOpt = guildDao.findByTag(tagUpper);
            if (guildOpt.isEmpty()) {
                send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild guild = guildOpt.get();
            Optional<GuildInvite> inviteOpt = guildInviteDao.find(guild.getId(), player.getUniqueId());
            if (inviteOpt.isEmpty()) {
                send(player, "guild.invite.error_no_invite", MessageType.ERROR,
                        PlaceholderContext.of(player).with("tag", tagUpper));
                return false;
            }
            guildInviteDao.delete(guild.getId(), player.getUniqueId());
            send(player, "guild.invite.declined", MessageType.INFO,
                    PlaceholderContext.of(player).with("guild", guild.getName()).with("tag", tagUpper));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to decline invite: " + e.getMessage());
            send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean cancelInvites(Player player) {
        try {
            PlayerData data = getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            GuildRank rank = data.getRank();
            if (!isModeratorOrHigher(rank)) {
                send(player, "guild.invite.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            guildInviteDao.deleteByGuild(data.getGuildId());
            send(player, "guild.invite.cancel", MessageType.SUCCESS, PlaceholderContext.of(player));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to cancel invites: " + e.getMessage());
            send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean kickPlayer(Player kicker, String targetName) {
        if (kicker.getName().equalsIgnoreCase(targetName)) {
            send(kicker, "guild.kick.error_self", MessageType.ERROR, PlaceholderContext.of(kicker));
            return false;
        }
        try {
            PlayerData kickerData = getOrCreatePlayerData(kicker);
            if (kickerData.getGuildId() == null) {
                send(kicker, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(kicker));
                return false;
            }
            GuildRank kickerRank = kickerData.getRank();
            if (!isModeratorOrHigher(kickerRank)) {
                send(kicker, "guild.kick.error_no_permission", MessageType.ERROR, PlaceholderContext.of(kicker));
                return false;
            }
            Optional<PlayerData> targetOpt = resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(kickerData.getGuildId())) {
                send(kicker, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(kicker).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            if (targetData.getRank() == GuildRank.LEADER) {
                send(kicker, "guild.kick.error_leader_kick", MessageType.ERROR,
                        PlaceholderContext.of(kicker).with("player", targetData.getName()));
                return false;
            }
            if (!canManageRank(kickerRank, targetData.getRank())) {
                send(kicker, "guild.kick.error_target_higher_rank", MessageType.ERROR,
                        PlaceholderContext.of(kicker).with("player", targetData.getName()));
                return false;
            }
            UUID guildId = kickerData.getGuildId();
            Optional<Guild> guildOpt = guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            removeFromGuild(targetData);

            send(kicker, "guild.kick.success", MessageType.SUCCESS,
                    PlaceholderContext.of(kicker).with("player", targetData.getName()));

            Component broadcast = langManager.getMessage("guild.kick.broadcast", MessageType.INFO,
                    PlaceholderContext.empty().with("player", targetData.getName()).with("guild", guildName).with("tag", tag));
            broadcastToGuild(guildId, broadcast, kicker);

            Player targetOnline = Bukkit.getPlayer(targetData.getUuid());
            if (targetOnline != null) {
                tagManager.clearPlayerTag(targetOnline);
                send(targetOnline, "guild.kick.target_message", MessageType.ERROR,
                        PlaceholderContext.of(targetOnline).with("guild", guildName).with("tag", tag));
            }
            tagManager.updateTagsForGuild(guildId);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to kick player: " + e.getMessage());
            send(kicker, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(kicker));
            return false;
        }
    }

    public boolean leaveGuild(Player player) {
        try {
            PlayerData data = getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (data.getRank() == GuildRank.LEADER) {
                send(player, "guild.leave.error_leader_cannot_leave", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            UUID guildId = data.getGuildId();
            Optional<Guild> guildOpt = guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            removeFromGuild(data);
            tagManager.clearPlayerTag(player);
            tagManager.updateTagsForGuild(guildId);

            send(player, "guild.leave.success", MessageType.INFO,
                    PlaceholderContext.of(player).with("guild", guildName).with("tag", tag));
            Component broadcast = langManager.getMessage("guild.leave.broadcast", MessageType.INFO,
                    PlaceholderContext.empty().with("player", player.getName()).with("guild", guildName).with("tag", tag));
            broadcastToGuild(guildId, broadcast, player);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to leave guild: " + e.getMessage());
            send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean promotePlayer(Player promoter, String targetName, String targetRankName) {
        try {
            PlayerData promoterData = getOrCreatePlayerData(promoter);
            if (promoterData.getGuildId() == null) {
                send(promoter, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(promoter));
                return false;
            }
            GuildRank promoterRank = promoterData.getRank();
            if (!isLeaderOrCoLeader(promoterRank)) {
                send(promoter, "guild.promote.error_no_permission", MessageType.ERROR, PlaceholderContext.of(promoter));
                return false;
            }
            Optional<PlayerData> targetOpt = resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(promoterData.getGuildId())) {
                send(promoter, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(promoter).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            if (!canManageRank(promoterRank, targetData.getRank())) {
                send(promoter, "guild.promote.error_target_higher_rank", MessageType.ERROR,
                        PlaceholderContext.of(promoter).with("player", targetData.getName()));
                return false;
            }
            GuildRank oldRank = targetData.getRank();
            GuildRank targetRank;
            if (targetRankName != null && !targetRankName.isBlank()) {
                Optional<GuildRank> parsed = resolveRankByName(targetRankName);
                if (parsed.isEmpty()) {
                    send(promoter, "guild.promote.error_invalid_rank", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()).with("rank", targetRankName));
                    return false;
                }
                targetRank = parsed.get();
                if (targetRank == oldRank) {
                    send(promoter, "guild.promote.error_same_rank", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                    return false;
                }
                if (getRankWeight(targetRank) <= getRankWeight(oldRank)) {
                    send(promoter, "guild.promote.error_target_lower_rank", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                    return false;
                }
                if (targetRank == GuildRank.LEADER) {
                    send(promoter, "guild.promote.error_leader_promote", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()));
                    return false;
                }
            } else {
                targetRank = getNextRank(oldRank);
                if (targetRank == null) {
                    send(promoter, "guild.promote.error_max_rank", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()));
                    return false;
                }
            }
            targetData.setRank(targetRank);
            playerDao.update(targetData);

            UUID guildId = promoterData.getGuildId();
            Optional<Guild> guildOpt = guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            send(promoter, "guild.promote.success", MessageType.SUCCESS,
                    PlaceholderContext.of(promoter)
                            .with("player", targetData.getName())
                            .with("old_rank", oldRank != null ? oldRank.getDisplayName() : "?")
                            .with("rank", targetRank.getDisplayName()));

            Component broadcast = langManager.getMessage("guild.promote.broadcast", MessageType.INFO,
                    PlaceholderContext.empty()
                            .with("player", targetData.getName())
                            .with("old_rank", oldRank != null ? oldRank.getDisplayName() : "?")
                            .with("rank", targetRank.getDisplayName())
                            .with("guild", guildName)
                            .with("tag", tag));
            broadcastToGuild(guildId, broadcast, promoter);

            Player targetOnline = Bukkit.getPlayer(targetData.getUuid());
            if (targetOnline != null) {
                send(targetOnline, "guild.promote.target_message", MessageType.INFO,
                        PlaceholderContext.of(targetOnline).with("rank", targetRank.getDisplayName()).with("guild", guildName).with("tag", tag));
            }
            tagManager.updateTagsForGuild(guildId);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to promote player: " + e.getMessage());
            send(promoter, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(promoter));
            return false;
        }
    }

    public boolean demotePlayer(Player demoter, String targetName, String targetRankName) {
        try {
            PlayerData demoterData = getOrCreatePlayerData(demoter);
            if (demoterData.getGuildId() == null) {
                send(demoter, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(demoter));
                return false;
            }
            GuildRank demoterRank = demoterData.getRank();
            if (!isLeaderOrCoLeader(demoterRank)) {
                send(demoter, "guild.demote.error_no_permission", MessageType.ERROR, PlaceholderContext.of(demoter));
                return false;
            }
            Optional<PlayerData> targetOpt = resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(demoterData.getGuildId())) {
                send(demoter, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(demoter).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            if (!canManageRank(demoterRank, targetData.getRank())) {
                send(demoter, "guild.demote.error_target_higher_rank", MessageType.ERROR,
                        PlaceholderContext.of(demoter).with("player", targetData.getName()));
                return false;
            }
            GuildRank oldRank = targetData.getRank();
            if (oldRank == GuildRank.LEADER) {
                send(demoter, "guild.demote.error_leader_demote", MessageType.ERROR,
                        PlaceholderContext.of(demoter).with("player", targetData.getName()));
                return false;
            }
            GuildRank targetRank;
            if (targetRankName != null && !targetRankName.isBlank()) {
                Optional<GuildRank> parsed = resolveRankByName(targetRankName);
                if (parsed.isEmpty()) {
                    send(demoter, "guild.demote.error_invalid_rank", MessageType.ERROR,
                            PlaceholderContext.of(demoter).with("player", targetData.getName()).with("rank", targetRankName));
                    return false;
                }
                targetRank = parsed.get();
                if (targetRank == oldRank) {
                    send(demoter, "guild.demote.error_same_rank", MessageType.ERROR,
                            PlaceholderContext.of(demoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                    return false;
                }
                if (getRankWeight(targetRank) >= getRankWeight(oldRank)) {
                    send(demoter, "guild.demote.error_target_rank_higher", MessageType.ERROR,
                            PlaceholderContext.of(demoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                    return false;
                }
            } else {
                targetRank = getPreviousRank(oldRank);
                if (targetRank == null) {
                    send(demoter, "guild.demote.error_min_rank", MessageType.ERROR,
                            PlaceholderContext.of(demoter).with("player", targetData.getName()));
                    return false;
                }
            }
            targetData.setRank(targetRank);
            playerDao.update(targetData);

            UUID guildId = demoterData.getGuildId();
            Optional<Guild> guildOpt = guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            send(demoter, "guild.demote.success", MessageType.SUCCESS,
                    PlaceholderContext.of(demoter)
                            .with("player", targetData.getName())
                            .with("old_rank", oldRank != null ? oldRank.getDisplayName() : "?")
                            .with("rank", targetRank.getDisplayName()));

            Component broadcast = langManager.getMessage("guild.demote.broadcast", MessageType.INFO,
                    PlaceholderContext.empty()
                            .with("player", targetData.getName())
                            .with("old_rank", oldRank != null ? oldRank.getDisplayName() : "?")
                            .with("rank", targetRank.getDisplayName())
                            .with("guild", guildName)
                            .with("tag", tag));
            broadcastToGuild(guildId, broadcast, demoter);

            Player targetOnline = Bukkit.getPlayer(targetData.getUuid());
            if (targetOnline != null) {
                send(targetOnline, "guild.demote.target_message", MessageType.INFO,
                        PlaceholderContext.of(targetOnline).with("rank", targetRank.getDisplayName()).with("guild", guildName).with("tag", tag));
            }
            tagManager.updateTagsForGuild(guildId);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to demote player: " + e.getMessage());
            send(demoter, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(demoter));
            return false;
        }
    }

    public boolean transferLeadership(Player leader, String targetName) {
        try {
            PlayerData leaderData = getOrCreatePlayerData(leader);
            if (leaderData.getGuildId() == null) {
                send(leader, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(leader));
                return false;
            }
            if (leaderData.getRank() != GuildRank.LEADER) {
                send(leader, "guild.leader.error_no_permission", MessageType.ERROR, PlaceholderContext.of(leader));
                return false;
            }
            Optional<PlayerData> targetOpt = resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(leaderData.getGuildId())) {
                send(leader, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(leader).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            if (targetData.getUuid().equals(leader.getUniqueId())) {
                send(leader, "guild.leader.error_self", MessageType.ERROR, PlaceholderContext.of(leader));
                return false;
            }
            UUID guildId = leaderData.getGuildId();
            Optional<Guild> guildOpt = guildDao.findById(guildId);
            if (guildOpt.isEmpty()) {
                send(leader, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(leader));
                return false;
            }
            Guild guild = guildOpt.get();

            leaderData.setRank(GuildRank.CO_LEADER);
            targetData.setRank(GuildRank.LEADER);
            guild.setLeaderUuid(targetData.getUuid());
            playerDao.update(leaderData);
            playerDao.update(targetData);
            guildDao.update(guild);

            tagManager.updateTagsForGuild(guildId);
            Player targetOnline = Bukkit.getPlayer(targetData.getUuid());

            send(leader, "guild.leader.success", MessageType.SUCCESS,
                    PlaceholderContext.of(leader).with("player", targetData.getName()));
            Component broadcast = langManager.getMessage("guild.leader.broadcast", MessageType.INFO,
                    PlaceholderContext.empty().with("player", targetData.getName()).with("guild", guild.getName()).with("tag", guild.getTag()));
            broadcastToGuild(guildId, broadcast, leader);
            if (targetOnline != null) {
                send(targetOnline, "guild.leader.target_message", MessageType.INFO,
                        PlaceholderContext.of(targetOnline).with("guild", guild.getName()).with("tag", guild.getTag()));
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to transfer leadership: " + e.getMessage());
            send(leader, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(leader));
            return false;
        }
    }

    public boolean banPlayer(Player banner, String targetName, String reason) {
        try {
            PlayerData bannerData = getOrCreatePlayerData(banner);
            if (bannerData.getGuildId() == null) {
                send(banner, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(banner));
                return false;
            }
            GuildRank bannerRank = bannerData.getRank();
            if (!isLeaderOrCoLeader(bannerRank)) {
                send(banner, "guild.ban.error_no_permission", MessageType.ERROR, PlaceholderContext.of(banner));
                return false;
            }
            Optional<PlayerData> targetOpt = resolveTargetData(targetName);
            UUID guildId = bannerData.getGuildId();
            if (targetOpt.isEmpty()) {
                send(banner, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(banner).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            UUID targetUuid = targetData.getUuid();
            String targetDisplayName = targetData.getName();
            GuildRank targetRank = targetData.getRank();
            if (targetData.getGuildId() != null && targetData.getGuildId().equals(guildId)) {
                if (targetRank == GuildRank.LEADER || !canManageRank(bannerRank, targetRank)) {
                    send(banner, "guild.ban.error_target_higher_rank", MessageType.ERROR,
                            PlaceholderContext.of(banner).with("player", targetDisplayName));
                    return false;
                }
            }
            if (guildBanDao.exists(guildId, targetUuid)) {
                send(banner, "guild.ban.error_already_banned", MessageType.ERROR,
                        PlaceholderContext.of(banner).with("player", targetDisplayName));
                return false;
            }
            if (targetData.getGuildId() != null && targetData.getGuildId().equals(guildId)) {
                removeFromGuild(targetData);
                Player targetOnline = Bukkit.getPlayer(targetUuid);
                if (targetOnline != null) {
                    tagManager.clearPlayerTag(targetOnline);
                }
                tagManager.updateTagsForGuild(guildId);
            }
            long now = System.currentTimeMillis() / 1000;
            GuildBan ban = new GuildBan(0, guildId, targetUuid, reason, banner.getUniqueId(), now);
            guildBanDao.save(ban);

            Optional<Guild> guildOpt = guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            send(banner, "guild.ban.success", MessageType.SUCCESS,
                    PlaceholderContext.of(banner).with("player", targetDisplayName));
            Component broadcast = langManager.getMessage("guild.ban.broadcast", MessageType.INFO,
                    PlaceholderContext.empty().with("player", targetDisplayName).with("guild", guildName).with("tag", tag));
            broadcastToGuild(guildId, broadcast, banner);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to ban player: " + e.getMessage());
            send(banner, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(banner));
            return false;
        }
    }

    public boolean unbanPlayer(Player banner, String targetName) {
        try {
            PlayerData bannerData = getOrCreatePlayerData(banner);
            if (bannerData.getGuildId() == null) {
                send(banner, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(banner));
                return false;
            }
            if (!isLeaderOrCoLeader(bannerData.getRank())) {
                send(banner, "guild.unban.error_no_permission", MessageType.ERROR, PlaceholderContext.of(banner));
                return false;
            }
            Optional<PlayerData> targetOpt = resolveTargetData(targetName);
            if (targetOpt.isEmpty()) {
                send(banner, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(banner).with("player", targetName));
                return false;
            }
            UUID guildId = bannerData.getGuildId();
            UUID targetUuid = targetOpt.get().getUuid();
            String targetDisplayName = targetOpt.get().getName();
            if (!guildBanDao.exists(guildId, targetUuid)) {
                send(banner, "guild.unban.error_not_banned", MessageType.ERROR,
                        PlaceholderContext.of(banner).with("player", targetDisplayName));
                return false;
            }
            guildBanDao.delete(guildId, targetUuid);
            send(banner, "guild.unban.success", MessageType.SUCCESS,
                    PlaceholderContext.of(banner).with("player", targetDisplayName));
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to unban player: " + e.getMessage());
            send(banner, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(banner));
            return false;
        }
    }

    public boolean sendGuildInfo(Player player, String tag) {
        try {
            Optional<Guild> guildOpt;
            if (tag == null) {
                PlayerData data = getOrCreatePlayerData(player);
                if (data.getGuildId() == null) {
                    send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                    return false;
                }
                guildOpt = guildDao.findById(data.getGuildId());
            } else {
                guildOpt = guildDao.findByTag(tag.trim().toUpperCase());
            }
            if (guildOpt.isEmpty()) {
                send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild guild = guildOpt.get();
            List<PlayerData> members = playerDao.findByGuild(guild.getId());
            PlayerData leaderData = members.stream().filter(m -> m.getRank() == GuildRank.LEADER).findFirst().orElse(null);
            String leaderName = leaderData != null ? leaderData.getName() : langManager.getRawMessage("placeholders.not_available");
            PlaceholderContext ctx = PlaceholderContext.of(player, guild)
                    .with("guild", guild.getName())
                    .with("tag", guild.getTag())
                    .with("leader", leaderName != null ? leaderName : "N/A")
                    .with("points", guild.getPoints())
                    .with("level", guild.getLevel())
                    .with("members", members.size())
                    .with("date", formatDate(guild.getCreatedAt()));
            send(player, "guild.info.header", MessageType.INFO, ctx);
            send(player, "guild.info.leader", MessageType.INFO, ctx);
            send(player, "guild.info.points", MessageType.INFO, ctx);
            send(player, "guild.info.level", MessageType.INFO, ctx);
            send(player, "guild.info.members", MessageType.INFO, ctx);
            send(player, "guild.info.created", MessageType.INFO, ctx);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to show guild info: " + e.getMessage());
            send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private JoinCheckResult canJoinGuild(Player player, UUID guildId) {
        try {
            PlayerData data = getOrCreatePlayerData(player);
            if (data.getGuildId() != null) {
                return new JoinCheckResult(false, "guild.join.error_already_in_guild");
            }
            if (guildBanDao.exists(guildId, player.getUniqueId())) {
                return new JoinCheckResult(false, "guild.join.error_banned");
            }
            long leftAt = data.getLeftGuildAt();
            int cooldownMinutes = guildConfig.getInt("member-management.cooldown-minutes-after-leave", 1440);
            if (leftAt > 0 && cooldownMinutes > 0) {
                long elapsed = (System.currentTimeMillis() / 1000 - leftAt) / 60;
                if (elapsed < cooldownMinutes) {
                    return new JoinCheckResult(false, "guild.join.error_cooldown");
                }
            }
            return new JoinCheckResult(true, null);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to validate join requirements: " + e.getMessage());
            return new JoinCheckResult(false, "guild.create.error_generic");
        }
    }

    private PlayerData getOrCreatePlayerData(Player player) throws SQLException {
        Optional<PlayerData> existing = playerDao.findByUuid(player.getUniqueId());
        if (existing.isPresent()) {
            PlayerData data = existing.get();
            data.setName(player.getName());
            return data;
        }
        PlayerData data = new PlayerData(player.getUniqueId(), player.getName(), null, null, 0, 0, 0, 1000, 0, 0);
        playerDao.save(data);
        return data;
    }

    private Optional<PlayerData> resolveTargetData(String name) throws SQLException {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            Optional<PlayerData> data = playerDao.findByUuid(online.getUniqueId());
            if (data.isPresent()) {
                return data;
            }
            PlayerData created = new PlayerData(online.getUniqueId(), online.getName(), null, null, 0, 0, 0, 1000, 0, 0);
            playerDao.save(created);
            return Optional.of(created);
        }
        return playerDao.findByName(name);
    }

    private void removeFromGuild(PlayerData data) throws SQLException {
        data.setGuildId(null);
        data.setRank(null);
        data.setJoinedGuildAt(0);
        data.setLeftGuildAt(System.currentTimeMillis() / 1000);
        playerDao.update(data);
    }

    private int getMemberCount(UUID guildId) throws SQLException {
        return playerDao.findByGuild(guildId).size();
    }

    private boolean isJoinCostEnabled() {
        return guildConfig.getBoolean("member-management.join-cost.enabled", false);
    }

    private String formatCostItems(List<CostItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(i -> i.amount() + "x " + i.material().name())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private void broadcastToGuild(UUID guildId, Component message) {
        broadcastToGuild(guildId, message, null);
    }

    private void broadcastToGuild(UUID guildId, Component message, Player exclude) {
        try {
            List<PlayerData> members = playerDao.findByGuild(guildId);
            for (PlayerData member : members) {
                Player online = Bukkit.getPlayer(member.getUuid());
                if (online == null || (exclude != null && online.getUniqueId().equals(exclude.getUniqueId()))) {
                    continue;
                }
                online.sendMessage(message);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to broadcast to guild: " + e.getMessage());
        }
    }

    private void send(Player player, String key, MessageType type, PlaceholderContext context) {
        String raw = langManager.getRawMessage(key);
        if (raw == null) {
            player.sendMessage(Component.text("[Brak tlumaczenia: " + key + "]").color(NamedTextColor.RED));
            return;
        }
        player.sendMessage(langManager.getMessage(key, type, context));
    }

    private String formatDate(long timestamp) {
        return java.time.Instant.ofEpochSecond(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .toString();
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
        return guildConfig.getInt("ranks." + key + ".weight", rank.ordinal() * 20);
    }

    private boolean canManageRank(GuildRank actor, GuildRank target) {
        return getRankWeight(actor) > getRankWeight(target);
    }

    private GuildRank getNextRank(GuildRank rank) {
        return switch (rank) {
            case RECRUIT -> GuildRank.MEMBER;
            case MEMBER -> GuildRank.MODERATOR;
            case MODERATOR -> GuildRank.CO_LEADER;
            default -> null;
        };
    }

    private GuildRank getPreviousRank(GuildRank rank) {
        return switch (rank) {
            case CO_LEADER -> GuildRank.MODERATOR;
            case MODERATOR -> GuildRank.MEMBER;
            case MEMBER -> GuildRank.RECRUIT;
            default -> null;
        };
    }

    private Optional<GuildRank> resolveRankByName(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeRankInput(input);
        Map<String, GuildRank> map = new HashMap<>();
        for (GuildRank rank : GuildRank.values()) {
            map.put(normalizeRankInput(rank.name()), rank);
            map.put(normalizeRankInput(rank.getDisplayName()), rank);
            String configKey = switch (rank) {
                case LEADER -> "leader";
                case CO_LEADER -> "coleader";
                case MODERATOR -> "moderator";
                case MEMBER -> "member";
                case RECRUIT -> "recruit";
            };
            String configName = guildConfig.getString("ranks." + configKey + ".name", rank.getDisplayName());
            if (configName != null && !configName.isBlank()) {
                map.put(normalizeRankInput(configName), rank);
            }
        }
        // Common aliases
        map.put("lider", GuildRank.LEADER);
        map.put("leader", GuildRank.LEADER);
        map.put("co-lider", GuildRank.CO_LEADER);
        map.put("coleader", GuildRank.CO_LEADER);
        map.put("colider", GuildRank.CO_LEADER);
        map.put("moderator", GuildRank.MODERATOR);
        map.put("mod", GuildRank.MODERATOR);
        map.put("czlonek", GuildRank.MEMBER);
        map.put("członek", GuildRank.MEMBER);
        map.put("member", GuildRank.MEMBER);
        map.put("rekrut", GuildRank.RECRUIT);
        map.put("recruit", GuildRank.RECRUIT);
        return Optional.ofNullable(map.get(normalized));
    }

    private String normalizeRankInput(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\-_]", "")
                .replace("ł", "l");
    }

    private boolean isModeratorOrHigher(GuildRank rank) {
        return rank == GuildRank.LEADER || rank == GuildRank.CO_LEADER || rank == GuildRank.MODERATOR;
    }

    private boolean isLeaderOrCoLeader(GuildRank rank) {
        return rank == GuildRank.LEADER || rank == GuildRank.CO_LEADER;
    }

    public boolean sendBanList(Player player) {
        try {
            PlayerData data = getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!isLeaderOrCoLeader(data.getRank())) {
                send(player, "guild.banlist.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            UUID guildId = data.getGuildId();
            Optional<Guild> guildOpt = guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            List<GuildBan> bans = guildBanDao.findByGuild(guildId);
            if (bans.isEmpty()) {
                send(player, "guild.banlist.empty", MessageType.INFO,
                        PlaceholderContext.of(player).with("guild", guildName).with("tag", tag));
                return true;
            }

            send(player, "guild.banlist.header", MessageType.INFO,
                    PlaceholderContext.of(player).with("guild", guildName).with("tag", tag).with("count", bans.size()));
            for (GuildBan ban : bans) {
                Optional<PlayerData> targetOpt = playerDao.findByUuid(ban.getPlayerUuid());
                String targetName = targetOpt.map(PlayerData::getName).orElse("?");
                PlayerData bannerData = playerDao.findByUuid(ban.getBannedBy()).orElse(null);
                String bannerName = bannerData != null ? bannerData.getName() : "?";
                String reason = ban.getReason() != null && !ban.getReason().isEmpty() ? ban.getReason() : langManager.getRawMessage("placeholders.not_available");
                send(player, "guild.banlist.entry", MessageType.INFO,
                        PlaceholderContext.of(player)
                                .with("player", targetName)
                                .with("banner", bannerName)
                                .with("reason", reason != null ? reason : "N/A")
                                .with("date", formatDate(ban.getBannedAt())));
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to show ban list: " + e.getMessage());
            send(player, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    private record JoinCheckResult(boolean allowed, String key) {
    }
}
