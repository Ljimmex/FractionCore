package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.CuboidData;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildCreateResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class GuildCreationValidator {

    private final GuildContext context;
    private final GuildCreationHelper helper;

    public GuildCreationValidator(GuildContext context) {
        this.context = context;
        this.helper = new GuildCreationHelper(context);
    }

    public ValidationResult validateAll(Player founder, String rawName, String rawTag) {
        ValidationResult syncValidation = validateSync(founder, rawName, rawTag);
        if (syncValidation.result() != GuildCreateResult.SUCCESS) {
            return syncValidation;
        }

        try {
            if (context.guildDao.existsByName(syncValidation.name())) {
                return new ValidationResult(GuildCreateResult.NAME_TAKEN, syncValidation.name(), syncValidation.tag());
            }
            if (context.guildDao.existsByTag(syncValidation.tag())) {
                return new ValidationResult(GuildCreateResult.TAG_TAKEN, syncValidation.name(), syncValidation.tag());
            }
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to check guild uniqueness: " + e.getMessage());
            return new ValidationResult(GuildCreateResult.ERROR, syncValidation.name(), syncValidation.tag());
        }

        return syncValidation;
    }

    public ValidationResult validateSync(Player founder, String rawName, String rawTag) {
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

        List<CostItem> costItems = helper.loadCostItems();
        if (!context.hasItems(founder, costItems)) {
            return new ValidationResult(GuildCreateResult.INSUFFICIENT_ITEMS, name, tag);
        }

        return new ValidationResult(GuildCreateResult.SUCCESS, name, tag);
    }

    public CompletableFuture<ValidationResult> validateAsync(Player founder, String rawName, String rawTag, ValidationResult syncResult) {
        if (syncResult.result() != GuildCreateResult.SUCCESS) {
            return CompletableFuture.completedFuture(syncResult);
        }

        return context.databaseExecutor.supplyAsync(() -> {
            try {
                if (context.guildDao.existsByName(syncResult.name())) {
                    return new ValidationResult(GuildCreateResult.NAME_TAKEN, syncResult.name(), syncResult.tag());
                }
                if (context.guildDao.existsByTag(syncResult.tag())) {
                    return new ValidationResult(GuildCreateResult.TAG_TAKEN, syncResult.name(), syncResult.tag());
                }
                return syncResult;
            } catch (SQLException e) {
                context.plugin.getLogger().severe("Failed to check guild uniqueness: " + e.getMessage());
                return new ValidationResult(GuildCreateResult.ERROR, syncResult.name(), syncResult.tag());
            }
        });
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

    public record ValidationResult(GuildCreateResult result, String name, String tag) {
    }
}
