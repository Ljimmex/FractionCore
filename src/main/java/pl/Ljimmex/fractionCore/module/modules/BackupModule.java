package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Collections;
import java.util.List;

public class BackupModule extends BaseModule {

    public BackupModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "backup";
    }

    @Override
    public List<String> getDependencies() {
        return Collections.singletonList("database");
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
