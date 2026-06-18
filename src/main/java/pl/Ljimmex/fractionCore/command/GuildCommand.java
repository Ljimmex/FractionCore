package pl.Ljimmex.fractionCore.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import pl.Ljimmex.fractionCore.config.DebugManager;
import pl.Ljimmex.fractionCore.module.BaseModule;
import pl.Ljimmex.fractionCore.module.ModuleManager;
import pl.Ljimmex.fractionCore.module.ModuleState;
import pl.Ljimmex.fractionCore.module.modules.LangModule;

public class GuildCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;

    public GuildCommand(JavaPlugin plugin, ModuleManager moduleManager) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "admin":
                return handleAdmin(sender, args);
            case "help":
                sendUsage(sender);
                return true;
            default:
                sender.sendMessage(Component.text("Nieznana komenda. Uzyj /guild help.").color(NamedTextColor.RED));
                return true;
        }
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fractioncore.admin")) {
            sender.sendMessage(Component.text("Brak uprawnien.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uzycie: /guild admin <module|lang> ...").color(NamedTextColor.YELLOW));
            return true;
        }

        String adminSub = args[1].toLowerCase();

        switch (adminSub) {
            case "module":
                return handleAdminModule(sender, args);
            case "lang":
                return handleAdminLang(sender, args);
            case "reload":
                return handleAdminReload(sender);
            case "debug":
                return handleAdminDebug(sender, args);
            default:
                sender.sendMessage(Component.text("Uzycie: /guild admin <module|lang|reload|debug> ...").color(NamedTextColor.YELLOW));
                return true;
        }
    }

    private boolean handleAdminModule(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fractioncore.admin.module")) {
            sender.sendMessage(Component.text("Brak uprawnien.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sendModuleUsage(sender);
            return true;
        }

        String subCommand = args[2].toLowerCase();

        switch (subCommand) {
            case "list":
                sendModuleList(sender);
                return true;
            case "enable":
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uzycie: /guild admin module enable <modul>").color(NamedTextColor.RED));
                    return true;
                }
                handleEnable(sender, args[3]);
                return true;
            case "disable":
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uzycie: /guild admin module disable <modul>").color(NamedTextColor.RED));
                    return true;
                }
                handleDisable(sender, args[3]);
                return true;
            case "reload":
                if (args.length < 4) {
                    moduleManager.reloadAllModules();
                    sender.sendMessage(Component.text("Przeladowano wszystkie moduly.").color(NamedTextColor.GREEN));
                } else {
                    handleReload(sender, args[3]);
                }
                return true;
            default:
                sendModuleUsage(sender);
                return true;
        }
    }

    private boolean handleAdminLang(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fractioncore.admin.lang.reload")) {
            sender.sendMessage(Component.text("Brak uprawnien.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 3 || !args[2].equalsIgnoreCase("reload")) {
            sender.sendMessage(Component.text("Uzycie: /guild admin lang reload").color(NamedTextColor.YELLOW));
            return true;
        }

        LangModule langModule = getLangModule();
        if (langModule == null) {
            sender.sendMessage(Component.text("Modul jezykowy nie jest aktywny.").color(NamedTextColor.RED));
            return true;
        }

        langModule.getLangManager().reload();
        sender.sendMessage(Component.text("Przeladowano pliki jezykowe.").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAdminReload(CommandSender sender) {
        if (!sender.hasPermission("fractioncore.admin.reload")) {
            sender.sendMessage(Component.text("Brak uprawnien.").color(NamedTextColor.RED));
            return true;
        }

        plugin.reloadConfig();
        ((pl.Ljimmex.fractionCore.FractionCore) plugin).getConfigManager().reload();
        moduleManager.reloadAllModules();
        sender.sendMessage(Component.text("Przeladowano wszystkie pliki konfiguracyjne i moduly.").color(NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAdminDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fractioncore.admin.debug")) {
            sender.sendMessage(Component.text("Brak uprawnien.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uzycie: /guild admin debug <true|false>").color(NamedTextColor.YELLOW));
            return true;
        }

        boolean enabled = Boolean.parseBoolean(args[2]);
        DebugManager debugManager = ((pl.Ljimmex.fractionCore.FractionCore) plugin).getDebugManager();
        debugManager.setDebugEnabled(enabled);
        sender.sendMessage(Component.text("Tryb debug: " + (enabled ? "wlaczony" : "wylaczony")).color(NamedTextColor.GREEN));
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== FRACTIONCORE ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild help - pomoc").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin module <list|enable|disable|reload> [modul]").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin lang reload - przeladowanie jezykow").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin reload - przeladowanie konfiguracji").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin debug <true|false> - tryb debug").color(NamedTextColor.YELLOW));
    }

    private void sendModuleUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uzycie: /guild admin module <list|enable|disable|reload> [modul]").color(NamedTextColor.YELLOW));
    }

    private void sendModuleList(CommandSender sender) {
        sender.sendMessage(Component.text("=== MODULY ===").color(NamedTextColor.GOLD));
        for (BaseModule module : moduleManager.getModules()) {
            ModuleState state = module.getState();
            NamedTextColor color = state == ModuleState.RUNNING ? NamedTextColor.GREEN :
                    state == ModuleState.DISABLED ? NamedTextColor.RED : NamedTextColor.YELLOW;
            sender.sendMessage(Component.text("- " + module.getName() + " [" + state + "]").color(color));
        }
    }

    private void handleEnable(CommandSender sender, String name) {
        if (moduleManager.enableModule(name)) {
            sender.sendMessage(Component.text("Wlaczono modul '" + name + "'.").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Nie udalo sie wlaczyc modulu '" + name + "'.").color(NamedTextColor.RED));
        }
    }

    private void handleDisable(CommandSender sender, String name) {
        if (moduleManager.disableModule(name)) {
            sender.sendMessage(Component.text("Wylaczono modul '" + name + "'.").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Nie udalo sie wylaczyc modulu '" + name + "'.").color(NamedTextColor.RED));
        }
    }

    private void handleReload(CommandSender sender, String name) {
        if (moduleManager.reloadModule(name)) {
            sender.sendMessage(Component.text("Przeladowano modul '" + name + "'.").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Nie udalo sie przeladowac modulu '" + name + "'.").color(NamedTextColor.RED));
        }
    }

    private LangModule getLangModule() {
        BaseModule module = moduleManager.getModule("lang");
        return module instanceof LangModule ? (LangModule) module : null;
    }
}
