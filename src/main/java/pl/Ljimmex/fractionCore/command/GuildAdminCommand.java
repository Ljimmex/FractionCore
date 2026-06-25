package pl.Ljimmex.fractionCore.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.FractionCore;
import pl.Ljimmex.fractionCore.config.ConfigManager;
import pl.Ljimmex.fractionCore.config.DebugManager;
import pl.Ljimmex.fractionCore.module.BaseModule;
import pl.Ljimmex.fractionCore.module.ModuleManager;
import pl.Ljimmex.fractionCore.module.ModuleState;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.lang.MessageParser;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.lang.PlaceholderResolver;
import pl.Ljimmex.fractionCore.module.modules.LangModule;

public class GuildAdminCommand {

    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;
    private final LangManager langManager;

    public GuildAdminCommand(JavaPlugin plugin, ModuleManager moduleManager, LangManager langManager) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;
        this.langManager = langManager;
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("admin")
                .requires(src -> src.getSender().hasPermission("fractioncore.admin"))
                .executes(this::sendAdminUsage)
                .then(moduleNode())
                .then(langNode())
                .then(reloadNode())
                .then(debugNode())
                .then(pluginNode());
    }

    private LiteralArgumentBuilder<CommandSourceStack> moduleNode() {
        return Commands.literal("module")
                .requires(src -> src.getSender().hasPermission("fractioncore.admin.module"))
                .executes(ctx -> usage(sender(ctx), "/guild admin module <list|enable|disable|reload> [modul]"))
                .then(Commands.literal("list").executes(this::handleModuleList))
                .then(Commands.literal("enable")
                        .then(Commands.argument("modul", StringArgumentType.word())
                                .suggests(moduleNames())
                                .executes(ctx -> handleModuleChange(ctx, ModuleAction.ENABLE))))
                .then(Commands.literal("disable")
                        .then(Commands.argument("modul", StringArgumentType.word())
                                .suggests(moduleNames())
                                .executes(ctx -> handleModuleChange(ctx, ModuleAction.DISABLE))))
                .then(Commands.literal("reload")
                        .executes(this::handleModuleReloadAll)
                        .then(Commands.argument("modul", StringArgumentType.word())
                                .suggests(moduleNames())
                                .executes(ctx -> handleModuleChange(ctx, ModuleAction.RELOAD))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> langNode() {
        return Commands.literal("lang")
                .requires(src -> src.getSender().hasPermission("fractioncore.admin.lang.reload"))
                .executes(ctx -> usage(sender(ctx), "/guild admin lang <reload|reset>"))
                .then(Commands.literal("reload").executes(ctx -> handleLang(ctx, LangAction.RELOAD)))
                .then(Commands.literal("reset").executes(ctx -> handleLang(ctx, LangAction.RESET)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> reloadNode() {
        return Commands.literal("reload")
                .requires(src -> src.getSender().hasPermission("fractioncore.admin.reload"))
                .executes(this::handleAdminReload);
    }

    private LiteralArgumentBuilder<CommandSourceStack> debugNode() {
        return Commands.literal("debug")
                .requires(src -> src.getSender().hasPermission("fractioncore.admin.debug"))
                .executes(ctx -> usage(sender(ctx), "/guild admin debug <true|false>"))
                .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(this::handleAdminDebug));
    }

    private LiteralArgumentBuilder<CommandSourceStack> pluginNode() {
        return Commands.literal("plugin")
                .requires(src -> src.getSender().hasPermission("fractioncore.admin.plugin.reload"))
                .executes(ctx -> usage(sender(ctx), "/guild admin plugin reload"))
                .then(Commands.literal("reload").executes(this::handleAdminPlugin));
    }

    private int handleModuleList(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = sender(ctx);
        sendRaw(sender, "guild.admin.module_list.header");
        for (BaseModule module : moduleManager.getModules()) {
            ModuleState state = module.getState();
            PlaceholderContext context = PlaceholderContext.empty()
                    .with("module", module.getName())
                    .with("state", state.name())
                    .with("state_color", stateColor(state));
            sendRaw(sender, "guild.admin.module_list.entry", context);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int handleModuleChange(CommandContext<CommandSourceStack> ctx, ModuleAction action) {
        String name = StringArgumentType.getString(ctx, "modul");
        boolean success = switch (action) {
            case ENABLE -> moduleManager.enableModule(name);
            case DISABLE -> moduleManager.disableModule(name);
            case RELOAD -> moduleManager.reloadModule(name);
        };
        CommandSender sender = sender(ctx);
        String key = switch (action) {
            case ENABLE -> success ? "guild.admin.module.enabled" : "guild.admin.module.enable_failed";
            case DISABLE -> success ? "guild.admin.module.disabled" : "guild.admin.module.disable_failed";
            case RELOAD -> success ? "guild.admin.module.reloaded" : "guild.admin.module.reload_failed";
        };
        sendRaw(sender, key, PlaceholderContext.empty().with("module", name));
        return Command.SINGLE_SUCCESS;
    }

    private int handleModuleReloadAll(CommandContext<CommandSourceStack> ctx) {
        moduleManager.reloadAllModules();
        sendRaw(sender(ctx), "guild.admin.module.reload_all_success");
        return Command.SINGLE_SUCCESS;
    }

    private int handleLang(CommandContext<CommandSourceStack> ctx, LangAction action) {
        CommandSender sender = sender(ctx);
        LangModule langModule = getLangModule();
        if (langModule == null) {
            sendRaw(sender, "guild.admin.lang.module_inactive");
            return Command.SINGLE_SUCCESS;
        }
        switch (action) {
            case RELOAD -> {
                langModule.getLangManager().reload();
                sendRaw(sender, "guild.admin.lang.reload_success");
            }
            case RESET -> {
                langModule.getLangManager().resetLanguages();
                sendRaw(sender, "guild.admin.lang.reset_success");
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int handleAdminReload(CommandContext<CommandSourceStack> ctx) {
        ConfigManager configManager = ((FractionCore) plugin).getConfigManager();
        configManager.reload();
        moduleManager.loadConfiguration(configManager.getPluginConfig());
        moduleManager.reloadAllModules();
        ((FractionCore) plugin).getDebugManager().loadConfiguration(configManager.getPluginConfig());
        LangModule langModule = getLangModule();
        if (langModule != null) {
            langModule.getLangManager().loadConfiguration(configManager.getPluginConfig());
        }
        sendRaw(sender(ctx), "guild.admin.reload_success");
        return Command.SINGLE_SUCCESS;
    }

    private int handleAdminDebug(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        DebugManager debugManager = ((FractionCore) plugin).getDebugManager();
        debugManager.setDebugEnabled(enabled);
        sendRaw(sender(ctx), enabled ? "guild.admin.debug.enabled" : "guild.admin.debug.disabled");
        return Command.SINGLE_SUCCESS;
    }

    private int handleAdminPlugin(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = sender(ctx);
        Plugin plugMan = Bukkit.getPluginManager().getPlugin("PlugMan");
        if (plugMan == null || !plugMan.isEnabled()) {
            sendRaw(sender, "guild.admin.plugin.plugman_missing");
            return Command.SINGLE_SUCCESS;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "plugman reload FractionCore");
        sendRaw(sender, "guild.admin.plugin.reload_success");
        return Command.SINGLE_SUCCESS;
    }

    private int sendAdminUsage(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = sender(ctx);
        sendRaw(sender, "guild.admin.usage.header");
        sendRaw(sender, "guild.admin.usage.lines");
        return Command.SINGLE_SUCCESS;
    }

    private LangModule getLangModule() {
        BaseModule module = moduleManager.getModule("lang");
        return module instanceof LangModule ? (LangModule) module : null;
    }

    private SuggestionProvider<CommandSourceStack> moduleNames() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (BaseModule module : moduleManager.getModules()) {
                String name = module.getName();
                if (name.toLowerCase().startsWith(remaining)) builder.suggest(name);
            }
            return builder.buildFuture();
        };
    }

    private static CommandSender sender(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }

    private int usage(CommandSender sender, String text) {
        sender.sendMessage(langManager.getMessage("guild.usage.base", MessageType.RAW, PlaceholderContext.empty().with("usage", text)));
        return 0;
    }

    private void sendRaw(CommandSender sender, String key) {
        sendRaw(sender, key, PlaceholderContext.empty());
    }

    private void sendRaw(CommandSender sender, String key, PlaceholderContext context) {
        String raw = langManager.getRawMessage(key);
        if (raw == null) return;
        String resolved = PlaceholderResolver.resolve(raw, context);
        for (String line : resolved.split("\n")) {
            sender.sendMessage(MessageParser.parse(line));
        }
    }

    private static String stateColor(ModuleState state) {
        return switch (state) {
            case RUNNING -> "green";
            case DISABLED -> "red";
            default -> "yellow";
        };
    }

    private enum ModuleAction { ENABLE, DISABLE, RELOAD }

    private enum LangAction { RELOAD, RESET }
}
