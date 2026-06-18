package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Arrays;
import java.util.List;

public class VillagersModule extends BaseModule {

    public VillagersModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "villagers";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("guild", "economy");
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public void onEnable() {
        getPlugin().getLogger().info("Villagers module enabled.");
    }

    @Override
    public void onDisable() {
        getPlugin().getLogger().info("Villagers module disabled.");
    }
}
