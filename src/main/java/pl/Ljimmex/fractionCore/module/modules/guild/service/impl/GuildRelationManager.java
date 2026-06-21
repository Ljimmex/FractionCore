package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;
import pl.Ljimmex.fractionCore.database.entity.GuildAllyRequest;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.GuildRelation;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.database.entity.RelationType;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuildRelationManager {

    private final GuildContext context;

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
        try {
            return context.guildRelationDao.find(viewerGuildId, targetGuildId)
                    .map(GuildRelation::getType)
                    .orElse(RelationType.NEUTRAL);
        } catch (SQLException e) {
            context.plugin.getLogger().warning("Failed to resolve relation type: " + e.getMessage());
            return RelationType.NEUTRAL;
        }
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
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                context.send(player, "guild.ally.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (targetOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild ownGuild = context.guildDao.findById(data.getGuildId()).orElse(null);
            Guild targetGuild = targetOpt.get();
            if (ownGuild == null) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (ownGuild.getId().equals(targetGuild.getId())) {
                context.send(player, "guild.ally.error_self", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            RelationType current = getRelationType(ownGuild.getId(), targetGuild.getId());
            if (current == RelationType.ALLY) {
                context.send(player, "guild.ally.error_already_ally", MessageType.ERROR,
                        PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
                return false;
            }
            if (context.guildAllyRequestDao.exists(ownGuild.getId(), targetGuild.getId())) {
                context.send(player, "guild.ally.error_request_already_sent", MessageType.ERROR,
                        PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
                return false;
            }
            Optional<GuildAllyRequest> incoming = context.guildAllyRequestDao.find(targetGuild.getId(), ownGuild.getId());
            if (incoming.isPresent()) {
                return acceptAllyInternal(player, ownGuild, targetGuild);
            }
            GuildAllyRequest request = new GuildAllyRequest(
                    0, ownGuild.getId(), targetGuild.getId(), System.currentTimeMillis() / 1000
            );
            context.guildAllyRequestDao.save(request);
            context.send(player, "guild.ally.request_sent", MessageType.SUCCESS,
                    PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
            Component message = context.langManager.getMessage("guild.ally.request_received", MessageType.INFO,
                    PlaceholderContext.of(player, ownGuild).with("guild", ownGuild.getName()).with("tag", ownGuild.getTag()).with("player", player.getName()));
            context.broadcastToGuild(targetGuild.getId(), message, null);
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to send ally request: " + e.getMessage());
            context.send(player, "guild.ally.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    public boolean acceptAllyRequest(Player player, String tag) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                context.send(player, "guild.ally.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Optional<Guild> ownOpt = context.guildDao.findById(data.getGuildId());
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (ownOpt.isEmpty() || targetOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            return acceptAllyInternal(player, ownOpt.get(), targetOpt.get());
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to accept ally request: " + e.getMessage());
            context.send(player, "guild.ally.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    private boolean acceptAllyInternal(Player player, Guild ownGuild, Guild targetGuild) throws SQLException {
        Optional<GuildAllyRequest> requestOpt = context.guildAllyRequestDao.find(targetGuild.getId(), ownGuild.getId());
        if (requestOpt.isEmpty()) {
            context.send(player, "guild.ally.error_no_request", MessageType.ERROR,
                    PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
            return false;
        }
        context.guildAllyRequestDao.delete(targetGuild.getId(), ownGuild.getId());
        context.guildRelationDao.setType(ownGuild.getId(), targetGuild.getId(), RelationType.ALLY);

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
        return true;
    }

    public boolean declineAllyRequest(Player player, String tag) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                context.send(player, "guild.ally.error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Optional<Guild> ownOpt = context.guildDao.findById(data.getGuildId());
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (ownOpt.isEmpty() || targetOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild ownGuild = ownOpt.get();
            Guild targetGuild = targetOpt.get();
            Optional<GuildAllyRequest> requestOpt = context.guildAllyRequestDao.find(targetGuild.getId(), ownGuild.getId());
            if (requestOpt.isEmpty()) {
                context.send(player, "guild.ally.error_no_request", MessageType.ERROR,
                        PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
                return false;
            }
            context.guildAllyRequestDao.delete(targetGuild.getId(), ownGuild.getId());
            context.send(player, "guild.ally.decline_success", MessageType.SUCCESS,
                    PlaceholderContext.of(player, targetGuild).with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag()));
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to decline ally request: " + e.getMessage());
            context.send(player, "guild.ally.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    // ============================================================
    // Enemy / neutral
    // ============================================================

    public boolean setEnemy(Player player, String tag) {
        return setRelation(player, tag, RelationType.ENEMY, "guild.enemy");
    }

    public boolean setNeutral(Player player, String tag) {
        return removeRelation(player, tag, "guild.neutral");
    }

    private boolean setRelation(Player player, String tag, RelationType type, String langPrefix) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                context.send(player, langPrefix + ".error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Optional<Guild> ownOpt = context.guildDao.findById(data.getGuildId());
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (ownOpt.isEmpty() || targetOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild ownGuild = ownOpt.get();
            Guild targetGuild = targetOpt.get();
            if (ownGuild.getId().equals(targetGuild.getId())) {
                context.send(player, langPrefix + ".error_self", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            context.guildRelationDao.setType(ownGuild.getId(), targetGuild.getId(), type);
            context.guildAllyRequestDao.delete(ownGuild.getId(), targetGuild.getId());
            context.guildAllyRequestDao.delete(targetGuild.getId(), ownGuild.getId());

            PlaceholderContext ctx = PlaceholderContext.of(player, ownGuild)
                    .with("guild", ownGuild.getName()).with("tag", ownGuild.getTag())
                    .with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag());
            context.send(player, langPrefix + ".success", MessageType.SUCCESS, ctx);

            Component ownBroadcast = context.langManager.getMessage(langPrefix + ".broadcast", MessageType.WARNING,
                    ctx.with("player", player.getName()));
            context.broadcastToGuild(ownGuild.getId(), ownBroadcast, player);

            Component targetBroadcast = context.langManager.getMessage(langPrefix + ".target_broadcast", MessageType.WARNING,
                    PlaceholderContext.of(player, targetGuild)
                            .with("guild", ownGuild.getName()).with("tag", ownGuild.getTag())
                            .with("player", player.getName()));
            context.broadcastToGuild(targetGuild.getId(), targetBroadcast, null);

            context.tagManager.updateAllTags();
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to set relation: " + e.getMessage());
            context.send(player, langPrefix + ".error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    private boolean removeRelation(Player player, String tag, String langPrefix) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            if (!context.isLeaderOrCoLeader(data.getRank())) {
                context.send(player, langPrefix + ".error_no_permission", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Optional<Guild> ownOpt = context.guildDao.findById(data.getGuildId());
            Optional<Guild> targetOpt = context.guildDao.findByTag(tag.toUpperCase());
            if (ownOpt.isEmpty() || targetOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild ownGuild = ownOpt.get();
            Guild targetGuild = targetOpt.get();
            if (ownGuild.getId().equals(targetGuild.getId())) {
                context.send(player, langPrefix + ".error_self", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            context.guildRelationDao.delete(ownGuild.getId(), targetGuild.getId());
            context.guildAllyRequestDao.delete(ownGuild.getId(), targetGuild.getId());
            context.guildAllyRequestDao.delete(targetGuild.getId(), ownGuild.getId());

            PlaceholderContext ctx = PlaceholderContext.of(player, ownGuild)
                    .with("guild", ownGuild.getName()).with("tag", ownGuild.getTag())
                    .with("target", targetGuild.getName()).with("target_tag", targetGuild.getTag());
            context.send(player, langPrefix + ".success", MessageType.SUCCESS, ctx);

            context.tagManager.updateAllTags();
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to remove relation: " + e.getMessage());
            context.send(player, langPrefix + ".error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }

    // ============================================================
    // List
    // ============================================================

    public boolean sendRelationsList(Player player) {
        try {
            PlayerData data = context.getOrCreatePlayerData(player);
            if (data.getGuildId() == null) {
                context.send(player, "guild.no_guild", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Optional<Guild> guildOpt = context.guildDao.findById(data.getGuildId());
            if (guildOpt.isEmpty()) {
                context.send(player, "guild.not_found", MessageType.ERROR, PlaceholderContext.of(player));
                return false;
            }
            Guild guild = guildOpt.get();
            List<GuildRelation> relations = context.guildRelationDao.findByGuild(guild.getId());

            PlaceholderContext ctx = PlaceholderContext.of(player, guild).with("guild", guild.getName()).with("tag", guild.getTag());
            player.sendMessage(Component.text("=== Relacje gildii " + guild.getName() + " [" + guild.getTag() + "] ===").color(NamedTextColor.GOLD));

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

            List<GuildAllyRequest> incoming = context.guildAllyRequestDao.findByTargetGuild(guild.getId());
            if (!incoming.isEmpty()) {
                player.sendMessage(Component.text("Oczekujace propozycje sojuszu:").color(NamedTextColor.YELLOW));
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
            return true;
        } catch (SQLException e) {
            context.plugin.getLogger().severe("Failed to list relations: " + e.getMessage());
            context.send(player, "guild.relations.error_generic", MessageType.ERROR, PlaceholderContext.of(player));
            return false;
        }
    }
}
