package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Arrays;
import java.util.List;

public class CuboidModule extends BaseModule {

    public CuboidModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "cuboid";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("guild");
    }

    @Override
    public void onEnable() {
        getPlugin().getLogger().info("Cuboid module enabled.");
    }

    @Override
    public void onDisable() {
        getPlugin().getLogger().info("Cuboid module disabled.");
    }
}
