package pl.Ljimmex.fractionCore;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.command.GuildCommand;
import pl.Ljimmex.fractionCore.config.ConfigManager;
import pl.Ljimmex.fractionCore.config.DebugManager;
import pl.Ljimmex.fractionCore.module.ModuleManager;
import pl.Ljimmex.fractionCore.module.modules.BackupModule;
import pl.Ljimmex.fractionCore.module.modules.CuboidModule;
import pl.Ljimmex.fractionCore.module.modules.DatabaseModule;
import pl.Ljimmex.fractionCore.module.modules.EconomyModule;
import pl.Ljimmex.fractionCore.module.modules.EggModule;
import pl.Ljimmex.fractionCore.module.modules.GuiModule;
import pl.Ljimmex.fractionCore.module.modules.GuildModule;
import pl.Ljimmex.fractionCore.module.modules.JoinItemsModule;
import pl.Ljimmex.fractionCore.module.modules.LangModule;
import pl.Ljimmex.fractionCore.module.modules.MapModule;
import pl.Ljimmex.fractionCore.module.modules.RankingModule;
import pl.Ljimmex.fractionCore.module.modules.TabModule;
import pl.Ljimmex.fractionCore.module.modules.VillagersModule;
import pl.Ljimmex.fractionCore.module.modules.WebhookModule;

import java.util.Objects;

public final class FractionCore extends JavaPlugin {

    private ConfigManager configManager;
    private DebugManager debugManager;
    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.initialize();

        debugManager = new DebugManager(this);
        debugManager.loadConfiguration(getConfig());

        moduleManager = new ModuleManager(this);
        registerModules();

        FileConfiguration config = getConfig();
        moduleManager.loadConfiguration(config);
        moduleManager.enableModules();

        Objects.requireNonNull(getCommand("guild")).setExecutor(new GuildCommand(this, moduleManager));

        getLogger().info("FractionCore enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableModules();
        }
        getLogger().info("FractionCore disabled.");
    }

    private void registerModules() {
        moduleManager.registerModule(new DatabaseModule(this));
        moduleManager.registerModule(new LangModule(this));
        moduleManager.registerModule(new EconomyModule(this));
        moduleManager.registerModule(new GuildModule(this));
        moduleManager.registerModule(new CuboidModule(this));
        moduleManager.registerModule(new EggModule(this));
        moduleManager.registerModule(new RankingModule(this));
        moduleManager.registerModule(new GuiModule(this));
        moduleManager.registerModule(new TabModule(this));
        moduleManager.registerModule(new MapModule(this));
        moduleManager.registerModule(new VillagersModule(this));
        moduleManager.registerModule(new JoinItemsModule(this));
        moduleManager.registerModule(new BackupModule(this));
        moduleManager.registerModule(new WebhookModule(this));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DebugManager getDebugManager() {
        return debugManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}
