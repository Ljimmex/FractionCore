package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.Ljimmex.fractionCore.database.entity.CuboidData;
import pl.Ljimmex.fractionCore.lang.MessageParser;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.lang.PlaceholderResolver;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildCreateResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuildCreationManager {

    private static final long CONFIRMATION_TIMEOUT_MS = 60_000;

    private final GuildContext context;
    private final GuildCreationValidator validator;
    private final GuildCreationExecutor executor;
    private final GuildCreationHelper helper;
    private final java.util.Map<UUID, PendingCreateRequest> pendingRequests = new java.util.concurrent.ConcurrentHashMap<>();

    public GuildCreationManager(GuildContext context) {
        this.context = context;
        this.validator = new GuildCreationValidator(context);
        this.executor = new GuildCreationExecutor(context);
        this.helper = new GuildCreationHelper(context);
    }

    public GuildCreateResult prepareCreation(Player founder, String rawName, String rawTag) {
        GuildCreationValidator.ValidationResult validation = validator.validateAll(founder, rawName, rawTag);
        context.plugin.getLogger().info("[FractionCore] prepareCreation validation result for " + founder.getName() + ": " + validation.result());
        if (validation.result() != GuildCreateResult.SUCCESS) {
            return validation.result();
        }

        pendingRequests.put(founder.getUniqueId(), new PendingCreateRequest(
                validation.name(), validation.tag(), System.currentTimeMillis()
        ));
        return GuildCreateResult.SUCCESS;
    }

    public GuildCreateResult confirmCreation(Player founder) {
        PendingCreateRequest request = pendingRequests.remove(founder.getUniqueId());
        if (request == null || System.currentTimeMillis() - request.createdAt() > CONFIRMATION_TIMEOUT_MS) {
            return GuildCreateResult.NO_PENDING_REQUEST;
        }

        GuildCreationValidator.ValidationResult validation = validator.validateAll(founder, request.name(), request.tag());
        if (validation.result() != GuildCreateResult.SUCCESS) {
            return validation.result();
        }

        return executor.executeCreation(founder, validation.name(), validation.tag());
    }

    public CompletableFuture<GuildCreateResult> prepareCreationAsync(Player founder, String rawName, String rawTag) {
        GuildCreationValidator.ValidationResult syncValidation = validator.validateSync(founder, rawName, rawTag);
        return validator.validateAsync(founder, rawName, rawTag, syncValidation)
                .thenApply(validation -> {
                    if (validation.result() != GuildCreateResult.SUCCESS) {
                        return validation.result();
                    }

                    pendingRequests.put(founder.getUniqueId(), new PendingCreateRequest(
                            validation.name(), validation.tag(), System.currentTimeMillis()
                    ));
                    return GuildCreateResult.SUCCESS;
                });
    }

    public CompletableFuture<GuildCreateResult> confirmCreationAsync(Player founder) {
        PendingCreateRequest request = pendingRequests.remove(founder.getUniqueId());
        if (request == null || System.currentTimeMillis() - request.createdAt() > CONFIRMATION_TIMEOUT_MS) {
            return CompletableFuture.completedFuture(GuildCreateResult.NO_PENDING_REQUEST);
        }

        GuildCreationValidator.ValidationResult syncValidation = validator.validateSync(founder, request.name(), request.tag());
        return validator.validateAsync(founder, request.name(), request.tag(), syncValidation)
                .thenCompose(validation -> {
                    if (validation.result() != GuildCreateResult.SUCCESS) {
                        return CompletableFuture.completedFuture(validation.result());
                    }

                    return executor.executeCreationAsync(founder, validation.name(), validation.tag());
                });
    }

    public Component buildPreview(Player founder, String rawName, String rawTag) {
        GuildCreationValidator.ValidationResult validation = validator.validateAll(founder, rawName, rawTag);
        String name = validation.name();
        String tag = validation.tag();
        PlaceholderContext ctx = PlaceholderContext.of(founder).with("name", name).with("tag", tag);

        Component preview = rawComponent("guild.create.preview.title",
                ctx,
                Component.text("=== GUILD CREATION SUMMARY ===").color(NamedTextColor.GOLD));
        preview = preview.append(Component.newline());
        preview = preview.append(rawComponent("guild.create.preview.name_label",
                        ctx,
                        Component.text("Name: ").color(NamedTextColor.GRAY)))
                .append(Component.text(name).color(NamedTextColor.AQUA));
        preview = preview.append(Component.newline());
        preview = preview.append(rawComponent("guild.create.preview.tag_label",
                        ctx,
                        Component.text("Tag: ").color(NamedTextColor.GRAY)))
                .append(parseTag(tag));
        preview = preview.append(Component.newline());

        Component status;
        if (validation.result() == GuildCreateResult.SUCCESS) {
            status = rawComponent("guild.create.preview.status_ok",
                    ctx,
                    Component.text("Everything looks good. Type /guild create confirm to proceed.").color(NamedTextColor.GREEN));
        } else {
            String reason = translateResult(validation.result());
            status = rawComponent("guild.create.preview.status_error",
                    ctx.with("reason", reason),
                    Component.text("Error: " + reason).color(NamedTextColor.RED));
        }
        preview = preview.append(status);

        List<CostItem> costItems = helper.loadCostItems();
        if (!costItems.isEmpty()) {
            preview = preview.append(Component.newline())
                    .append(rawComponent("guild.create.preview.cost_header",
                            ctx,
                            Component.text("Cost:").color(NamedTextColor.YELLOW)));
            for (CostItem costItem : costItems) {
                int available = founder.getInventory().all(costItem.material()).values().stream()
                        .mapToInt(ItemStack::getAmount).sum();
                NamedTextColor amountColor = available >= costItem.amount() ? NamedTextColor.GREEN : NamedTextColor.RED;
                Component entryPrefix = rawComponent("guild.create.preview.cost_entry",
                        ctx.with("material", costItem.material().name()),
                        Component.text("- " + costItem.material().name() + ": ").color(NamedTextColor.GRAY));
                preview = preview.append(Component.newline())
                        .append(entryPrefix)
                        .append(Component.text(available + "/" + costItem.amount()).color(amountColor));
            }
        }

        return preview;
    }

    public Component getResultMessage(Player player, GuildCreateResult result, String rawName, String rawTag) {
        String name = rawName != null ? rawName.trim() : "";
        String tag = rawTag != null ? rawTag.trim().toUpperCase() : "";
        context.plugin.getLogger().info("[FractionCore] getResultMessage: result=" + result + ", name='" + name + "', tag='" + tag + "'");
        PlaceholderContext ctx = PlaceholderContext.of(player);
        String key = "guild.create.error_generic";

        switch (result) {
            case SUCCESS -> {
                ctx.with("name", name).with("tag", tag);
                key = "guild.create.success";
            }
            case NAME_TOO_SHORT -> {
                int min = context.guildConfig.getInt("foundation.min-name-length", 3);
                ctx.with("min", min).with("current", name.length());
                key = "guild.create.error_name_too_short";
            }
            case NAME_TOO_LONG -> {
                int max = context.guildConfig.getInt("foundation.max-name-length", 24);
                ctx.with("max", max).with("current", name.length());
                key = "guild.create.error_name_too_long";
            }
            case INVALID_NAME -> {
                ctx.with("name", name);
                key = "guild.create.error_name_invalid";
            }
            case TAG_TOO_SHORT -> {
                int min = context.guildConfig.getInt("foundation.min-tag-length", 2);
                ctx.with("min", min).with("current", tag.length());
                key = "guild.create.error_tag_too_short";
            }
            case TAG_TOO_LONG -> {
                int max = context.guildConfig.getInt("foundation.max-tag-length", 6);
                ctx.with("max", max).with("current", tag.length());
                key = "guild.create.error_tag_too_long";
            }
            case INVALID_TAG -> {
                ctx.with("tag", tag);
                key = "guild.create.error_tag_invalid";
            }
            case NAME_TAKEN -> {
                ctx.with("name", name);
                key = "guild.create.error_name_taken";
            }
            case TAG_TAKEN -> {
                ctx.with("tag", tag);
                key = "guild.create.error_tag_taken";
            }
            case INSUFFICIENT_ITEMS -> key = "guild.create.error_insufficient_items";
            case ALREADY_IN_GUILD -> key = "guild.create.error_already_in_guild";
            case COOLDOWN -> {
                int minutes = context.guildConfig.getInt("foundation-requirements.cooldown-minutes", 2880);
                ctx.with("minutes", minutes);
                key = "guild.create.error_cooldown";
            }
            case BLOCKED_WORLD -> {
                ctx.with("world", player.getWorld().getName());
                key = "guild.create.error_blocked_world";
            }
            case TOO_CLOSE_TO_SPAWN -> {
                int required = context.guildConfig.getInt("foundation.min-distance-from-spawn", 200);
                double current = player.getLocation().distance(player.getWorld().getSpawnLocation());
                ctx.with("required", required)
                        .with("current", String.format(Locale.US, "%.1f", current))
                        .with("remaining", Math.max(0, (int) Math.ceil(required - current)));
                key = "guild.create.error_too_close_to_spawn";
            }
            case TOO_CLOSE_TO_OTHER_GUILD -> {
                int startingRadius = context.guildConfig.getInt("foundation.starting-radius", 16);
                int buffer = context.guildConfig.getInt("foundation.min-distance-between-guilds", 5);
                double current = Double.MAX_VALUE;
                int required = 0;
                try {
                    for (CuboidData other : context.cuboidDao.findAll()) {
                        if (!other.getWorld().equalsIgnoreCase(player.getWorld().getName())) {
                            continue;
                        }
                        double dx = player.getLocation().getBlockX() - other.getCenterX();
                        double dz = player.getLocation().getBlockZ() - other.getCenterZ();
                        double distance = Math.sqrt(dx * dx + dz * dz);
                        double needed = startingRadius + other.getRadius() + buffer;
                        if (distance < current) {
                            current = distance;
                            required = (int) needed;
                        }
                    }
                } catch (SQLException e) {
                    context.plugin.getLogger().severe("Failed to compute distance to nearest guild: " + e.getMessage());
                }
                if (current == Double.MAX_VALUE) {
                    current = 0;
                }
                ctx.with("required", required)
                        .with("current", String.format(Locale.US, "%.1f", current))
                        .with("remaining", Math.max(0, (int) Math.ceil(required - current)));
                key = "guild.create.error_too_close_to_other_guild";
            }
            case INVALID_LOCATION -> key = "guild.create.error_invalid_location";
            case NO_PENDING_REQUEST -> key = "guild.create.error_no_pending_request";
            case ERROR -> key = "guild.create.error_generic";
        }

        context.plugin.getLogger().info("[FractionCore] getResultMessage resolved key: " + key);
        String rawMessage = context.langManager.getRawMessage(key);
        if (rawMessage == null) {
            context.plugin.getLogger().warning("[FractionCore] Missing lang key: " + key);
            return context.langManager.getMessageWithoutPrefix("general.missing_translation", ctx.with("key", key));
        }
        return context.langManager.getMessageWithoutPrefix(key, ctx);
    }

    private Component rawComponent(String key, PlaceholderContext ctx, Component fallback) {
        String raw = context.langManager.getRawMessage(key);
        if (raw == null) {
            context.plugin.getLogger().warning("[FractionCore] Missing lang key: " + key);
            return fallback;
        }
        return MessageParser.parse(PlaceholderResolver.resolve(raw, ctx));
    }


    private Component parseTag(String tag) {
        String color = context.guildConfig.getString("relation-colors.neutral", "<white>");
        String full = color + "[" + tag + "] ";
        if (full.contains("&") || full.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(full);
        }
        try {
            return MiniMessage.miniMessage().deserialize(full);
        } catch (Exception e) {
            return Component.text("[" + tag + "] ");
        }
    }

    private String translateResult(GuildCreateResult result) {
        String key = switch (result) {
            case SUCCESS -> "guild.create.preview.result.ok";
            case INVALID_NAME -> "guild.create.preview.result.invalid_name";
            case NAME_TOO_SHORT -> "guild.create.preview.result.name_too_short";
            case NAME_TOO_LONG -> "guild.create.preview.result.name_too_long";
            case INVALID_TAG -> "guild.create.preview.result.invalid_tag";
            case TAG_TOO_SHORT -> "guild.create.preview.result.tag_too_short";
            case TAG_TOO_LONG -> "guild.create.preview.result.tag_too_long";
            case NAME_TAKEN -> "guild.create.preview.result.name_taken";
            case TAG_TAKEN -> "guild.create.preview.result.tag_taken";
            case INSUFFICIENT_ITEMS -> "guild.create.preview.result.insufficient_items";
            case ALREADY_IN_GUILD -> "guild.create.preview.result.already_in_guild";
            case COOLDOWN -> "guild.create.preview.result.cooldown";
            case BLOCKED_WORLD -> "guild.create.preview.result.blocked_world";
            case TOO_CLOSE_TO_SPAWN -> "guild.create.preview.result.too_close_to_spawn";
            case TOO_CLOSE_TO_OTHER_GUILD -> "guild.create.preview.result.too_close_to_other_guild";
            case INVALID_LOCATION -> "guild.create.preview.result.invalid_location";
            case ERROR -> "guild.create.preview.result.error";
            case NO_PENDING_REQUEST -> "guild.create.preview.result.no_pending_request";
        };
        String raw = context.langManager.getRawMessage(key);
        return raw != null ? raw : key;
    }

    private record PendingCreateRequest(String name, String tag, long createdAt) {
    }
}
