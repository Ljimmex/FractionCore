package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.cuboid.model.CuboidAction;
import pl.Ljimmex.fractionCore.cuboid.model.CuboidFlagType;
import pl.Ljimmex.fractionCore.cuboid.model.CuboidFlagValue;
import pl.Ljimmex.fractionCore.database.entity.CuboidData;
import pl.Ljimmex.fractionCore.database.entity.GuildFlag;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.database.entity.RelationType;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;

import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CuboidManager {

    private final GuildContext context;
    private final List<CuboidData> cuboids = new CopyOnWriteArrayList<>();
    private final Map<UUID, Map<CuboidFlagType, CuboidFlagValue>> flags = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tntCooldowns = new ConcurrentHashMap<>();

    public CuboidManager(GuildContext context) {
        this.context = context;
        load();
    }

    // ============================================================
    // Loading
    // ============================================================

    public void load() {
        cuboids.clear();
        flags.clear();
        tntCooldowns.clear();

        try {
            cuboids.addAll(context.cuboidDao.findAll());
            for (CuboidData cuboid : cuboids) {
                UUID guildId = cuboid.getGuildId();
                loadFlags(guildId);
            }
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to load cuboids: " + e.getMessage());
            context.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Full stack trace", e);
        }
    }

    private void loadFlags(UUID guildId) throws SQLException {
        Map<CuboidFlagType, CuboidFlagValue> map = new EnumMap<>(CuboidFlagType.class);
        for (GuildFlag flag : context.guildFlagDao.findByGuild(guildId)) {
            try {
                CuboidFlagType type = CuboidFlagType.valueOf(flag.getFlagName());
                CuboidFlagValue value = CuboidFlagValue.valueOf(flag.getFlagValue());
                map.put(type, value);
            } catch (IllegalArgumentException e) {
                context.plugin.getLogger().warning("Ignoring invalid cuboid flag " + flag.getFlagName()
                        + "=" + flag.getFlagValue() + " for guild " + guildId);
            }
        }
        flags.put(guildId, map);
    }

    // ============================================================
    // Cuboid lookup
    // ============================================================

    public Optional<CuboidData> findCuboidAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (CuboidData cuboid : cuboids) {
            if (!cuboid.getWorld().equalsIgnoreCase(worldName)) {
                continue;
            }
            int dx = Math.abs(x - cuboid.getCenterX());
            int dz = Math.abs(z - cuboid.getCenterZ());
            if (dx <= cuboid.getRadius() && dz <= cuboid.getRadius()) {
                return Optional.of(cuboid);
            }
        }
        return Optional.empty();
    }

    public Optional<CuboidData> findCuboidByGuild(UUID guildId) {
        for (CuboidData cuboid : cuboids) {
            if (cuboid.getGuildId().equals(guildId)) {
                return Optional.of(cuboid);
            }
        }
        return Optional.empty();
    }

    public Optional<UUID> getGuildAt(Location location) {
        return findCuboidAt(location).map(CuboidData::getGuildId);
    }

    public List<CuboidData> getCuboids() {
        return Collections.unmodifiableList(cuboids);
    }

    // ============================================================
    // Cuboid lifecycle
    // ============================================================

    public CuboidData createDefaultCuboid(UUID guildId, World world, Location center, int radius) {
        CuboidData cuboid = new CuboidData(
                guildId,
                world.getName(),
                center.getBlockX(),
                center.getBlockY(),
                center.getBlockZ(),
                radius,
                1
        );

        try {
            context.cuboidDao.save(cuboid);
            cuboids.add(cuboid);

            Map<CuboidFlagType, CuboidFlagValue> defaultFlags = defaultFlags();
            for (Map.Entry<CuboidFlagType, CuboidFlagValue> entry : defaultFlags.entrySet()) {
                context.guildFlagDao.save(new GuildFlag(guildId, entry.getKey().name(), entry.getValue().name()));
            }
            flags.put(guildId, new EnumMap<>(defaultFlags));
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to create default cuboid for guild " + guildId + ": " + e.getMessage());
            context.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Full stack trace", e);
        }

        return cuboid;
    }

    public void deleteCuboid(UUID guildId) {
        try {
            context.cuboidDao.delete(guildId);
            context.guildFlagDao.deleteByGuild(guildId);
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to delete cuboid data for guild " + guildId + ": " + e.getMessage());
            context.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Full stack trace", e);
        }

        cuboids.removeIf(c -> c.getGuildId().equals(guildId));
        flags.remove(guildId);
    }

    // ============================================================
    // Flags
    // ============================================================

    public void setFlag(UUID guildId, CuboidFlagType type, CuboidFlagValue value) {
        Map<CuboidFlagType, CuboidFlagValue> map = flags.computeIfAbsent(guildId, k -> new EnumMap<>(CuboidFlagType.class));
        map.put(type, value);

        try {
            GuildFlag flag = new GuildFlag(guildId, type.name(), value.name());
            if (context.guildFlagDao.find(guildId, type.name()).isPresent()) {
                context.guildFlagDao.update(flag);
            } else {
                context.guildFlagDao.save(flag);
            }
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to save cuboid flag " + type + " for guild " + guildId + ": " + e.getMessage());
            context.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Full stack trace", e);
        }
    }

    public CuboidFlagValue getFlag(UUID guildId, CuboidFlagType type) {
        Map<CuboidFlagType, CuboidFlagValue> map = flags.get(guildId);
        if (map != null && map.containsKey(type)) {
            return map.get(type);
        }
        return defaultValue(type);
    }

    public Map<CuboidFlagType, CuboidFlagValue> getFlags(UUID guildId) {
        Map<CuboidFlagType, CuboidFlagValue> map = new EnumMap<>(CuboidFlagType.class);
        for (CuboidFlagType type : CuboidFlagType.values()) {
            map.put(type, getFlag(guildId, type));
        }
        return map;
    }

    private static Map<CuboidFlagType, CuboidFlagValue> defaultFlags() {
        Map<CuboidFlagType, CuboidFlagValue> map = new EnumMap<>(CuboidFlagType.class);
        for (CuboidFlagType type : CuboidFlagType.values()) {
            map.put(type, defaultValue(type));
        }
        return map;
    }

    private static CuboidFlagValue defaultValue(CuboidFlagType type) {
        return switch (type) {
            case FRIENDLY_FIRE -> CuboidFlagValue.DENY;
            default -> CuboidFlagValue.MEMBERS;
        };
    }

    // ============================================================
    // TNT build cooldown
    // ============================================================

    public void activateTntCooldown(UUID guildId) {
        int seconds = context.guildConfig.getInt("cuboid.tnt-build-cooldown-seconds", 60);
        if (seconds <= 0) {
            return;
        }
        tntCooldowns.put(guildId, System.currentTimeMillis() + seconds * 1000L);
    }

    public boolean isTntCooldownActive(UUID guildId) {
        Long expiresAt = tntCooldowns.get(guildId);
        if (expiresAt == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiresAt) {
            tntCooldowns.remove(guildId);
            return false;
        }
        return true;
    }

    public long getRemainingTntCooldownSeconds(UUID guildId) {
        Long expiresAt = tntCooldowns.get(guildId);
        if (expiresAt == null) {
            return 0;
        }
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        if (remaining <= 0) {
            tntCooldowns.remove(guildId);
            return 0;
        }
        return remaining;
    }

    // ============================================================
    // Permission checks
    // ============================================================

    public boolean isAllowed(Player player, CuboidAction action) {
        return isAllowed(player, player.getLocation(), action);
    }

    public boolean isAllowed(Player player, Location location, CuboidAction action) {
        Optional<CuboidData> cuboidOpt = findCuboidAt(location);
        if (cuboidOpt.isEmpty()) {
            return true;
        }

        CuboidData cuboid = cuboidOpt.get();
        UUID guildId = cuboid.getGuildId();

        if ((action == CuboidAction.BUILD || action == CuboidAction.DESTROY) && isTntCooldownActive(guildId)) {
            return false;
        }

        CuboidFlagType flagType = mapActionToFlag(action);
        CuboidFlagValue flag = getFlag(guildId, flagType);

        if (flag == CuboidFlagValue.ALLOW) {
            return true;
        }
        if (flag == CuboidFlagValue.DENY) {
            return false;
        }

        AccessLevel access = resolveAccess(player, cuboid.getGuildId());
        return switch (flag) {
            case MEMBERS -> access == AccessLevel.LEADER || access == AccessLevel.MEMBER;
            case ALLIES -> access == AccessLevel.LEADER || access == AccessLevel.MEMBER || access == AccessLevel.ALLY;
            case LEADER -> access == AccessLevel.LEADER;
            default -> false;
        };
    }

    private AccessLevel resolveAccess(Player player, UUID cuboidGuildId) {
        Optional<PlayerGuildCache.Entry> cached = context.getPlayerGuildCache().get(player.getUniqueId());
        if (cached.isPresent()) {
            PlayerGuildCache.Entry entry = cached.get();
            UUID playerGuildId = entry.guildId();
            if (playerGuildId.equals(cuboidGuildId)) {
                return entry.rank() == GuildRank.LEADER ? AccessLevel.LEADER : AccessLevel.MEMBER;
            }
            if (context.relationManager.getRelationType(playerGuildId, cuboidGuildId) == RelationType.ALLY) {
                return AccessLevel.ALLY;
            }
            return AccessLevel.OUTSIDER;
        }

        Optional<PlayerData> dataOpt;
        try {
            dataOpt = context.playerDao.findByUuid(player.getUniqueId());
        } catch (SQLException e) {
            context.plugin.getLogger().warning("Failed to resolve player data for cuboid check: " + e.getMessage());
            return AccessLevel.OUTSIDER;
        }

        if (dataOpt.isEmpty()) {
            return AccessLevel.OUTSIDER;
        }

        PlayerData data = dataOpt.get();
        context.getPlayerGuildCache().refresh(player.getUniqueId(), data);
        UUID playerGuildId = data.getGuildId();

        if (playerGuildId != null && playerGuildId.equals(cuboidGuildId)) {
            return data.getRank() == GuildRank.LEADER ? AccessLevel.LEADER : AccessLevel.MEMBER;
        }

        if (playerGuildId != null) {
            RelationType relation = context.relationManager.getRelationType(playerGuildId, cuboidGuildId);
            if (relation == RelationType.ALLY) {
                return AccessLevel.ALLY;
            }
        }

        return AccessLevel.OUTSIDER;
    }

    private static CuboidFlagType mapActionToFlag(CuboidAction action) {
        return switch (action) {
            case BUILD -> CuboidFlagType.BUILD;
            case DESTROY -> CuboidFlagType.DESTROY;
            case USE -> CuboidFlagType.USE;
            case INTERACT -> CuboidFlagType.INTERACT;
            case TNT -> CuboidFlagType.TNT;
            case FRIENDLY_FIRE -> CuboidFlagType.FRIENDLY_FIRE;
        };
    }

    public enum AccessLevel {
        OUTSIDER,
        ALLY,
        MEMBER,
        LEADER
    }

    // ============================================================
    // Command helpers
    // ============================================================

    public boolean setCuboidFlag(Player player, String flagName, String valueName) {
        SetCuboidFlagResult result = prepareSetCuboidFlag(player, flagName, valueName);
        applySetCuboidFlagEffects(player, result);
        return result.success();
    }

    public SetCuboidFlagResult prepareSetCuboidFlag(Player player, String flagName, String valueName) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return SetCuboidFlagResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return SetCuboidFlagResult.error("guild.cuboid.flag.error_no_permission", PlaceholderContext.of(player));
            }

            CuboidFlagType flagType;
            CuboidFlagValue flagValue;
            try {
                flagType = CuboidFlagType.valueOf(flagName.toUpperCase());
                flagValue = CuboidFlagValue.valueOf(valueName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return SetCuboidFlagResult.error("guild.cuboid.flag.error_invalid", PlaceholderContext.of(player));
            }

            setFlag(data.getGuildId(), flagType, flagValue);
            return new SetCuboidFlagResult(true, flagType, flagValue, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to set cuboid flag: " + e.getMessage());
            return SetCuboidFlagResult.error("guild.cuboid.flag.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applySetCuboidFlagEffects(Player player, SetCuboidFlagResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        PlaceholderContext ctx = PlaceholderContext.of(player)
                .with("flag", result.flagType().name())
                .with("value", result.flagValue().name());
        context.send(player, "guild.cuboid.flag.success", MessageType.SUCCESS, ctx);
    }

    public java.util.concurrent.CompletableFuture<SetCuboidFlagResult> setCuboidFlagAsync(Player player, String flagName, String valueName) {
        return context.databaseExecutor.supplyAsync(() -> prepareSetCuboidFlag(player, flagName, valueName));
    }

    public record SetCuboidFlagResult(boolean success, CuboidFlagType flagType, CuboidFlagValue flagValue,
                                       String errorKey, PlaceholderContext errorContext) {
        public static SetCuboidFlagResult error(String key, PlaceholderContext context) {
            return new SetCuboidFlagResult(false, null, null, key, context);
        }
    }

    public boolean sendCuboidFlagList(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Optional<CuboidData> cuboidOpt = findCuboidByGuild(data.getGuildId());
            if (cuboidOpt.isEmpty()) {
                context.send(player, "guild.cuboid.flag.error_no_cuboid", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }

            player.sendMessage(net.kyori.adventure.text.Component.text("=== Flagi cuboidu ===")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
            Map<CuboidFlagType, CuboidFlagValue> flagMap = getFlags(data.getGuildId());
            for (Map.Entry<CuboidFlagType, CuboidFlagValue> entry : flagMap.entrySet()) {
                player.sendMessage(net.kyori.adventure.text.Component.text(entry.getKey().name() + ": ")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                        .append(net.kyori.adventure.text.Component.text(entry.getValue().name())
                                .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)));
            }
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to list cuboid flags: " + e.getMessage());
            context.send(player, "guild.cuboid.flag.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public AccessLevel getAccessLevel(Player player, UUID cuboidGuildId) {
        return resolveAccess(player, cuboidGuildId);
    }

    public Optional<UUID> getPlayerGuildId(Player player) {
        Optional<PlayerGuildCache.Entry> cached = context.getPlayerGuildCache().get(player.getUniqueId());
        if (cached.isPresent()) {
            return Optional.of(cached.get().guildId());
        }

        try {
            Optional<PlayerData> data = context.playerDao.findByUuid(player.getUniqueId());
            data.ifPresent(playerData -> context.getPlayerGuildCache().refresh(player.getUniqueId(), playerData));
            return data.map(PlayerData::getGuildId);
        } catch (SQLException e) {
            context.plugin.getLogger().warning("Failed to resolve player guild: " + e.getMessage());
            return Optional.empty();
        }
    }
}
