package pl.Ljimmex.fractionCore.module.modules.guild.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;

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
    private final FileConfiguration guildConfig;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public GuildTagManager(PlayerDao playerDao, GuildDao guildDao, FileConfiguration guildConfig) {
        this.playerDao = playerDao;
        this.guildDao = guildDao;
        this.guildConfig = guildConfig;
    }

    /**
     * Refreshes the guild tag visibility for a single observer.
     * The tag (with rank letter) is visible only to members of the same guild.
     */
    public void updatePlayerTag(Player player) {
        try {
            Optional<PlayerData> dataOpt = playerDao.findByUuid(player.getUniqueId());
            UUID observerGuildId = dataOpt.map(PlayerData::getGuildId).orElse(null);

            Scoreboard scoreboard = getOrCreateScoreboard(player);
            clearGuildTeams(scoreboard);

            if (observerGuildId == null) {
                return;
            }

            Optional<Guild> guildOpt = guildDao.findById(observerGuildId);
            if (guildOpt.isEmpty()) {
                return;
            }

            Guild guild = guildOpt.get();

            for (Player target : Bukkit.getOnlinePlayers()) {
                Optional<PlayerData> targetOpt = playerDao.findByUuid(target.getUniqueId());
                if (targetOpt.isEmpty()) {
                    continue;
                }
                PlayerData targetData = targetOpt.get();
                if (!observerGuildId.equals(targetData.getGuildId())) {
                    continue;
                }

                GuildRank rank = targetData.getRank();
                String rankLetter = getRankLetter(rank);
                Team team = getOrCreateTeam(scoreboard, observerGuildId, rankLetter);
                team.prefix(buildTeamPrefix(guild.getTag(), guild.getColor(), rank, rankLetter));
                team.color(NamedTextColor.WHITE);
                team.addEntry(target.getName());
            }
        } catch (SQLException e) {
            player.getServer().getLogger().severe("Failed to update guild tag for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Refreshes tags for all online members of the given guild.
     */
    public void updateTagsForGuild(UUID guildId) {
        if (guildId == null) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            try {
                Optional<PlayerData> dataOpt = playerDao.findByUuid(online.getUniqueId());
                if (dataOpt.isPresent() && guildId.equals(dataOpt.get().getGuildId())) {
                    updatePlayerTag(online);
                }
            } catch (SQLException e) {
                online.getServer().getLogger().severe("Failed to refresh guild tags: " + e.getMessage());
            }
        }
    }

    /**
     * Refreshes tags for all online players.
     */
    public void updateAllTags() {
        for (Player online : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            updatePlayerTag(online);
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

    private void clearGuildTeams(Scoreboard scoreboard) {
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            for (String entry : new HashSet<>(team.getEntries())) {
                team.removeEntry(entry);
            }
            team.unregister();
        }
    }

    private Team getOrCreateTeam(Scoreboard scoreboard, UUID guildId, String rankLetter) {
        String teamName = buildTeamName(guildId, rankLetter);
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        return team;
    }

    private String buildTeamName(UUID guildId, String rankLetter) {
        String hex = guildId.toString().replace("-", "");
        String suffix = hex.substring(Math.max(0, hex.length() - 12)) + rankLetter;
        String name = "fgc" + suffix;
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    private Component buildTeamPrefix(String tag, String color, GuildRank rank, String rankLetter) {
        String colorPrefix = color != null ? color : "";
        String rankColor = getRankLetterColor(rank);
        String full = colorPrefix + "[" + tag + "]" + rankColor + "[" + rankLetter + "]<reset> ";
        if (full.contains("&") || full.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(full);
        }
        try {
            return MiniMessage.miniMessage().deserialize(full);
        } catch (Exception e) {
            return Component.text("[" + tag + "][" + rankLetter + "] ");
        }
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
