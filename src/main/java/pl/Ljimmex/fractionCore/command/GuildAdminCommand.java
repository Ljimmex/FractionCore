package pl.Ljimmex.fractionCore.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import pl.Ljimmex.fractionCore.module.modules.LangModule;

public class GuildAdminCommand {

    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;

    public GuildAdminCommand(JavaPlugin plugin, ModuleManager moduleManager) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;
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
        sender.sendMessage(Component.text("=== MODULY ===").color(NamedTextColor.GOLD));
        for (BaseModule module : moduleManager.getModules()) {
            ModuleState state = module.getState();
            NamedTextColor color = state == ModuleState.RUNNING ? NamedTextColor.GREEN :
                    state == ModuleState.DISABLED ? NamedTextColor.RED : NamedTextColor.YELLOW;
            sender.sendMessage(Component.text("- " + module.getName() + " [" + state + "]").color(color));
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
        String message = switch (action) {
            case ENABLE -> success ? "Wlaczono modul '" + name + "'." : "Nie udalo sie wlaczyc modulu '" + name + "'.";
            case DISABLE -> success ? "Wylaczono modul '" + name + "'." : "Nie udalo sie wylaczyc modulu '" + name + "'.";
            case RELOAD -> success ? "Przeladowano modul '" + name + "'." : "Nie udalo sie przeladowac modulu '" + name + "'.";
        };
        sender.sendMessage(Component.text(message).color(success ? NamedTextColor.GREEN : NamedTextColor.RED));
        return Command.SINGLE_SUCCESS;
    }

    private int handleModuleReloadAll(CommandContext<CommandSourceStack> ctx) {
        moduleManager.reloadAllModules();
        sender(ctx).sendMessage(Component.text("Przeladowano wszystkie moduly.").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int handleLang(CommandContext<CommandSourceStack> ctx, LangAction action) {
        CommandSender sender = sender(ctx);
        LangModule langModule = getLangModule();
        if (langModule == null) {
            sender.sendMessage(Component.text("Modul jezykowy nie jest aktywny.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        switch (action) {
            case RELOAD -> {
                langModule.getLangManager().reload();
                sender.sendMessage(Component.text("Przeladowano pliki jezykowe.").color(NamedTextColor.GREEN));
            }
            case RESET -> {
                langModule.getLangManager().resetLanguages();
                sender.sendMessage(Component.text("Zresetowano pliki jezykowe do domyslnych.").color(NamedTextColor.GREEN));
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
        sender(ctx).sendMessage(Component.text("Przeladowano wszystkie pliki konfiguracyjne i moduly.").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int handleAdminDebug(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        DebugManager debugManager = ((FractionCore) plugin).getDebugManager();
        debugManager.setDebugEnabled(enabled);
        sender(ctx).sendMessage(Component.text("Tryb debug: " + (enabled ? "wlaczony" : "wylaczony")).color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int handleAdminPlugin(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = sender(ctx);
        Plugin plugMan = Bukkit.getPluginManager().getPlugin("PlugMan");
        if (plugMan == null || !plugMan.isEnabled()) {
            sender.sendMessage(Component.text("Aby przeladowac plugin bez restartu, zainstaluj PlugMan (lub podobny plugin-manager).").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "plugman reload FractionCore");
        sender.sendMessage(Component.text("Przeladowano plugin FractionCore za pomoca PlugMan.").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int sendAdminUsage(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = sender(ctx);
        sender.sendMessage(Component.text("=== ADMIN FRACTIONCORE ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild admin module <list|enable|disable|reload> [modul]").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin lang <reload|reset> - przeladowanie / reset jezykow").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin reload - przeladowanie konfiguracji").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin debug <true|false> - tryb debug").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin plugin reload - przeladowanie pluginu (wymaga PlugMan)").color(NamedTextColor.YELLOW));
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

    private static int usage(CommandSender sender, String text) {
        sender.sendMessage(Component.text("Uzycie: " + text).color(NamedTextColor.YELLOW));
        return 0;
    }

    private enum ModuleAction { ENABLE, DISABLE, RELOAD }

    private enum LangAction { RELOAD, RESET }
}
