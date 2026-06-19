package pl.Ljimmex.fractionCore.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import pl.Ljimmex.fractionCore.module.ModuleManager;

public class GuildCommand implements CommandExecutor {

    private final GuildAdminCommand adminCommand;

    public GuildCommand(JavaPlugin plugin, ModuleManager moduleManager) {
        this.adminCommand = new GuildAdminCommand(plugin, moduleManager);
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
                String[] adminArgs = new String[args.length - 1];
                System.arraycopy(args, 1, adminArgs, 0, adminArgs.length);
                return adminCommand.onCommand(sender, command, label, adminArgs);
            case "help":
                sendUsage(sender);
                return true;
            default:
                sender.sendMessage(Component.text("Nieznana komenda. Uzyj /guild help.").color(NamedTextColor.RED));
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== FRACTIONCORE ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild help - pomoc").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild admin - komendy administratorskie").color(NamedTextColor.YELLOW));
    }
}
