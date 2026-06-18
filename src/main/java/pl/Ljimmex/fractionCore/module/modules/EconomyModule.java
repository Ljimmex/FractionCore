package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

public class EconomyModule extends BaseModule {

    public EconomyModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "economy";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public void onEnable() {
        getPlugin().getLogger().info("Economy module enabled.");
    }

    @Override
    public void onDisable() {
        getPlugin().getLogger().info("Economy module disabled.");
    }
}
