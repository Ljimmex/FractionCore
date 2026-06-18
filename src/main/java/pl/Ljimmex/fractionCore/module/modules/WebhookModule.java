package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.util.Collections;
import java.util.List;

public class WebhookModule extends BaseModule {

    public WebhookModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "webhook";
    }

    @Override
    public List<String> getDependencies() {
        return Collections.singletonList("guild");
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
