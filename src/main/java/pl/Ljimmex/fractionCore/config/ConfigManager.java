package pl.Ljimmex.fractionCore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private static final int CURRENT_SCHEMA_VERSION = 2;

    private final JavaPlugin plugin;
    private final File modulesDirectory;
    private final Map<String, FileConfiguration> moduleConfigs = new HashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.modulesDirectory = new File(plugin.getDataFolder(), "modules");
    }

    public void initialize() {
        if (!modulesDirectory.exists()) {
            modulesDirectory.mkdirs();
        }

        plugin.saveDefaultConfig();
        migrateMainConfig();

        loadModuleConfig("guild.yml");
        loadModuleConfig("cuboid.yml");
        loadModuleConfig("egg.yml");
        loadModuleConfig("economy.yml");
        loadModuleConfig("ranking.yml");
        loadModuleConfig("gui.yml");
        loadModuleConfig("tab.yml");
        loadModuleConfig("map.yml");
        loadModuleConfig("villagers.yml");
        loadModuleConfig("backup.yml");
        loadModuleConfig("webhook.yml");
    }

    public void reload() {
        plugin.reloadConfig();
        migrateMainConfig();

        for (String fileName : moduleConfigs.keySet()) {
            loadModuleConfig(fileName);
        }
    }

    public FileConfiguration getModuleConfig(String moduleName) {
        String fileName = moduleName.toLowerCase() + ".yml";
        return moduleConfigs.computeIfAbsent(fileName, this::loadModuleConfig);
    }

    private void migrateMainConfig() {
        FileConfiguration config = plugin.getConfig();
        int schemaVersion = config.getInt("general.version", 0);

        if (schemaVersion < CURRENT_SCHEMA_VERSION) {
            plugin.getLogger().info("Migrating config.yml from version " + schemaVersion + " to " + CURRENT_SCHEMA_VERSION);

            setDefault(config, "general.version", CURRENT_SCHEMA_VERSION);
            setDefault(config, "general.language", "pl_PL");
            setDefault(config, "general.debug", false);
            setDefault(config, "general.prefix", "<dark_gray>[<aqua>FGC<dark_gray>] <gray>");
            setDefault(config, "general.custom-join-message.enabled", true);
            setDefault(config, "general.custom-join-message.format", "<dark_gray>[<green><bold>+</bold></green>] <gray>{player}");
            setDefault(config, "general.custom-quit-message.enabled", true);
            setDefault(config, "general.custom-quit-message.format", "<dark_gray>[<red><bold>-</bold></red>] <gray>{player}");
            setDefault(config, "lang.default", "pl_PL");

            if (!config.contains("modules")) {
                setDefault(config, "modules.guild.enabled", true);
                setDefault(config, "modules.cuboid.enabled", true);
                setDefault(config, "modules.egg.enabled", true);
                setDefault(config, "modules.economy.enabled", true);
                setDefault(config, "modules.ranking.enabled", true);
                setDefault(config, "modules.gui.enabled", true);
                setDefault(config, "modules.tab.enabled", true);
                setDefault(config, "modules.map.enabled", true);
                setDefault(config, "modules.villagers.enabled", false);
                setDefault(config, "modules.lang.enabled", true);
                setDefault(config, "modules.database.enabled", true);
                setDefault(config, "modules.backup.enabled", true);
                setDefault(config, "modules.webhook.enabled", false);
            }

            if (!config.contains("database")) {
                setDefault(config, "database.type", "SQLITE");
                setDefault(config, "database.sqlite.file", "data/database.db");
                setDefault(config, "database.mysql.host", "localhost");
                setDefault(config, "database.mysql.port", 3306);
                setDefault(config, "database.mysql.database", "fractioncore");
                setDefault(config, "database.mysql.username", "root");
                setDefault(config, "database.mysql.password", "");
                setDefault(config, "database.postgresql.host", "localhost");
                setDefault(config, "database.postgresql.port", 5432);
                setDefault(config, "database.postgresql.database", "fractioncore");
                setDefault(config, "database.postgresql.username", "postgres");
                setDefault(config, "database.postgresql.password", "");
                setDefault(config, "database.pool.max-size", 10);
                setDefault(config, "database.pool.connection-timeout", 30000);
                setDefault(config, "database.pool.idle-timeout", 600000);
                setDefault(config, "database.pool.max-lifetime", 1800000);
            }

            plugin.saveConfig();
            plugin.getLogger().info("Config.yml migration completed.");
        }
    }

    private void setDefault(FileConfiguration config, String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }

    private FileConfiguration loadModuleConfig(String fileName) {
        File file = new File(modulesDirectory, fileName);
        if (!file.exists()) {
            saveDefaultModuleConfig(fileName);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        FileConfiguration defaults = mergeModuleDefaults(fileName, file, config);
        if ("guild.yml".equals(fileName)) {
            migrateGuildChatFormat(config, defaults, file);
        }
        moduleConfigs.put(fileName, config);
        return config;
    }

    private void saveDefaultModuleConfig(String fileName) {
        File file = new File(modulesDirectory, fileName);
        try (InputStream inputStream = plugin.getResource("modules/" + fileName)) {
            if (inputStream != null) {
                Files.copy(inputStream, file.toPath());
                plugin.getLogger().info("Saved default module config: " + fileName);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default module config " + fileName, e);
        }
    }

    private FileConfiguration mergeModuleDefaults(String fileName, File file, FileConfiguration config) {
        FileConfiguration defaults = null;
        try (InputStream inputStream = plugin.getResource("modules/" + fileName)) {
            if (inputStream == null) {
                return new YamlConfiguration();
            }
            defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key)) {
                    continue;
                }
                if (!config.contains(key)) {
                    config.set(key, defaults.get(key));
                    changed = true;
                }
            }
            if (changed) {
                config.save(file);
                plugin.getLogger().info("Updated module config with missing keys: " + fileName);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to merge defaults for module config " + fileName, e);
        }
        return defaults != null ? defaults : new YamlConfiguration();
    }

    private void migrateGuildChatFormat(FileConfiguration config, FileConfiguration defaults, File file) {
        String format = config.getString("chat.format", "");
        if (format.contains("{rank}") || format.contains("<yellow>[{rank_letter}]</yellow>")) {
            String newFormat = defaults.getString("chat.format",
                    "<dark_gray>[<aqua>{tag}</aqua>]{rank_letter} <white>{player}<gray>: ");
            config.set("chat.format", newFormat);
            try {
                config.save(file);
                plugin.getLogger().info("Migrated guild chat format to the new version (removed {rank} placeholder).");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to migrate guild chat format", e);
            }
        }
    }

    public File getModulesDirectory() {
        return modulesDirectory;
    }
}
