package pl.Ljimmex.fractionCore.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigSerializable
public class PluginConfig {

    @Comment("General plugin settings")
    private GeneralSettings general = new GeneralSettings();

    @Comment("Language settings")
    private LangSettings lang = new LangSettings();

    @Comment("Database configuration")
    private DatabaseSettings database = new DatabaseSettings();

    @Comment("Module configuration. Set enabled: false to disable a module on startup.")
    private Map<String, ModuleEntry> modules = defaultModules();

    public GeneralSettings getGeneral() {
        return general;
    }

    public LangSettings getLang() {
        return lang;
    }

    public DatabaseSettings getDatabase() {
        return database;
    }

    public Map<String, ModuleEntry> getModules() {
        return modules;
    }

    private static Map<String, ModuleEntry> defaultModules() {
        Map<String, ModuleEntry> defaults = new LinkedHashMap<>();
        defaults.put("guild", new ModuleEntry(true));
        defaults.put("cuboid", new ModuleEntry(true));
        defaults.put("egg", new ModuleEntry(true));
        defaults.put("economy", new ModuleEntry(true));
        defaults.put("ranking", new ModuleEntry(true));
        defaults.put("gui", new ModuleEntry(true));
        defaults.put("tab", new ModuleEntry(true));
        defaults.put("map", new ModuleEntry(true));
        defaults.put("villagers", new ModuleEntry(false));
        defaults.put("lang", new ModuleEntry(true));
        defaults.put("database", new ModuleEntry(true));
        defaults.put("backup", new ModuleEntry(true));
        defaults.put("webhook", new ModuleEntry(false));
        return defaults;
    }
}
