package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Arrays;
import java.util.List;

public class GuiModule extends BaseModule {

    public GuiModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("guild", "cuboid");
    }

    @Override
    public void onEnable() {
        getPlugin().getLogger().info("GUI module enabled.");
    }

    @Override
    public void onDisable() {
        getPlugin().getLogger().info("GUI module disabled.");
    }
}
