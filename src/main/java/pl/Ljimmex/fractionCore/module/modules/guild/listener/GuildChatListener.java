package pl.Ljimmex.fractionCore.module.modules.guild.listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class GuildChatListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDao playerDao;
    private final GuildDao guildDao;
    private final FileConfiguration guildConfig;

    public GuildChatListener(JavaPlugin plugin, PlayerDao playerDao, GuildDao guildDao, FileConfiguration guildConfig) {
        this.plugin = plugin;
        this.playerDao = playerDao;
        this.guildDao = guildDao;
        this.guildConfig = guildConfig;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        if (!guildConfig.getBoolean("chat.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        try {
            Optional<PlayerData> dataOpt = playerDao.findByUuid(player.getUniqueId());
            if (dataOpt.isEmpty() || dataOpt.get().getGuildId() == null) {
                return;
            }
            PlayerData data = dataOpt.get();
            UUID sourceGuildId = data.getGuildId();
            Optional<Guild> guildOpt = guildDao.findById(sourceGuildId);
            if (guildOpt.isEmpty()) {
                return;
            }
            Guild guild = guildOpt.get();
            GuildRank rank = data.getRank();

            String format = guildConfig.getString("chat.format", "<dark_gray>[<aqua>{tag}</aqua>]{rank_letter} <white>{player}<gray>: ");
            Component publicPrefix = buildPrefix(format, player, guild, rank, false);
            Component guildPrefix = buildPrefix(format, player, guild, rank, true);

            // Guild tag is visible to everyone on the global chat.
            // Rank letter is visible only to members of the same guild.
            event.renderer((source, sourceDisplayName, message, viewer) -> {
                if (viewer instanceof Player viewerPlayer) {
                    try {
                        Optional<PlayerData> viewerDataOpt = playerDao.findByUuid(viewerPlayer.getUniqueId());
                        if (viewerDataOpt.isPresent() && sourceGuildId.equals(viewerDataOpt.get().getGuildId())) {
                            return guildPrefix.append(message);
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Failed to resolve viewer guild for chat: " + e.getMessage());
                    }
                }
                return publicPrefix.append(message);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to format guild chat for " + player.getName() + ": " + e.getMessage());
        }
    }

    private Component buildPrefix(String format, Player player, Guild guild, GuildRank rank, boolean includeRankLetter) {
        String rankLetter = getRankLetter(rank);
        String rankLetterPart = includeRankLetter ? getRankLetterColor(rank) + rankLetter + "<reset>" : "";
        String processed = format
                .replace("{tag}", guild.getTag())
                .replace("{guild}", guild.getName())
                .replace("{rank_letter}", rankLetterPart)
                .replace("{rank}", "")
                .replace("{player}", player.getName());
        try {
            return MiniMessage.miniMessage().deserialize(processed);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse chat format, using fallback: " + e.getMessage());
            if (includeRankLetter) {
                return Component.text("[" + guild.getTag() + "][" + rankLetter + "] " + player.getName() + ": ");
            }
            return Component.text("[" + guild.getTag() + "] " + player.getName() + ": ");
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
