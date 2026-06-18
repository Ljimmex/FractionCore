package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Arrays;
import java.util.List;

public class TabModule extends BaseModule {

    public TabModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "tab";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("guild", "ranking");
    }

    @Override
    public void onEnable() {
        getPlugin().getLogger().info("TAB module enabled.");
    }

    @Override
    public void onDisable() {
        getPlugin().getLogger().info("TAB module disabled.");
    }
}
