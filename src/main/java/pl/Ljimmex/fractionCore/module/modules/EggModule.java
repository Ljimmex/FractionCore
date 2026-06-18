package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Arrays;
import java.util.List;

public class EggModule extends BaseModule {

    public EggModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "egg";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("cuboid", "guild");
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
