package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Arrays;
import java.util.List;

public class MapModule extends BaseModule {

    public MapModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "map";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("guild", "cuboid");
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
