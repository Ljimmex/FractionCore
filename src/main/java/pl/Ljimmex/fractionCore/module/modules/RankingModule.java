package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Collections;
import java.util.List;

public class RankingModule extends BaseModule {

    public RankingModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "ranking";
    }

    @Override
    public List<String> getDependencies() {
        return Collections.singletonList("guild");
    }

    @Override
    public void onEnable() {
        getPlugin().getLogger().info("Ranking module enabled.");
    }

    @Override
    public void onDisable() {
        getPlugin().getLogger().info("Ranking module disabled.");
    }
}
