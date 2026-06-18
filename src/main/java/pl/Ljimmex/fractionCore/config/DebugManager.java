package pl.Ljimmex.fractionCore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class DebugManager {

    private final JavaPlugin plugin;
    private boolean debugEnabled;

    public DebugManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfiguration(FileConfiguration config) {
        this.debugEnabled = config.getBoolean("general.debug", false);
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        plugin.getConfig().set("general.debug", enabled);
        plugin.saveConfig();
    }

    public void log(String module, String message) {
        if (debugEnabled) {
            plugin.getLogger().log(Level.INFO, "[DEBUG][" + module + "] " + message);
        }
    }

    public void log(String module, String message, Throwable throwable) {
        if (debugEnabled) {
            plugin.getLogger().log(Level.INFO, "[DEBUG][" + module + "] " + message, throwable);
        }
    }
}
