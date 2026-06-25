package pl.Ljimmex.fractionCore.command.guild;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.lang.MessageParser;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.lang.PlaceholderResolver;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildService;

import java.util.List;

public final class CommandHelper {

    private final JavaPlugin plugin;
    private final GuildService guildService;
    private final LangManager langManager;

    public CommandHelper(JavaPlugin plugin, GuildService guildService, LangManager langManager) {
        this.plugin = plugin;
        this.guildService = guildService;
        this.langManager = langManager;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public GuildService guildService() {
        return guildService;
    }

    public LangManager langManager() {
        return langManager;
    }

    public Player player(CommandContext<CommandSourceStack> ctx) {
        if (sender(ctx) instanceof Player p) {
            return p;
        }
        sender(ctx).sendMessage(langManager.getMessage("guild.error.player_only", MessageType.ERROR, PlaceholderContext.empty()));
        return null;
    }

    public CommandSender sender(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }

    public int usage(CommandSender sender, String text) {
        sender.sendMessage(langManager.getMessage("guild.usage.base", MessageType.RAW, PlaceholderContext.empty().with("usage", text)));
        return 0;
    }

    public int createUsage(CommandSender sender) {
        sender.sendMessage(langManager.getMessage("guild.usage.create", MessageType.RAW, PlaceholderContext.empty()));
        sender.sendMessage(langManager.getMessage("guild.usage.create_confirm", MessageType.RAW, PlaceholderContext.empty()));
        return 0;
    }

    public int flagUsage(CommandSender sender) {
        sender.sendMessage(langManager.getMessage("guild.usage.flag", MessageType.RAW, PlaceholderContext.empty()));
        sender.sendMessage(langManager.getMessage("guild.usage.flag_available", MessageType.RAW, PlaceholderContext.empty()));
        return 0;
    }

    public int cuboidUsage(CommandSender sender) {
        sender.sendMessage(langManager.getMessage("guild.usage.cuboid", MessageType.RAW, PlaceholderContext.empty()));
        sender.sendMessage(langManager.getMessage("guild.usage.cuboid_available_flags", MessageType.RAW, PlaceholderContext.empty()));
        sender.sendMessage(langManager.getMessage("guild.usage.cuboid_available_values", MessageType.RAW, PlaceholderContext.empty()));
        return 0;
    }

    public void sendRaw(CommandSender sender, String key) {
        sendRaw(sender, key, PlaceholderContext.empty());
    }

    public void sendRaw(CommandSender sender, String key, PlaceholderContext context) {
        String raw = langManager.getRawMessage(key);
        if (raw == null) {
            return;
        }
        String resolved = PlaceholderResolver.resolve(raw, context);
        for (String line : resolved.split("\n")) {
            sender.sendMessage(MessageParser.parse(line));
        }
    }

    public void sendNoPermission(Player player) {
        player.sendMessage(langManager.getMessage("general.no_permission", MessageType.ERROR, PlaceholderContext.of(player)));
    }

    public SuggestionProvider<CommandSourceStack> onlinePlayers() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(remaining)) {
                    builder.suggest(p.getName());
                }
            }
            return builder.buildFuture();
        };
    }

    public SuggestionProvider<CommandSourceStack> rankNames() {
        return suggest(List.of("Lider", "Co-Lider", "Moderator", "Czlonek", "Rekrut"));
    }

    public SuggestionProvider<CommandSourceStack> guildTags() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            try {
                for (var guild : guildService.getGuildDao().findAll()) {
                    String tag = guild.getTag();
                    if (tag.toLowerCase().startsWith(remaining)) {
                        builder.suggest(tag);
                    }
                }
            } catch (Exception ignored) {
            }
            return builder.buildFuture();
        };
    }

    public SuggestionProvider<CommandSourceStack> helpCategories() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            suggestIf(builder, remaining, "guild", "gildia", "g");
            if (ctx.getSource().getSender().hasPermission("fractioncore.admin")) {
                suggestIf(builder, remaining, "admin", "administrator", "a");
            }
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> suggest(List<String> values) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String value : values) {
                if (value.toLowerCase().startsWith(remaining)) {
                    builder.suggest(value);
                }
            }
            return builder.buildFuture();
        };
    }

    public static void suggestIf(com.mojang.brigadier.suggestion.SuggestionsBuilder builder, String remaining, String... values) {
        for (String value : values) {
            if (value.startsWith(remaining)) {
                builder.suggest(value);
            }
        }
    }

    public LiteralArgumentBuilder<CommandSourceStack> relationNode(String name, String permission, String usage, RelationExecutor executor) {
        return Commands.literal(name)
                .requires(src -> src.getSender().hasPermission(permission))
                .executes(ctx -> usage(sender(ctx), usage))
                .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests(guildTags())
                        .executes(ctx -> executor.run(ctx, StringArgumentType.getString(ctx, "tag"))));
    }

    public String getAuthor() {
        List<String> authors = plugin.getPluginMeta().getAuthors();
        return authors.isEmpty() ? "Ljimex" : authors.get(0);
    }

    public void renderHelpMain(CommandSender sender) {
        PlaceholderContext infoContext = PlaceholderContext.empty()
                .with("version", plugin.getPluginMeta().getVersion())
                .with("author", getAuthor());

        sendRaw(sender, "guild.help.separator");
        sendRaw(sender, "guild.help.main.title");
        sendRaw(sender, "guild.help.main.welcome");
        sendRaw(sender, "guild.help.main.available_commands");
        sendRaw(sender, "guild.help.main.lines");
        sendRaw(sender, "guild.help.main.info_header");
        sendRaw(sender, "guild.help.main.plugin");
        sendRaw(sender, "guild.help.main.version", infoContext);
        sendRaw(sender, "guild.help.main.author", infoContext);
        sendRaw(sender, "guild.help.separator");
    }

    public void renderHelpGuild(CommandSender sender) {
        sendRaw(sender, "guild.help.separator");
        sendRaw(sender, "guild.help.guild.title");
        sendRaw(sender, "guild.help.guild.subtitle");
        sendRaw(sender, "guild.help.guild.lines");
        sendRaw(sender, "guild.help.separator");
    }

    public void renderHelpAdmin(CommandSender sender) {
        sendRaw(sender, "guild.help.separator");
        sendRaw(sender, "guild.help.admin.title");
        sendRaw(sender, "guild.help.admin.subtitle");
        sendRaw(sender, "guild.help.admin.lines");
        sendRaw(sender, "guild.help.separator");
    }

    @FunctionalInterface
    public interface RelationExecutor {
        int run(CommandContext<CommandSourceStack> ctx, String tag);
    }
}
