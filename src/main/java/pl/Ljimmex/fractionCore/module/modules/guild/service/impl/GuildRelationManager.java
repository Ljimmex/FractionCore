package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import pl.Ljimmex.fractionCore.util.TimeUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildAllyRequest;
import pl.Ljimmex.fractionCore.database.entity.GuildRelation;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.database.entity.RelationType;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuildRelationManager {

    private static final long RELATION_CACHE_TTL_MS = 30_000;

    private final GuildContext context;
    private final Map<RelationKey, CachedRelation> relationCache = new ConcurrentHashMap<>();

    public GuildRelationManager(GuildContext context) {
        this.context = context;
    }

    // ============================================================
    // Relation color helpers
    // ============================================================

    public RelationType getRelationType(UUID viewerGuildId, UUID targetGuildId) {
        if (viewerGuildId == null || targetGuildId == null) {
            return RelationType.NEUTRAL;
        }
        if (viewerGuildId.equals(targetGuildId)) {
            return RelationType.NEUTRAL; // callers handle member separately
        }

        RelationKey key = new RelationKey(viewerGuildId, targetGuildId);
        CachedRelation cached = relationCache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.timestamp < RELATION_CACHE_TTL_MS) {
            return cached.type;
        }

        try {
            RelationType type = context.guildRelationDao.find(viewerGuildId, targetGuildId)
                    .map(GuildRelation::getType)
                    .orElse(RelationType.NEUTRAL);
            relationCache.put(key, new CachedRelation(type, now));
            return type;
        } catch (SQLException e) {
            context.plugin.getLogger().warning("Failed to resolve relation type: " + e.getMessage());
            return RelationType.NEUTRAL;
        }
    }

    public void clearCache() {
        relationCache.clear();
    }

    private record RelationKey(UUID a, UUID b) {
        RelationKey {
            // Canonical ordering so (a,b) and (b,a) share the same key.
            if (a.compareTo(b) > 0) {
                UUID tmp = a;
                a = b;
                b = tmp;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RelationKey that)) return false;
            return Objects.equals(a, that.a) && Objects.equals(b, that.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }
    }

    private record CachedRelation(RelationType type, long timestamp) {
    }

    public String getColor(UUID viewerGuildId, UUID targetGuildId) {
        String key;
        if (viewerGuildId == null) {
            key = "guildless";
        } else if (targetGuildId != null && viewerGuildId.equals(targetGuildId)) {
            key = "member";
        } else {
            RelationType type = getRelationType(viewerGuildId, targetGuildId);
            key = switch (type) {
                case ALLY -> "ally";
                case ENEMY -> "enemy";
                case TRUCE -> "truce";
                default -> "neutral";
            };
        }
        return context.guildConfig.getString("relation-colors." + key, "<white>");
    }

    public Component getColoredTag(String tag, UUID viewerGuildId, UUID targetGuildId) {
        String color = getColor(viewerGuildId, targetGuildId);
        String raw = color + tag + "<reset>";
        try {
            return MiniMessage.miniMessage().deserialize(raw);
        } catch (Exception e) {
            return Component.text(tag);
        }
    }

    public Component getColoredTagPrefix(String tag, UUID viewerGuildId, UUID targetGuildId) {
        String color = getColor(viewerGuildId, targetGuildId);
        String raw = color + "[" + tag + "]<reset> ";
        try {
            return MiniMessage.miniMessage().deserialize(raw);
        } catch (Exception e) {
            return Component.text("[" + tag + "] ");
        }
    }

    // ============================================================
    // Ally requests
    // ============================================================

    public boolean sendAllyRequest(Player player, String tag) {
        AllyRequestResult result = prepareSendAllyRequest(player, tag);
        applySendAllyRequestEffects(player, result);
        return result.success();
    }

    public AllyRequestResult prepareSendAllyRequest(Player player, String tag) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return AllyRequestResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return AllyRequestResult.error("guild.ally.error_no_permission", PlaceholderContext.of(player));
            }
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (targetOpt.isEmpty()) {
                return AllyRequestResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild ownGuild = context.guildDao.findById(data.getGuildId()).orElse(null);
            Guild targetGuild = targetOpt.get();
            if (ownGuild == null) {
                return AllyRequestResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            if (ownGuild.getId().equals(targetGuild.getId())) {
                return AllyRequestResult.error("guild.ally.error_self", PlaceholderContext.of(player));
            }
            RelationType current = getRelationType(ownGuild.getId(), targetGuild.getId());
            if (current == RelationType.ALLY) {
                return AllyRequestResult.error("guild.ally.error_already_ally",
                        PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
            }
            if (context.guildAllyRequestDao.exists(ownGuild.getId(), targetGuild.getId())) {
                return AllyRequestResult.error("guild.ally.error_request_already_sent",
                        PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
            }
            Optional<GuildAllyRequest> incoming = context.guildAllyRequestDao.find(targetGuild.getId(), ownGuild.getId());
            if (incoming.isPresent()) {
                AllyAcceptResult acceptResult = prepareAcceptAllyInternal(player, ownGuild, targetGuild);
                return new AllyRequestResult(true, ownGuild, targetGuild, false, acceptResult, null, PlaceholderContext.empty());
            }
            GuildAllyRequest request = new GuildAllyRequest(
                    0, ownGuild.getId(), targetGuild.getId(), TimeUtil.currentEpochSeconds()
            );
            context.guildAllyRequestDao.save(request);
            return new AllyRequestResult(true, ownGuild, targetGuild, true, null, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to send ally request: " + e.getMessage());
            return AllyRequestResult.error("guild.ally.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applySendAllyRequestEffects(Player player, AllyRequestResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        if (result.autoAccept() != null) {
            applyAcceptAllyEffects(player, result.autoAccept());
            return;
        }
        Guild ownGuild = result.ownGuild();
        Guild targetGuild = result.targetGuild();
        context.send(player, "guild.ally.request_sent", MessageType.SUCCESS,
                PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
        Component message = context.langManager.getMessage("guild.ally.request_received", MessageType.INFO,
                PlaceholderContext.of(player, ownGuild).with("guild", ownGuild.getName()).with("tag", ownGuild.getTag()).with("player", player.getName()));
        context.broadcastToGuild(targetGuild.getId(), message, null);
    }

    public java.util.concurrent.CompletableFuture<AllyRequestResult> sendAllyRequestAsync(Player player, String tag) {
        return context.databaseExecutor.supplyAsync(() -> prepareSendAllyRequest(player, tag));
    }

    public record AllyRequestResult(boolean success, Guild ownGuild, Guild targetGuild, boolean requestSent,
                                     AllyAcceptResult autoAccept, String errorKey, PlaceholderContext errorContext) {
        public static AllyRequestResult error(String key, PlaceholderContext context) {
            return new AllyRequestResult(false, null, null, false, null, key, context);
        }
    }

    public boolean acceptAllyRequest(Player player, String tag) {
        AllyAcceptResult result = prepareAcceptAllyRequest(player, tag);
        applyAcceptAllyEffects(player, result);
        return result.success();
    }

    public AllyAcceptResult prepareAcceptAllyRequest(Player player, String tag) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return AllyAcceptResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return AllyAcceptResult.error("guild.ally.error_no_permission", PlaceholderContext.of(player));
            }
            Optional<Guild> ownOpt = context.guildDao.findById(data.getGuildId());
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (ownOpt.isEmpty() || targetOpt.isEmpty()) {
                return AllyAcceptResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            return prepareAcceptAllyInternal(player, ownOpt.get(), targetOpt.get());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to accept ally request: " + e.getMessage());
            return AllyAcceptResult.error("guild.ally.error_generic", PlaceholderContext.of(player));
        }
    }

    private AllyAcceptResult prepareAcceptAllyInternal(Player player, Guild ownGuild, Guild targetGuild) throws SQLException {
        Optional<GuildAllyRequest> requestOpt = context.guildAllyRequestDao.find(targetGuild.getId(), ownGuild.getId());
        if (requestOpt.isEmpty()) {
            return AllyAcceptResult.error("guild.ally.error_no_request",
                    PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
        }
        context.guildAllyRequestDao.delete(targetGuild.getId(), ownGuild.getId());
        context.guildRelationDao.setType(ownGuild.getId(), targetGuild.getId(), RelationType.ALLY);
        clearCache();
        return new AllyAcceptResult(true, ownGuild, targetGuild, null, PlaceholderContext.empty());
    }

    public void applyAcceptAllyEffects(Player player, AllyAcceptResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild ownGuild = result.ownGuild();
        Guild targetGuild = result.targetGuild();
        PlaceholderContext ctx = PlaceholderContext.of(player, ownGuild)
                .with("guild", ownGuild.getName()).with("tag", ownGuild.getTag())
                .with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag());
        context.send(player, "guild.ally.accept_success", MessageType.SUCCESS, ctx);

        Component ownBroadcast = context.langManager.getMessage("guild.ally.accept_broadcast", MessageType.SUCCESS,
                ctx.with("player", player.getName()));
        context.broadcastToGuild(ownGuild.getId(), ownBroadcast, player);

        Component targetBroadcast = context.langManager.getMessage("guild.ally.accepted_by", MessageType.SUCCESS,
                PlaceholderContext.of(player, targetGuild)
                        .with("guild", ownGuild.getName()).with("tag", ownGuild.getTag())
                        .with("player", player.getName()));
        context.broadcastToGuild(targetGuild.getId(), targetBroadcast, null);

        context.tagManager.updateAllTags();
    }

    public java.util.concurrent.CompletableFuture<AllyAcceptResult> acceptAllyRequestAsync(Player player, String tag) {
        return context.databaseExecutor.supplyAsync(() -> prepareAcceptAllyRequest(player, tag));
    }

    public record AllyAcceptResult(boolean success, Guild ownGuild, Guild targetGuild,
                                    String errorKey, PlaceholderContext errorContext) {
        public static AllyAcceptResult error(String key, PlaceholderContext context) {
            return new AllyAcceptResult(false, null, null, key, context);
        }
    }

    public boolean declineAllyRequest(Player player, String tag) {
        AllyDeclineResult result = prepareDeclineAllyRequest(player, tag);
        applyDeclineAllyEffects(player, result);
        return result.success();
    }

    public AllyDeclineResult prepareDeclineAllyRequest(Player player, String tag) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return AllyDeclineResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return AllyDeclineResult.error("guild.ally.error_no_permission", PlaceholderContext.of(player));
            }
            Optional<Guild> ownOpt = context.guildDao.findById(data.getGuildId());
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (ownOpt.isEmpty() || targetOpt.isEmpty()) {
                return AllyDeclineResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild ownGuild = ownOpt.get();
            Guild targetGuild = targetOpt.get();
            Optional<GuildAllyRequest> requestOpt = context.guildAllyRequestDao.find(targetGuild.getId(), ownGuild.getId());
            if (requestOpt.isEmpty()) {
                return AllyDeclineResult.error("guild.ally.error_no_request",
                        PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
            }
            context.guildAllyRequestDao.delete(targetGuild.getId(), ownGuild.getId());
            return new AllyDeclineResult(true, targetGuild, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to decline ally request: " + e.getMessage());
            return AllyDeclineResult.error("guild.ally.error_generic", PlaceholderContext.of(player));
        }
    }

    public void applyDeclineAllyEffects(Player player, AllyDeclineResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild targetGuild = result.targetGuild();
        context.send(player, "guild.ally.decline_success", MessageType.SUCCESS,
                PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
    }

    public java.util.concurrent.CompletableFuture<AllyDeclineResult> declineAllyRequestAsync(Player player, String tag) {
        return context.databaseExecutor.supplyAsync(() -> prepareDeclineAllyRequest(player, tag));
    }

    public record AllyDeclineResult(boolean success, Guild targetGuild,
                                     String errorKey, PlaceholderContext errorContext) {
        public static AllyDeclineResult error(String key, PlaceholderContext context) {
            return new AllyDeclineResult(false, null, key, context);
        }
    }

    // ============================================================
    // Enemy / neutral
    // ============================================================

    public boolean setEnemy(Player player, String tag) {
        RelationChangeResult result = prepareSetRelation(player, tag, RelationType.ENEMY, "guild.enemy");
        applyRelationChangeEffects(player, result, "guild.enemy");
        return result.success();
    }

    public boolean setNeutral(Player player, String tag) {
        RelationChangeResult result = prepareRemoveRelation(player, tag, "guild.neutral");
        applyRelationChangeEffects(player, result, "guild.neutral");
        return result.success();
    }

    private RelationChangeResult prepareSetRelation(Player player, String tag, RelationType type, String langPrefix) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return RelationChangeResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return RelationChangeResult.error(langPrefix + ".error_no_permission", PlaceholderContext.of(player));
            }
            Optional<Guild> ownOpt = context.guildDao.findById(data.getGuildId());
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (ownOpt.isEmpty() || targetOpt.isEmpty()) {
                return RelationChangeResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild ownGuild = ownOpt.get();
            Guild targetGuild = targetOpt.get();
            if (ownGuild.getId().equals(targetGuild.getId())) {
                return RelationChangeResult.error(langPrefix + ".error_self", PlaceholderContext.of(player));
            }
            context.guildRelationDao.setType(ownGuild.getId(), targetGuild.getId(), type);
            clearCache();
            context.guildAllyRequestDao.delete(ownGuild.getId(), targetGuild.getId());
            context.guildAllyRequestDao.delete(targetGuild.getId(), ownGuild.getId());
            return new RelationChangeResult(true, ownGuild, targetGuild, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to set relation: " + e.getMessage());
            return RelationChangeResult.error(langPrefix + ".error_generic", PlaceholderContext.of(player));
        }
    }

    private RelationChangeResult prepareRemoveRelation(Player player, String tag, String langPrefix) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return RelationChangeResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                return RelationChangeResult.error(langPrefix + ".error_no_permission", PlaceholderContext.of(player));
            }
            Optional<Guild> ownOpt = context.guildDao.findById(data.getGuildId());
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (ownOpt.isEmpty() || targetOpt.isEmpty()) {
                return RelationChangeResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild ownGuild = ownOpt.get();
            Guild targetGuild = targetOpt.get();
            if (ownGuild.getId().equals(targetGuild.getId())) {
                return RelationChangeResult.error(langPrefix + ".error_self", PlaceholderContext.of(player));
            }
            context.guildRelationDao.delete(ownGuild.getId(), targetGuild.getId());
            clearCache();
            context.guildAllyRequestDao.delete(ownGuild.getId(), targetGuild.getId());
            context.guildAllyRequestDao.delete(targetGuild.getId(), ownGuild.getId());
            return new RelationChangeResult(true, ownGuild, targetGuild, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to remove relation: " + e.getMessage());
            return RelationChangeResult.error(langPrefix + ".error_generic", PlaceholderContext.of(player));
        }
    }

    public void applyRelationChangeEffects(Player player, RelationChangeResult result, String langPrefix) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        Guild ownGuild = result.ownGuild();
        Guild targetGuild = result.targetGuild();
        PlaceholderContext ctx = PlaceholderContext.of(player, ownGuild)
                .with("guild", ownGuild.getName()).with("tag", ownGuild.getTag())
                .with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag());
        context.send(player, langPrefix + ".success", MessageType.SUCCESS, ctx);

        if (langPrefix.equals("guild.enemy")) {
            Component ownBroadcast = context.langManager.getMessage(langPrefix + ".broadcast", MessageType.WARNING,
                    ctx.with("player", player.getName()));
            context.broadcastToGuild(ownGuild.getId(), ownBroadcast, player);

            Component targetBroadcast = context.langManager.getMessage(langPrefix + ".target_broadcast", MessageType.WARNING,
                    PlaceholderContext.of(player, targetGuild)
                            .with("guild", ownGuild.getName()).with("tag", ownGuild.getTag())
                            .with("player", player.getName()));
            context.broadcastToGuild(targetGuild.getId(), targetBroadcast, null);
        }

        context.tagManager.updateAllTags();
    }

    public java.util.concurrent.CompletableFuture<RelationChangeResult> setEnemyAsync(Player player, String tag) {
        return context.databaseExecutor.supplyAsync(() -> prepareSetRelation(player, tag, RelationType.ENEMY, "guild.enemy"));
    }

    public java.util.concurrent.CompletableFuture<RelationChangeResult> setNeutralAsync(Player player, String tag) {
        return context.databaseExecutor.supplyAsync(() -> prepareRemoveRelation(player, tag, "guild.neutral"));
    }

    public record RelationChangeResult(boolean success, Guild ownGuild, Guild targetGuild,
                                        String errorKey, PlaceholderContext errorContext) {
        public static RelationChangeResult error(String key, PlaceholderContext context) {
            return new RelationChangeResult(false, null, null, key, context);
        }
    }

    // ============================================================
    // List
    // ============================================================

    public boolean sendRelationsList(Player player) {
        RelationsListResult result = fetchRelationsList(player);
        renderRelationsList(player, result);
        return result.success();
    }

    public RelationsListResult fetchRelationsList(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                return RelationsListResult.error("guild.no_guild", PlaceholderContext.of(player));
            }
            Optional<Guild> guildOpt = context.guildDao.findById(data.getGuildId());
            if (guildOpt.isEmpty()) {
                return RelationsListResult.error("guild.not_found", PlaceholderContext.of(player));
            }
            Guild guild = guildOpt.get();
            List<GuildRelation> relations = context.guildRelationDao.findByGuild(guild.getId());
            List<GuildAllyRequest> incoming = context.guildAllyRequestDao.findByTargetGuild(guild.getId());
            return new RelationsListResult(true, guild, relations, incoming, null, PlaceholderContext.empty());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to list relations: " + e.getMessage());
            return RelationsListResult.error("guild.relations.error_generic", PlaceholderContext.of(player));
        }
    }

    public void renderRelationsList(Player player, RelationsListResult result) {
        if (!result.success()) {
            context.send(player, result.errorKey(), MessageType.ERROR, result.errorContext());
            return;
        }
        try {
            Guild guild = result.guild();
            List<GuildRelation> relations = result.relations();
            List<GuildAllyRequest> incoming = result.incomingRequests();

            PlaceholderContext ctx = PlaceholderContext.of(player, guild).with("guild", guild.getName()).with("tag", guild.getTag());
            player.sendMessage(context.langManager.getMessage("guild.relations.list_header", MessageType.INFO, ctx));

            boolean hasAllies = false;
            boolean hasEnemies = false;
            for (GuildRelation relation : relations) {
                UUID otherId = relation.getGuild1Id().equals(guild.getId()) ? relation.getGuild2Id() : relation.getGuild1Id();
                Optional<Guild> otherOpt = context.guildDao.findById(otherId);
                if (otherOpt.isEmpty()) {
                    continue;
                }
                Guild other = otherOpt.get();
                if (relation.getType() == RelationType.ALLY) {
                    hasAllies = true;
                    player.sendMessage(context.langManager.getMessage("guild.relations.ally_entry", MessageType.INFO,
                            ctx.with("target", other.getName()).with("target_tag", other.getTag())));
                } else if (relation.getType() == RelationType.ENEMY) {
                    hasEnemies = true;
                    player.sendMessage(context.langManager.getMessage("guild.relations.enemy_entry", MessageType.INFO,
                            ctx.with("target", other.getName()).with("target_tag", other.getTag())));
                }
            }
            if (!hasAllies) {
                player.sendMessage(context.langManager.getMessage("guild.relations.no_allies", MessageType.INFO, ctx));
            }
            if (!hasEnemies) {
                player.sendMessage(context.langManager.getMessage("guild.relations.no_enemies", MessageType.INFO, ctx));
            }

            if (!incoming.isEmpty()) {
                player.sendMessage(context.langManager.getMessage("guild.relations.incoming_requests_header", MessageType.INFO, ctx));
                for (GuildAllyRequest request : incoming) {
                    Optional<Guild> senderOpt = context.guildDao.findById(request.getGuildId());
                    if (senderOpt.isEmpty()) {
                        continue;
                    }
                    Guild sender = senderOpt.get();
                    player.sendMessage(context.langManager.getMessage("guild.relations.request_entry", MessageType.INFO,
                            ctx.with("target", sender.getName()).with("target_tag", sender.getTag())));
                }
            }
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to render relations list: " + e.getMessage());
            context.send(player, "guild.relations.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
        }
    }

    public java.util.concurrent.CompletableFuture<RelationsListResult> sendRelationsListAsync(Player player) {
        return context.databaseExecutor.supplyAsync(() -> fetchRelationsList(player));
    }

    public record RelationsListResult(boolean success, Guild guild, List<GuildRelation> relations,
                                       List<GuildAllyRequest> incomingRequests,
                                       String errorKey, PlaceholderContext errorContext) {
        public static RelationsListResult error(String key, PlaceholderContext context) {
            return new RelationsListResult(false, null, List.of(), List.of(), key, context);
        }
    }
}
