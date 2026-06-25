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
    private final GuildRankHelper rankHelper;

    public GuildRankManager(GuildContext context) {
        this.context = context;
        this.rankHelper = new GuildRankHelper(context);
    }

    public boolean promotePlayer(Player promoter, String targetName, String targetRankName) {
        RankChangeResult result = preparePromotePlayer(promoter, targetName, targetRankName);
        applyRankChangeEffects(promoter, result, "guild.promote");
        return result.success();
    }

    public RankChangeResult preparePromotePlayer(Player promoter, String targetName, String targetRankName) {
        try {
            PlayerData promoterData = context.getOrCreatePlayerData(promoter);
            if (promoterData.getGuildId() == null) {
                return RankChangeResult.error("guild.no_guild", PlaceholderContext.of(promoter));
            }
            GuildRank promoterRank = promoterData.getRank();
            if (!context.isLeaderOrCoLeader(promoterRank)) {
                return RankChangeResult.error("guild.promote.error_no_permission", PlaceholderContext.of(promoter));
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(promoterData.getGuildId())) {
                return RankChangeResult.error("guild.not_member", PlaceholderContext.of(promoter).with("player", targetName));
            }
            PlayerData targetData = targetOpt.get();
            if (!canManageRank(promoterRank, targetData.getRank())) {
                return RankChangeResult.error("guild.promote.error_target_higher_rank",
                        PlaceholderContext.of(promoter).with("player", targetData.getName()));
            }
            GuildRank oldRank = targetData.getRank();
            GuildRank targetRank;
            if (targetRankName != null && !targetRankName.isBlank()) {
                Optional<GuildRank> parsed = resolveRankByName(targetRankName);
                if (parsed.isEmpty()) {
                    return RankChangeResult.error("guild.promote.error_invalid_rank",
                            PlaceholderContext.of(promoter).with("player", targetData.getName()).with("rank", targetRankName));
                }
                targetRank = parsed.get();
                if (targetRank == oldRank) {
                    return RankChangeResult.error("guild.promote.error_same_rank",
                            PlaceholderContext.of(promoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                }
                if (getRankWeight(targetRank) <= getRankWeight(oldRank)) {
                    return RankChangeResult.error("guild.promote.error_target_lower_rank",
                            PlaceholderContext.of(promoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                }
                if (targetRank == GuildRank.LEADER) {
                    return RankChangeResult.error("guild.promote.error_leader_promote",
                            PlaceholderContext.of(promoter).with("player", targetData.getName()));
                }
            } else {
                targetRank = getNextRank(oldRank);
                if (targetRank == null) {
                    return RankChangeResult.error("guild.promote.error_max_rank",
                            PlaceholderContext.of(promoter).with("player", targetData.getName()));
                }
            }
            targetData.setRank(targetRank);
            context.playerDao.update(targetData);

            UUID guildId = promoterData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");
            return new RankChangeResult(true, targetData.getUuid(), targetData.getName(), oldRank, targetRank, guildId, guildName, tag, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to promote player: " + e.getMessage());
            return RankChangeResult.error("guild.create.error_generic", PlaceholderContext.of(promoter));
        }
    }

    public void applyRankChangeEffects(Player promoter, RankChangeResult result, String langPrefix) {
        if (!result.success()) {
            context.send(promoter, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        context.getPlayerGuildCache().refresh(result.targetUuid(),
                new PlayerData(result.targetUuid(), result.targetName(), result.guildId(), result.newRank(), 0, 0, 0, 0, 0, 0));

        context.send(promoter, langPrefix + ".success", MessageType.SUCCESS,
                PlaceholderContext.of(promoter)
                        .with("player", result.targetName())
                        .with("old_rank", result.oldRank() != null ? result.oldRank().getDisplayName() : "?")
                        .with("rank", result.newRank().getDisplayName()));

        Component broadcast = context.langManager.getMessage(langPrefix + ".broadcast", MessageType.INFO,
                PlaceholderContext.empty()
                        .with("player", result.targetName())
                        .with("old_rank", result.oldRank() != null ? result.oldRank().getDisplayName() : "?")
                        .with("rank", result.newRank().getDisplayName())
                        .with("guild", result.guildName())
                        .with("tag", result.tag()));
        context.broadcastToGuild(result.guildId(), broadcast, promoter);

        Player targetOnline = Bukkit.getPlayer(result.targetUuid());
        if (targetOnline != null) {
            context.send(targetOnline, langPrefix + ".target_message", MessageType.INFO,
                    PlaceholderContext.of(targetOnline).with("rank", result.newRank().getDisplayName()).with("guild", result.guildName()).with("tag", result.tag()));
        }
        context.tagManager.updateTagsForGuild(result.guildId());
    }

    public java.util.concurrent.CompletableFuture<RankChangeResult> promotePlayerAsync(Player promoter, String targetName, String targetRankName) {
        return context.databaseExecutor.supplyAsync(() -> preparePromotePlayer(promoter, targetName, targetRankName));
    }

    public record RankChangeResult(boolean success, UUID targetUuid, String targetName,
                                    GuildRank oldRank, GuildRank newRank,
                                    UUID guildId, String guildName, String tag,
                                    String errorKey, PlaceholderContext errorContext) {
        public static RankChangeResult error(String key, PlaceholderContext context) {
            return new RankChangeResult(false, null, null, null, null, null, null, null, key, context);
        }
    }

    public boolean demotePlayer(Player demoter, String targetName, String targetRankName) {
        RankChangeResult result = prepareDemotePlayer(demoter, targetName, targetRankName);
        applyRankChangeEffects(demoter, result, "guild.demote");
        return result.success();
    }

    public RankChangeResult prepareDemotePlayer(Player demoter, String targetName, String targetRankName) {
        try {
            PlayerData demoterData = context.getOrCreatePlayerData(demoter);
            if (demoterData.getGuildId() == null) {
                return RankChangeResult.error("guild.no_guild", PlaceholderContext.of(demoter));
            }
            GuildRank demoterRank = demoterData.getRank();
            if (!context.isLeaderOrCoLeader(demoterRank)) {
                return RankChangeResult.error("guild.demote.error_no_permission", PlaceholderContext.of(demoter));
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(demoterData.getGuildId())) {
                return RankChangeResult.error("guild.not_member", PlaceholderContext.of(demoter).with("player", targetName));
            }
            PlayerData targetData = targetOpt.get();
            if (!canManageRank(demoterRank, targetData.getRank())) {
                return RankChangeResult.error("guild.demote.error_target_higher_rank",
                        PlaceholderContext.of(demoter).with("player", targetData.getName()));
            }
            GuildRank oldRank = targetData.getRank();
            if (oldRank == GuildRank.LEADER) {
                return RankChangeResult.error("guild.demote.error_leader_demote",
                        PlaceholderContext.of(demoter).with("player", targetData.getName()));
            }
            GuildRank targetRank;
            if (targetRankName != null && !targetRankName.isBlank()) {
                Optional<GuildRank> parsed = resolveRankByName(targetRankName);
                if (parsed.isEmpty()) {
                    return RankChangeResult.error("guild.demote.error_invalid_rank",
                            PlaceholderContext.of(demoter).with("player", targetData.getName()).with("rank", targetRankName));
                }
                targetRank = parsed.get();
                if (targetRank == oldRank) {
                    return RankChangeResult.error("guild.demote.error_same_rank",
                            PlaceholderContext.of(demoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                }
                if (getRankWeight(targetRank) >= getRankWeight(oldRank)) {
                    return RankChangeResult.error("guild.demote.error_target_rank_higher",
                            PlaceholderContext.of(demoter).with("player", targetData.getName()).with("rank", targetRank.getDisplayName()));
                }
            } else {
                targetRank = getPreviousRank(oldRank);
                if (targetRank == null) {
                    return RankChangeResult.error("guild.demote.error_min_rank",
                            PlaceholderContext.of(demoter).with("player", targetData.getName()));
                }
            }
            targetData.setRank(targetRank);
            context.playerDao.update(targetData);

            UUID guildId = demoterData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            String guildName = guildOpt.map(Guild::getName).orElse("?");
            String tag = guildOpt.map(Guild::getTag).orElse("?");
            return new RankChangeResult(true, targetData.getUuid(), targetData.getName(), oldRank, targetRank, guildId, guildName, tag, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to demote player: " + e.getMessage());
            return RankChangeResult.error("guild.create.error_generic", PlaceholderContext.of(demoter));
        }
    }

    public java.util.concurrent.CompletableFuture<RankChangeResult> demotePlayerAsync(Player demoter, String targetName, String targetRankName) {
        return context.databaseExecutor.supplyAsync(() -> prepareDemotePlayer(demoter, targetName, targetRankName));
    }

    public boolean transferLeadership(Player leader, String targetName) {
        TransferLeadershipResult result = prepareTransferLeadership(leader, targetName);
        applyTransferLeadershipEffects(leader, result);
        return result.success();
    }

    public TransferLeadershipResult prepareTransferLeadership(Player leader, String targetName) {
        try {
            PlayerData leaderData = context.getOrCreatePlayerData(leader);
            if (leaderData.getGuildId() == null) {
                return TransferLeadershipResult.error("guild.no_guild", PlaceholderContext.of(leader));
            }
            if (leaderData.getRank() != GuildRank.LEADER) {
                return TransferLeadershipResult.error("guild.leader.error_no_permission", PlaceholderContext.of(leader));
            }
            Optional<PlayerData> targetOpt = context.resolveTargetData(targetName);
            if (targetOpt.isEmpty() || targetOpt.get().getGuildId() == null
                    || !targetOpt.get().getGuildId().equals(leaderData.getGuildId())) {
                return TransferLeadershipResult.error("guild.not_member", PlaceholderContext.of(leader).with("player", targetName));
            }
            PlayerData targetData = targetOpt.get();
            if (targetData.getUuid().equals(leader.getUniqueId())) {
                return TransferLeadershipResult.error("guild.leader.error_self", PlaceholderContext.of(leader));
            }
            UUID guildId = leaderData.getGuildId();
            Optional<Guild> guildOpt = context.guildDao.findById(guildId);
            if (guildOpt.isEmpty()) {
                return TransferLeadershipResult.error("guild.not_found", PlaceholderContext.of(leader));
            }
            Guild guild = guildOpt.get();

            leaderData.setRank(GuildRank.CO_LEADER);
            targetData.setRank(GuildRank.LEADER);
            guild.setLeaderUuid(targetData.getUuid());
            context.playerDao.update(leaderData);
            context.playerDao.update(targetData);
            context.guildDao.update(guild);

            return new TransferLeadershipResult(true, targetData.getUuid(), targetData.getName(), guild, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to transfer leadership: " + e.getMessage());
            return TransferLeadershipResult.error("guild.create.error_generic", PlaceholderContext.of(leader));
        }
    }

    public void applyTransferLeadershipEffects(Player leader, TransferLeadershipResult result) {
        if (!result.success()) {
            context.send(leader, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild guild = result.guild();
        UUID guildId = guild.getId();
        UUID targetUuid = result.targetUuid();
        String targetName = result.targetName();

        context.getPlayerGuildCache().refresh(leader.getUniqueId(),
                new PlayerData(leader.getUniqueId(), leader.getName(), guildId, GuildRank.CO_LEADER, 0, 0, 0, 0, 0, 0));
        context.getPlayerGuildCache().refresh(targetUuid,
                new PlayerData(targetUuid, targetName, guildId, GuildRank.LEADER, 0, 0, 0, 0, 0, 0));

        context.tagManager.updateTagsForGuild(guildId);
        Player targetOnline = Bukkit.getPlayer(targetUuid);

        context.send(leader, "guild.leader.success", MessageType.SUCCESS,
                PlaceholderContext.of(leader).with("player", targetName));
        Component broadcast = context.langManager.getMessage("guild.leader.broadcast", MessageType.INFO,
                PlaceholderContext.empty().with("player", targetName).with("guild", guild.getName()).with("tag", guild.getTag()));
        context.broadcastToGuild(guildId, broadcast, leader);
        if (targetOnline != null) {
            context.send(targetOnline, "guild.leader.target_message", MessageType.INFO,
                    PlaceholderContext.of(targetOnline).with("guild", guild.getName()).with("tag", guild.getTag()));
        }
    }

    public java.util.concurrent.CompletableFuture<TransferLeadershipResult> transferLeadershipAsync(Player leader, String targetName) {
        return context.databaseExecutor.supplyAsync(() -> prepareTransferLeadership(leader, targetName));
    }

    public record TransferLeadershipResult(boolean success, UUID targetUuid, String targetName, Guild guild,
                                            String errorKey, PlaceholderContext errorContext) {
        public static TransferLeadershipResult error(String key, PlaceholderContext context) {
            return new TransferLeadershipResult(false, null, null, null, key, context);
        }
    }

    private int getRankWeight(GuildRank rank) {
        return rankHelper.getRankWeight(rank);
    }

    private boolean canManageRank(GuildRank actor, GuildRank target) {
        return rankHelper.canManageRank(actor, target);
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
