package pl.Ljimmex.fractionCore.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import pl.Ljimmex.fractionCore.module.BaseModule;
import pl.Ljimmex.fractionCore.module.ModuleManager;
import pl.Ljimmex.fractionCore.module.ModuleState;

public class GuildAdminCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;

    public GuildAdminCommand(JavaPlugin plugin, ModuleManager moduleManager) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("fractioncore.admin.module")) {
            sender.sendMessage(Component.text("Brak uprawnien.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("admin") || !args[1].equalsIgnoreCase("module")) {
            sendUsage(sender);
            return true;
        }

        if (args.length == 2) {
            sendUsage(sender);
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
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
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
}
