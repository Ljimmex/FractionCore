package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.module.BaseModule;

public class LangModule extends BaseModule {

    private LangManager langManager;

    public LangModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "lang";
    }

    @Override
    public void onEnable() {
        langManager = new LangManager(getPlugin());
        langManager.loadConfiguration(getPlugin().getConfig());
        langManager.loadLanguages();
        getPlugin().getLogger().info("Lang module enabled. Loaded languages: " + langManager.getLoadedLanguages());
    }

    @Override
    public void onDisable() {
        langManager = null;
    }

    @Override
    public void onReload() {
        if (langManager != null) {
            langManager.reload();
        }
    }

    public LangManager getLangManager() {
        return langManager;
    }
}
