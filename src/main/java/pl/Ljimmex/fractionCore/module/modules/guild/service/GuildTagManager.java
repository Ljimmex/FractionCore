package pl.Ljimmex.fractionCore.module.modules.guild.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.config.ModuleConfig;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.GuildRelationManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GuildTagManager {

    private final PlayerDao playerDao;
    private final GuildDao guildDao;
    private final ModuleConfig guildConfig;
    private final GuildRelationManager relationManager;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public GuildTagManager(PlayerDao playerDao, GuildDao guildDao, ModuleConfig guildConfig,
                           GuildRelationManager relationManager) {
        this.playerDao = playerDao;
        this.guildDao = guildDao;
        this.guildConfig = guildConfig;
        this.relationManager = relationManager;
    }

    /**
     * Refreshes the guild tag visibility for a single observer.
     * Every online player with a guild is shown with a tag colored by relation.
     */
    public void updatePlayerTag(Player player) {
        Map<UUID, PlayerData> onlineData = loadOnlinePlayerData();
        updatePlayerTag(player, onlineData);
    }

    private void updatePlayerTag(Player player, Map<UUID, PlayerData> onlineData) {
        PlayerData observerData = onlineData.get(player.getUniqueId());
        UUID observerGuildId = observerData != null ? observerData.getGuildId() : null;

        Scoreboard scoreboard = getOrCreateScoreboard(player);
        Set<String> activeEntries = new HashSet<>();

        for (Player target : Bukkit.getOnlinePlayers()) {
            PlayerData targetData = onlineData.get(target.getUniqueId());
            if (targetData == null || targetData.getGuildId() == null) {
                continue;
            }
            UUID targetGuildId = targetData.getGuildId();
            Optional<Guild> guildOpt;
            try {
                guildOpt = guildDao.findById(targetGuildId);
            } catch (SQLException e) {
                player.getServer().getLogger().warning("Failed to load guild for tag update: " + e.getMessage());
                continue;
            }
            if (guildOpt.isEmpty()) {
                continue;
            }
            Guild guild = guildOpt.get();
            boolean sameGuild = observerGuildId != null && observerGuildId.equals(targetGuildId);
            GuildRank rank = targetData.getRank();
            String rankLetter = sameGuild ? getRankLetter(rank) : "";

            Team team = getOrCreateTeam(scoreboard, target.getUniqueId());
            team.prefix(buildTeamPrefix(guild.getTag(), observerGuildId, targetGuildId, rank, rankLetter));
            team.color(resolveNameplateColor(observerGuildId, targetGuildId));
            team.addEntry(target.getName());
            activeEntries.add(target.getName());
        }

        // Remove stale entries and unregister empty teams
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            for (String entry : new HashSet<>(team.getEntries())) {
                if (!activeEntries.contains(entry)) {
                    team.removeEntry(entry);
                }
            }
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    /**
     * Refreshes tags for all online members of the given guild.
     */
    public void updateTagsForGuild(UUID guildId) {
        if (guildId == null) {
            return;
        }
        Map<UUID, PlayerData> onlineData = loadOnlinePlayerData();
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerData data = onlineData.get(online.getUniqueId());
            if (data != null && guildId.equals(data.getGuildId())) {
                updatePlayerTag(online, onlineData);
            }
        }
    }

    /**
     * Refreshes tags for all online players.
     */
    public void updateAllTags() {
        Map<UUID, PlayerData> onlineData = loadOnlinePlayerData();
        for (Player online : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            updatePlayerTag(online, onlineData);
        }
    }

    public void clearPlayerTag(Player player) {
        Scoreboard scoreboard = scoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            for (Team team : new ArrayList<>(scoreboard.getTeams())) {
                team.unregister();
            }
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
        player.displayName(null);
    }

    private Map<UUID, PlayerData> loadOnlinePlayerData() {
        Map<UUID, PlayerData> map = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                Optional<PlayerData> dataOpt = playerDao.findByUuid(player.getUniqueId());
                dataOpt.ifPresent(data -> map.put(player.getUniqueId(), data));
            } catch (SQLException e) {
                player.getServer().getLogger().warning("Failed to load player data for tag update: " + e.getMessage());
            }
        }
        return map;
    }

    private Scoreboard getOrCreateScoreboard(Player player) {
        return scoreboards.computeIfAbsent(player.getUniqueId(), uuid -> {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) {
                return player.getScoreboard();
            }
            Scoreboard scoreboard = manager.getNewScoreboard();
            player.setScoreboard(scoreboard);
            return scoreboard;
        });
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, UUID targetUuid) {
        String teamName = buildTeamName(targetUuid);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        return team;
    }

    private String buildTeamName(UUID targetUuid) {
        String hex = targetUuid.toString().replace("-", "");
        String suffix = hex.substring(Math.max(0, hex.length() - 12));
        String name = "fgc" + suffix;
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    private Component buildTeamPrefix(String tag, UUID observerGuildId, UUID targetGuildId, GuildRank rank, String rankLetter) {
        String color = relationManager.getColor(observerGuildId, targetGuildId);
        String colorPrefix = color != null ? color : "";
        String full;
        if (rankLetter != null && !rankLetter.isBlank()) {
            String rankColor = getRankLetterColor(rank);
            full = colorPrefix + "[" + tag + "]" + rankColor + "[" + rankLetter + "]<reset> ";
        } else {
            full = colorPrefix + "[" + tag + "]<reset> ";
        }
        try {
            String normalized = full.replace('§', '&');
            if (normalized.contains("&")) {
                normalized = legacyToMiniMessage(normalized);
            }
            return MiniMessage.miniMessage().deserialize(normalized);
        } catch (Exception e) {
            return Component.text("[" + tag + "]" + (rankLetter == null || rankLetter.isBlank() ? "" : "[" + rankLetter + "]") + " ");
        }
    }

    private String legacyToMiniMessage(String input) {
        return input
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }

    private NamedTextColor resolveNameplateColor(UUID observerGuildId, UUID targetGuildId) {
        String colorString = relationManager.getColor(observerGuildId, targetGuildId);
        if (colorString == null || colorString.isBlank()) {
            return NamedTextColor.WHITE;
        }

        String normalized = colorString.toLowerCase(Locale.ROOT).replace("§", "&");
        NamedTextColor named = switch (normalized) {
            case "<black>", "&0" -> NamedTextColor.BLACK;
            case "<dark_blue>", "&1" -> NamedTextColor.DARK_BLUE;
            case "<dark_green>", "&2" -> NamedTextColor.DARK_GREEN;
            case "<dark_aqua>", "&3" -> NamedTextColor.DARK_AQUA;
            case "<dark_red>", "&4" -> NamedTextColor.DARK_RED;
            case "<dark_purple>", "&5" -> NamedTextColor.DARK_PURPLE;
            case "<gold>", "&6" -> NamedTextColor.GOLD;
            case "<gray>", "&7" -> NamedTextColor.GRAY;
            case "<dark_gray>", "&8" -> NamedTextColor.DARK_GRAY;
            case "<blue>", "&9" -> NamedTextColor.BLUE;
            case "<green>", "&a" -> NamedTextColor.GREEN;
            case "<aqua>", "&b" -> NamedTextColor.AQUA;
            case "<red>", "&c" -> NamedTextColor.RED;
            case "<light_purple>", "&d" -> NamedTextColor.LIGHT_PURPLE;
            case "<yellow>", "&e" -> NamedTextColor.YELLOW;
            case "<white>", "&f" -> NamedTextColor.WHITE;
            default -> null;
        };
        if (named != null) {
            return named;
        }

        // Try hex colors (<#RRGGBB>, &#RRGGBB, #RRGGBB)
        String hex = colorString.replaceAll("[<>]", "").replace("&", "");
        if (hex.startsWith("#")) {
            TextColor textColor = TextColor.fromHexString(hex);
            if (textColor != null) {
                return NamedTextColor.nearestTo(textColor);
            }
        }

        return NamedTextColor.WHITE;
    }

    private String getRankLetter(GuildRank rank) {
        if (rank == null) {
            return "";
        }
        String configKey = rankConfigKey(rank);
        String defaultLetter = switch (rank) {
            case LEADER -> "L";
            case CO_LEADER -> "Z";
            case MODERATOR -> "M";
            case MEMBER -> "C";
            case RECRUIT -> "R";
        };
        String letter = guildConfig.getString("chat.rank-letters." + configKey, defaultLetter);
        return letter != null && !letter.isBlank() ? letter.toUpperCase(Locale.ROOT) : defaultLetter;
    }

    private String getRankLetterColor(GuildRank rank) {
        if (rank == null) {
            return "<white>";
        }
        String configKey = rankConfigKey(rank);
        String defaultColor = switch (rank) {
            case LEADER -> "<gold>";
            case CO_LEADER -> "<aqua>";
            case MODERATOR -> "<green>";
            case MEMBER -> "<gray>";
            case RECRUIT -> "<dark_gray>";
        };
        String color = guildConfig.getString("chat.rank-letter-colors." + configKey, defaultColor);
        return color != null && !color.isBlank() ? color : defaultColor;
    }

    private String rankConfigKey(GuildRank rank) {
        return switch (rank) {
            case LEADER -> "leader";
            case CO_LEADER -> "coleader";
            case MODERATOR -> "moderator";
            case MEMBER -> "member";
            case RECRUIT -> "recruit";
        };
    }
}
