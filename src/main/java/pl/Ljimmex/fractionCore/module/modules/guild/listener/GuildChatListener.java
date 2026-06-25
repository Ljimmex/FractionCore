package pl.Ljimmex.fractionCore.module.modules.guild.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.config.ModuleConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.GuildRelationManager;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.PlayerGuildCache;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class GuildChatListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDao playerDao;
    private final GuildDao guildDao;
    private final ModuleConfig guildConfig;
    private final GuildRelationManager relationManager;
    private final PlayerGuildCache playerGuildCache;

    public GuildChatListener(JavaPlugin plugin, PlayerDao playerDao, GuildDao guildDao, ModuleConfig guildConfig,
                             GuildRelationManager relationManager, PlayerGuildCache playerGuildCache) {
        this.plugin = plugin;
        this.playerDao = playerDao;
        this.guildDao = guildDao;
        this.guildConfig = guildConfig;
        this.relationManager = relationManager;
        this.playerGuildCache = playerGuildCache;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        if (!guildConfig.getBoolean("chat.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        Optional<PlayerGuildCache.Entry> sourceCached = playerGuildCache.get(player.getUniqueId());
        UUID sourceGuildId;
        GuildRank rank;
        if (sourceCached.isPresent()) {
            sourceGuildId = sourceCached.get().guildId();
            rank = sourceCached.get().rank();
        } else {
            try {
                Optional<PlayerData> dataOpt = playerDao.findByUuid(player.getUniqueId());
                if (dataOpt.isEmpty() || dataOpt.get().getGuildId() == null) {
                    return;
                }
                PlayerData data = dataOpt.get();
                playerGuildCache.refresh(player.getUniqueId(), data);
                sourceGuildId = data.getGuildId();
                rank = data.getRank();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to format guild chat for " + player.getName() + ": " + e.getMessage());
                return;
            }
        }
        Optional<Guild> guildOpt;
        try {
            guildOpt = guildDao.findById(sourceGuildId);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to format guild chat for " + player.getName() + ": " + e.getMessage());
            return;
        }
        if (guildOpt.isEmpty()) {
            return;
        }
        Guild guild = guildOpt.get();

        String format = guildConfig.getString("chat.format", "<dark_gray>[{tag}]{rank_letter} <white>{player}<gray>: ");

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            UUID viewerGuildId = null;
            if (viewer instanceof Player viewerPlayer) {
                Optional<PlayerGuildCache.Entry> cached = playerGuildCache.get(viewerPlayer.getUniqueId());
                if (cached.isPresent()) {
                    viewerGuildId = cached.get().guildId();
                } else {
                    try {
                        Optional<PlayerData> viewerDataOpt = playerDao.findByUuid(viewerPlayer.getUniqueId());
                        if (viewerDataOpt.isPresent()) {
                            viewerGuildId = viewerDataOpt.get().getGuildId();
                        }
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Failed to resolve viewer guild for chat: " + e.getMessage());
                    }
                }
            }
            boolean sameGuild = sourceGuildId.equals(viewerGuildId);
            Component prefix = buildPrefix(format, player, guild, rank, viewerGuildId, sameGuild);
            return prefix.append(message);
        });
    }

    private Component buildPrefix(String format, Player player, Guild guild, GuildRank rank, UUID viewerGuildId, boolean includeRankLetter) {
        String rankLetter = getRankLetter(rank);
        String rankLetterPart = includeRankLetter ? getRankLetterColor(rank) + "[" + rankLetter + "]<reset>" : "";
        String tagColor = relationManager.getColor(viewerGuildId, guild.getId());
        String processed = format
                .replace("{tag}", tagColor + guild.getTag() + closeColorTag(tagColor))
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

    private String closeColorTag(String color) {
        if (color == null || color.isBlank()) {
            return "<reset>";
        }
        String trimmed = color.trim();
        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).toLowerCase();
            if (inner.matches("[a-z_]+") && !inner.startsWith("#")) {
                return "</" + inner + ">";
            }
        }
        return "<reset>";
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
