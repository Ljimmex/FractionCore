package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

public class JoinItemsModule extends BaseModule {

    public JoinItemsModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "join_items";
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public void onEnable() {
        getPlugin().getLogger().info("JoinItems module enabled.");
    }

    @Override
    public void onDisable() {
        getPlugin().getLogger().info("JoinItems module disabled.");
    }
}
