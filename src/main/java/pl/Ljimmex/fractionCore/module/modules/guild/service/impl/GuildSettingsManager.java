package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildInfo;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class GuildSettingsManager {

    private final GuildContext context;
    private final java.util.Map<UUID, Long> homeCooldowns = new ConcurrentHashMap<>();
    private final java.util.Map<UUID, PendingHomeTeleport> homeTeleports = new ConcurrentHashMap<>();

    public GuildSettingsManager(GuildContext context) {
        this.context = context;
    }

    public boolean setGuildHome(Player player) {
        SetHomeResult result = prepareSetGuildHome(player, player.getLocation());
        applySetGuildHomeEffects(player, result);
        return result.success();
    }

    public SetHomeResult prepareSetGuildHome(Player player, Location loc) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return SetHomeResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isModeratorOrHigher(data.getRank())) {
                return SetHomeResult.error("guild.sethome.error_no_permission", PlaceholderContext.of(player));
            }
            Optional<Guild> guildOpt = context.guildDao.findById(data.getGuildId());
            if (guildOpt.isEmpty()) {
                return SetHomeResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild guild = guildOpt.get();
            guild.setHomeWorld(loc.getWorld().getName());
            guild.setHomeX(loc.getX());
            guild.setHomeY(loc.getY());
            guild.setHomeZ(loc.getZ());
            context.guildDao.update(guild);
            return new SetHomeResult(true, guild, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to set guild home: " + e.getMessage());
            return SetHomeResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applySetGuildHomeEffects(Player player, SetHomeResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild guild = result.guild();
        context.send(player, "guild.sethome.success", MessageType.SUCCESS,
                PlaceholderContext.of(player, guild).with("guild", guild.getName()).with("tag", guild.getTag()));
    }

    public java.util.concurrent.CompletableFuture<SetHomeResult> setGuildHomeAsync(Player player, Location loc) {
        return context.databaseExecutor.supplyAsync(() -> prepareSetGuildHome(player, loc));
    }

    public record SetHomeResult(boolean success, Guild guild, String errorKey, PlaceholderContext errorContext) {
        public static SetHomeResult error(String key, PlaceholderContext context) {
            return new SetHomeResult(false, null, key, context);
        }
    }

    public boolean teleportHome(Player player) {
        TeleportHomeResult result = prepareTeleportHome(player);
        return applyTeleportHomeEffects(player, result);
    }

    public TeleportHomeResult prepareTeleportHome(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return TeleportHomeResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            Optional<Guild> guildOpt = context.guildDao.findById(data.getGuildId());
            if (guildOpt.isEmpty()) {
                return TeleportHomeResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild guild = guildOpt.get();
            String worldName = guild.getHomeWorld();
            if (worldName == null) {
                return TeleportHomeResult.error("guild.home.error_no_home", PlaceholderContext.of(player));
            }
            return new TeleportHomeResult(true, guild, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to teleport to guild home: " + e.getMessage());
            return TeleportHomeResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public boolean applyTeleportHomeEffects(Player player, TeleportHomeResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return false;
        }
        Guild guild = result.guild();
        String worldName = guild.getHomeWorld();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            context.send(player, "guild.home.error_world_not_found", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
        Location homeLoc = new Location(world, guild.getHomeX(), guild.getHomeY(), guild.getHomeZ());

        long cooldownSeconds = context.guildConfig.getLong("settings.home.cooldown-seconds", 300);
        long lastUse = homeCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remainingMs = lastUse + cooldownSeconds * 1000 - System.currentTimeMillis();
        if (remainingMs > 0) {
            context.send(player, "guild.home.error_cooldown", MessageType.ERROR,
                    PlaceholderContext.of(player).with("seconds", Math.max(1, remainingMs / 1000)));
            return false;
        }

        int delaySeconds = context.guildConfig.getInt("settings.home.teleport-delay-seconds", 5);
        PlaceholderContext ctx = PlaceholderContext.of(player, guild)
                .with("guild", guild.getName())
                .with("tag", guild.getTag())
                .with("seconds", delaySeconds);

        if (delaySeconds <= 0) {
            player.teleport(homeLoc);
            homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            context.send(player, "guild.home.success", MessageType.SUCCESS, ctx);
            return true;
        }

        cancelHomeTeleport(player);

        context.send(player, "guild.home.teleport_start", MessageType.INFO, ctx);
        int totalTicks = delaySeconds * 20;
        AtomicInteger taskIdHolder = new AtomicInteger();
        int taskId = Bukkit.getScheduler().runTaskTimer(context.plugin, () -> {
            PendingHomeTeleport pending = homeTeleports.get(player.getUniqueId());
            if (pending == null || pending.taskId != taskIdHolder.get() || !player.isOnline()) {
                Bukkit.getScheduler().cancelTask(taskIdHolder.get());
                return;
            }
            int tick = pending.tick++;
            spawnHomeSpiralParticle(player, tick, totalTicks);
            if (tick >= totalTicks) {
                homeTeleports.remove(player.getUniqueId());
                Bukkit.getScheduler().cancelTask(taskIdHolder.get());
                player.teleport(homeLoc);
                homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                context.send(player, "guild.home.success", MessageType.SUCCESS, ctx);
            }
        }, 0L, 1L).getTaskId();
        taskIdHolder.set(taskId);
        homeTeleports.put(player.getUniqueId(), new PendingHomeTeleport(taskId, 0));
        return true;
    }

    public java.util.concurrent.CompletableFuture<TeleportHomeResult> teleportHomeAsync(Player player) {
        return context.databaseExecutor.supplyAsync(() -> prepareTeleportHome(player));
    }

    public record TeleportHomeResult(boolean success, Guild guild, String errorKey, PlaceholderContext errorContext) {
        public static TeleportHomeResult error(String key, PlaceholderContext context) {
            return new TeleportHomeResult(false, null, key, context);
        }
    }

    public void cancelHomeTeleport(Player player) {
        PendingHomeTeleport pending = homeTeleports.remove(player.getUniqueId());
        if (pending != null) {
            Bukkit.getScheduler().cancelTask(pending.taskId);
            context.send(player, "guild.home.cancelled", MessageType.ERROR, PlaceholderContext.of(player));
        }
    }

    public boolean setGuildDescription(Player player, String text) {
        SetDescriptionResult result = prepareSetGuildTextField(player, text, "description", 256, Guild::setDescription);
        applySetDescriptionEffects(player, result);
        return result.success();
    }

    private SetDescriptionResult prepareSetGuildTextField(Player player, String text, String configKey, int defaultMaxLength,
                                                          BiConsumer<Guild, String> setter) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return SetDescriptionResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return SetDescriptionResult.error("guild.description.error_no_permission", PlaceholderContext.of(player));
            }
            Optional<Guild> guildOpt = context.guildDao.findById(data.getGuildId());
            if (guildOpt.isEmpty()) {
                return SetDescriptionResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild guild = guildOpt.get();
            int maxLength = context.guildConfig.getInt("settings." + configKey + ".max-length", defaultMaxLength);
            if (text != null && !text.isBlank()) {
                String stripped = MiniMessage.miniMessage().stripTags(text);
                if (stripped.length() > maxLength) {
                    return SetDescriptionResult.error("guild.description.error_too_long",
                            PlaceholderContext.of(player).with("max", maxLength));
                }
            }
            setter.accept(guild, text != null && !text.isBlank() ? text : null);
            context.guildDao.update(guild);
            return new SetDescriptionResult(true, guild, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to update guild description: " + e.getMessage());
            return SetDescriptionResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applySetDescriptionEffects(Player player, SetDescriptionResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild guild = result.guild();
        context.send(player, "guild.description.success", MessageType.SUCCESS,
                PlaceholderContext.of(player, guild).with("guild", guild.getName()).with("tag", guild.getTag()));
    }

    public java.util.concurrent.CompletableFuture<SetDescriptionResult> setGuildDescriptionAsync(Player player, String text) {
        return context.databaseExecutor.supplyAsync(() -> prepareSetGuildTextField(player, text, "description", 256, Guild::setDescription));
    }

    public record SetDescriptionResult(boolean success, Guild guild, String errorKey, PlaceholderContext errorContext) {
        public static SetDescriptionResult error(String key, PlaceholderContext context) {
            return new SetDescriptionResult(false, null, key, context);
        }
    }

    public boolean setGuildFlag(Player player, String flagName, String value) {
        SetFlagResult result = prepareSetGuildFlag(player, flagName, value);
        applySetFlagEffects(player, result);
        return result.success();
    }

    public SetFlagResult prepareSetGuildFlag(Player player, String flagName, String value) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return SetFlagResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return SetFlagResult.error("guild.flag.error_no_permission", PlaceholderContext.of(player));
            }
            Optional<Guild> guildOpt = context.guildDao.findById(data.getGuildId());
            if (guildOpt.isEmpty()) {
                return SetFlagResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Boolean parsed = parseBoolean(value);
            if (parsed == null) {
                return SetFlagResult.error("guild.flag.error_invalid_value", PlaceholderContext.of(player).with("value", value));
            }
            Guild guild = guildOpt.get();
            String normalized = flagName.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
            String flagKey;
            switch (normalized) {
                case "public", "ispublic", "widocznosc" -> {
                    guild.setPublic(parsed);
                    flagKey = "is-public";
                }
                case "allowjoinrequests", "joinrequests", "prosby" -> {
                    guild.setAllowJoinRequests(parsed);
                    flagKey = "allow-join-requests";
                }
                case "showhome", "pokazhome" -> {
                    guild.setShowHome(parsed);
                    flagKey = "show-home";
                }
                default -> {
                    return SetFlagResult.error("guild.flag.error_invalid_flag",
                            PlaceholderContext.of(player).with("flag", flagName));
                }
            }
            context.guildDao.update(guild);
            return new SetFlagResult(true, guild, flagKey, parsed, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to set guild flag: " + e.getMessage());
            return SetFlagResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applySetFlagEffects(Player player, SetFlagResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild guild = result.guild();
        context.send(player, "guild.flag.success", MessageType.SUCCESS,
                PlaceholderContext.of(player, guild)
                        .with("guild", guild.getName())
                        .with("tag", guild.getTag())
                        .with("flag", result.flagKey())
                        .with("value", result.value() ? "włączona" : "wyłączona"));
    }

    public java.util.concurrent.CompletableFuture<SetFlagResult> setGuildFlagAsync(Player player, String flagName, String value) {
        return context.databaseExecutor.supplyAsync(() -> prepareSetGuildFlag(player, flagName, value));
    }

    public record SetFlagResult(boolean success, Guild guild, String flagKey, boolean value,
                                 String errorKey, PlaceholderContext errorContext) {
        public static SetFlagResult error(String key, PlaceholderContext context) {
            return new SetFlagResult(false, null, null, false, key, context);
        }
    }

    public boolean sendGuildInfo(Player player, String tag) {
        GuildInfoFetchResult result = fetchGuildInfo(player, tag);
        if (result.info() == null) {
            if (result.errorKey() != null) {
                context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            }
            return false;
        }
        renderGuildInfo(player, result.info(), tag);
        return true;
    }

    /**
     * Fetches guild info data. This method performs database I/O and should
     * only be called from the database executor or an async context.
     */
    public GuildInfoFetchResult fetchGuildInfo(Player player, String tag) {
        try {
            Optional<Guild> guildOpt;
            if (tag == null) {
                PlayerData data = context.getOrCreatePlayerData(player);
                if (data.getGuildId() == null) {
                    return GuildInfoFetchResult.error("guild.no_guild", PlaceholderContext.of(player));
                }
                guildOpt = context.guildDao.findById(data.getGuildId());
            } else {
                guildOpt = context.guildDao.findByTag(tag.trim().toUpperCase());
            }
            if (guildOpt.isEmpty()) {
                return GuildInfoFetchResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild guild = guildOpt.get();
            Optional<PlayerData> viewerDataOpt = context.playerDao.findByUuid(player.getUniqueId());
            boolean isMember = viewerDataOpt.map(d -> d.getGuildId() != null && d.getGuildId().equals(guild.getId())).orElse(false);
            if (tag != null && !isMember && !guild.isPublic()) {
                return GuildInfoFetchResult.error("guild.info.error_private",
                        PlaceholderContext.of(player).with("tag", tag));
            }
            List<PlayerData> members = context.playerDao.findByGuild(guild.getId());
            return GuildInfoFetchResult.success(new GuildInfo(guild, members, isMember));
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to show guild info: " + e.getMessage());
            return GuildInfoFetchResult.error("guild.create.error_generic", PlaceholderContext.of(player));
        }
    }

    /**
     * Renders guild info messages to the player. Must be called from the main server thread.
     */
    public void renderGuildInfo(Player player, GuildInfo info, String requestedTag) {
        Guild guild = info.guild();
        List<PlayerData> members = info.members();
        PlayerData leaderData = members.stream().filter(m -> m.getRank() == GuildRank.LEADER).findFirst().orElse(null);
        String leaderName = leaderData != null ? leaderData.getName() : context.langManager.getRawMessage("placeholders.not_available");
        PlaceholderContext ctx = PlaceholderContext.of(player, guild)
                .with("guild", guild.getName())
                .with("tag", guild.getTag())
                .with("leader", leaderName != null ? leaderName : "N/A")
                .with("points", guild.getPoints())
                .with("level", guild.getLevel())
                .with("members", members.size())
                .with("date", context.formatDate(guild.getCreatedAt()));
        context.send(player, "guild.info.header", MessageType.RAW, ctx);
        context.send(player, "guild.info.leader", MessageType.RAW, ctx);
        if (guild.getDescription() != null && !guild.getDescription().isBlank()) {
            context.send(player, "guild.info.description", MessageType.RAW, ctx.with("description", guild.getDescription()));
        }
        if (info.viewerIsMember() || guild.isShowHome()) {
            String homeWorld = guild.getHomeWorld() != null ? guild.getHomeWorld() : context.langManager.getRawMessage("placeholders.not_available");
            context.send(player, "guild.info.home", MessageType.RAW,
                    ctx.with("home_world", homeWorld)
                            .with("home_x", String.format("%.1f", guild.getHomeX()))
                            .with("home_y", String.format("%.1f", guild.getHomeY()))
                            .with("home_z", String.format("%.1f", guild.getHomeZ())));
        }
        context.send(player, "guild.info.points", MessageType.RAW, ctx);
        context.send(player, "guild.info.level", MessageType.RAW, ctx);
        context.send(player, "guild.info.members", MessageType.RAW, ctx);
        context.send(player, "guild.info.flags", MessageType.RAW,
                ctx.with("is_public", guild.isPublic() ? "tak" : "nie")
                        .with("allow_join_requests", guild.isAllowJoinRequests() ? "tak" : "nie")
                        .with("show_home", guild.isShowHome() ? "tak" : "nie"));
        context.send(player, "guild.info.created", MessageType.RAW, ctx);
    }

    private Boolean parseBoolean(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes", "1", "wlaczone", "włączone", "tak" -> true;
            case "false", "off", "no", "0", "wylaczone", "wyłączone", "nie" -> false;
            default -> null;
        };
    }

    private void spawnHomeSpiralParticle(Player player, int tick, int totalTicks) {
        Location center = player.getLocation().clone();
        double progress = tick / (double) totalTicks;
        double height = progress * (player.getHeight() + 0.2);
        double angle = tick * 0.5;
        double radius = 0.6;
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        Location particleLoc = center.add(x, height, z);
        player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
    }

    private static class PendingHomeTeleport {
        final int taskId;
        int tick;

        PendingHomeTeleport(int taskId, int tick) {
            this.taskId = taskId;
            this.tick = tick;
        }
    }

}
