package pl.Ljimmex.fractionCore.module;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public abstract class ModuleCommand extends Command {

    protected ModuleCommand(String name) {
        super(name);
    }

    protected ModuleCommand(String name, String description, String usageMessage, java.util.List<String> aliases) {
        super(name, description, usageMessage, aliases);
    }

    @Override
    public abstract boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args);
}
