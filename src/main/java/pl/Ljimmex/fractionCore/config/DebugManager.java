package pl.Ljimmex.fractionCore.config;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.config.model.PluginConfig;

import java.util.logging.Level;

public class DebugManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private boolean debugEnabled;

    public DebugManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void loadConfiguration(PluginConfig config) {
        this.debugEnabled = config.getGeneral().isDebug();
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        configManager.setDebugEnabled(enabled);
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
