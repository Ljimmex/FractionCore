package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GuildRankManager {

    private final GuildContext context;

    public GuildRankManager(GuildContext context) {
        this.context = context;
    }

    public boolean promotePlayer(Player promoter, String targetName, String targetRankName) {
        try {
            PlayerData promoterData = context.getOrCreatePlayerData(promoter);
            if (promoterData.getGuildId() == null) {
                context.send(promoter, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(promoter));
                return false;
            }
            GuildRank promoterRank = promoterData.getRank();
            if (!context.isLeaderOrCoLeader(promoterRank)) {
                context.send(promoter, "guild.promote.error_no_permission", MessageType.ERROR, PlaceholderContext.of(promoter));
                return false;
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(promoterData.getGuildId())) {
                context.send(promoter, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(promoter).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            if (!canManageRank(promoterRank, targetData.getRank())) {
                context.send(promoter, "guild.promote.error_target_higher_rank", MessageType.ERROR,
                        PlaceholderContext.of(promoter).with("player", targetData.getName()));
                return false;
            }
            GuildRank oldRank = targetData.getRank();
            GuildRank targetRank;
            if (targetRankName != null && !targetRankName.isBlank()) {
                Optional<GuildRank> parsed = resolveRankByName(targetRankName);
                if (parsed.isEmpty()) {
                    context.send(promoter, "guild.promote.error_invalid_rank", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()).with("rank", targetRankName));
                    return false;
                }
                targetRank = parsed.get();
                if (targetRank == oldRank) {
                    context.send(promoter, "guild.promote.error_same_rank", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                    return false;
                }
                if (getRankWeight(targetRank) <= getRankWeight(oldRank)) {
                    context.send(promoter, "guild.promote.error_target_lower_rank", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                    return false;
                }
                if (targetRank == GuildRank.LEADER) {
                    context.send(promoter, "guild.promote.error_leader_promote", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()));
                    return false;
                }
            } else {
                targetRank = getNextRank(oldRank);
                if (targetRank == null) {
                    context.send(promoter, "guild.promote.error_max_rank", MessageType.ERROR,
                            PlaceholderContext.of(promoter).with("player", targetData.getName()));
                    return false;
                }
            }
            targetData.setRank(targetRank);
            context.playerDao.update(targetData);

            UUID guildId = promoterData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            context.send(promoter, "guild.promote.success", MessageType.SUCCESS,
                    PlaceholderContext.of(promoter)
                            .with("player", targetData.getName())
                            .with("old_rank", oldRank != null ? oldRank.getDisplayName() : "?")
                            .with("rank", targetRank.getDisplayName()));

            Component broadcast = context.langManager.getMessage("guild.promote.broadcast", MessageType.INFO,
                    PlaceholderContext.empty()
                            .with("player", targetData.getName())
                            .with("old_rank", oldRank != null ? oldRank.getDisplayName() : "?")
                            .with("rank", targetRank.getDisplayName())
                            .with("guild", guildName)
                            .with("tag", tag));
            context.broadcastToGuild(guildId, broadcast, promoter);

            Player targetOnline = Bukkit.getPlayer(targetData.getUuid());
            if (targetOnline != null) {
                context.send(targetOnline, "guild.promote.target_message", MessageType.INFO,
                        PlaceholderContext.of(targetOnline).with("rank", targetRank.getDisplayName()).with("guild", guildName).with("tag", tag));
            }
            context.tagManager.updateTagsForGuild(guildId);
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to promote player: " + e.getMessage());
            context.send(promoter, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(promoter));
            return false;
        }
    }

    public boolean demotePlayer(Player demoter, String targetName, String targetRankName) {
        try {
            PlayerData demoterData = context.getOrCreatePlayerData(demoter);
            if (demoterData.getGuildId() == null) {
                context.send(demoter, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(demoter));
                return false;
            }
            GuildRank demoterRank = demoterData.getRank();
            if (!context.isLeaderOrCoLeader(demoterRank)) {
                context.send(demoter, "guild.demote.error_no_permission", MessageType.ERROR, PlaceholderContext.of(demoter));
                return false;
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(demoterData.getGuildId())) {
                context.send(demoter, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(demoter).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            if (!canManageRank(demoterRank, targetData.getRank())) {
                context.send(demoter, "guild.demote.error_target_higher_rank", MessageType.ERROR,
                        PlaceholderContext.of(demoter).with("player", targetData.getName()));
                return false;
            }
            GuildRank oldRank = targetData.getRank();
            if (oldRank == GuildRank.LEADER) {
                context.send(demoter, "guild.demote.error_leader_demote", MessageType.ERROR,
                        PlaceholderContext.of(demoter).with("player", targetData.getName()));
                return false;
            }
            GuildRank targetRank;
            if (targetRankName != null && !targetRankName.isBlank()) {
                Optional<GuildRank> parsed = resolveRankByName(targetRankName);
                if (parsed.isEmpty()) {
                    context.send(demoter, "guild.demote.error_invalid_rank", MessageType.ERROR,
                            PlaceholderContext.of(demoter).with("player", targetData.getName()).with("rank", targetRankName));
                    return false;
                }
                targetRank = parsed.get();
                if (targetRank == oldRank) {
                    context.send(demoter, "guild.demote.error_same_rank", MessageType.ERROR,
                            PlaceholderContext.of(demoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                    return false;
                }
                if (getRankWeight(targetRank) >= getRankWeight(oldRank)) {
                    context.send(demoter, "guild.demote.error_target_rank_higher", MessageType.ERROR,
                            PlaceholderContext.of(demoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                    return false;
                }
            } else {
                targetRank = getPreviousRank(oldRank);
                if (targetRank == null) {
                    context.send(demoter, "guild.demote.error_min_rank", MessageType.ERROR,
                            PlaceholderContext.of(demoter).with("player", targetData.getName()));
                    return false;
                }
            }
            targetData.setRank(targetRank);
            context.playerDao.update(targetData);

            UUID guildId = demoterData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");

            context.send(demoter, "guild.demote.success", MessageType.SUCCESS,
                    PlaceholderContext.of(demoter)
                            .with("player", targetData.getName())
                            .with("old_rank", oldRank != null ? oldRank.getDisplayName() : "?")
                            .with("rank", targetRank.getDisplayName()));

            Component broadcast = context.langManager.getMessage("guild.demote.broadcast", MessageType.INFO,
                    PlaceholderContext.empty()
                            .with("player", targetData.getName())
                            .with("old_rank", oldRank != null ? oldRank.getDisplayName() : "?")
                            .with("rank", targetRank.getDisplayName())
                            .with("guild", guildName)
                            .with("tag", tag));
            context.broadcastToGuild(guildId, broadcast, demoter);

            Player targetOnline = Bukkit.getPlayer(targetData.getUuid());
            if (targetOnline != null) {
                context.send(targetOnline, "guild.demote.target_message", MessageType.INFO,
                        PlaceholderContext.of(targetOnline).with("rank", targetRank.getDisplayName()).with("guild", guildName).with("tag", tag));
            }
            context.tagManager.updateTagsForGuild(guildId);
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to demote player: " + e.getMessage());
            context.send(demoter, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(demoter));
            return false;
        }
    }

    public boolean transferLeadership(Player leader, String targetName) {
        try {
            PlayerData leaderData = context.getOrCreatePlayerData(leader);
            if (leaderData.getGuildId() == null) {
                context.send(leader, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(leader));
                return false;
            }
            if (leaderData.getRank() != GuildRank.LEADER) {
                context.send(leader, "guild.leader.error_no_permission", MessageType.ERROR, PlaceholderContext.of(leader));
                return false;
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(leaderData.getGuildId())) {
                context.send(leader, "guild.not_member", MessageType.ERROR, PlaceholderContext.of(leader).with("player", targetName));
                return false;
            }
            PlayerData targetData = targetOpt.get();
            if (targetData.getUuid().equals(leader.getUniqueId())) {
                context.send(leader, "guild.leader.error_self", MessageType.ERROR, PlaceholderContext.of(leader));
                return false;
            }
            UUID guildId = leaderData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            if (guildOpt.isEmpty()) {
                context.send(leader, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(leader));
                return false;
            }
            Guild guild = guildOpt.get();

            leaderData.setRank(GuildRank.CO_LEADER);
            targetData.setRank(GuildRank.LEADER);
            guild.setLeaderUuid(targetData.getUuid());
            context.playerDao.update(leaderData);
            context.playerDao.update(targetData);
            context.guildDao.update(guild);

            context.tagManager.updateTagsForGuild(guildId);
            Player targetOnline = Bukkit.getPlayer(targetData.getUuid());

            context.send(leader, "guild.leader.success", MessageType.SUCCESS,
                    PlaceholderContext.of(leader).with("player", targetData.getName()));
            Component broadcast = context.langManager.getMessage("guild.leader.broadcast", MessageType.INFO,
                    PlaceholderContext.empty().with("player", targetData.getName()).with("guild", guild.getName()).with("tag", guild.getTag()));
            context.broadcastToGuild(guildId, broadcast, leader);
            if (targetOnline != null) {
                context.send(targetOnline, "guild.leader.target_message", MessageType.INFO,
                        PlaceholderContext.of(targetOnline).with("guild", guild.getName()).with("tag", guild.getTag()));
            }
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to transfer leadership: " + e.getMessage());
            context.send(leader, "guild.create.error_generic", MessageType.ERROR, PlaceholderContext.of(leader));
            return false;
        }
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
        return context.guildConfig.getInt("ranks." + key + ".weight", rank.ordinal() * 20);
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
            String configName = context.guildConfig.getString("ranks." + configKey + ".name", rank.getDisplayName());
            if (configName != null && !configName.isBlank()) {
                map.put(normalizeRankInput(configName), rank);
            }
        }
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

}
