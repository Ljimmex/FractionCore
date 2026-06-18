package pl.Ljimmex.fractionCore.module;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseModule {

    private final JavaPlugin plugin;
    private ModuleState state = ModuleState.DISABLED;
    private final List<Listener> listeners = new ArrayList<>();
    private final List<Command> commands = new ArrayList<>();

    protected BaseModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract String getName();

    public List<String> getDependencies() {
        return Collections.emptyList();
    }

    public boolean isEnabledByDefault() {
        return true;
    }

    public final JavaPlugin getPlugin() {
        return plugin;
    }

    public final ModuleState getState() {
        return state;
    }

    public final void setState(ModuleState state) {
        this.state = state;
    }

    public abstract void onEnable();

    public abstract void onDisable();

    public void onReload() {
        onDisable();
        onEnable();
    }

    protected final void registerListener(Listener listener) {
        if (!listeners.contains(listener)) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            listeners.add(listener);
        }
    }

    protected final void registerCommand(Command command) {
        if (!commands.contains(command)) {
            getCommandMap().register(plugin.getName().toLowerCase(), command);
            commands.add(command);
        }
    }

    protected final void unregisterListeners() {
        for (Listener listener : new ArrayList<>(listeners)) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
    }

    protected final void unregisterCommands() {
        CommandMap commandMap = getCommandMap();
        for (Command command : new ArrayList<>(commands)) {
            command.unregister(commandMap);
        }
        commands.clear();
    }

    public final void unregisterAll() {
        unregisterListeners();
        unregisterCommands();
    }

    public final List<Listener> getRegisteredListeners() {
        return new ArrayList<>(listeners);
    }

    public final List<Command> getRegisteredCommands() {
        return new ArrayList<>(commands);
    }

    private CommandMap getCommandMap() {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(plugin.getServer());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Could not access CommandMap", e);
        }
    }
}
