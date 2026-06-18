package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Arrays;
import java.util.List;

public class GuildModule extends BaseModule {

    public GuildModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "guild";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("database", "lang");
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
