package pl.Ljimmex.fractionCore.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.config.model.ConnectionSettings;
import pl.Ljimmex.fractionCore.config.model.PluginConfig;
import pl.Ljimmex.fractionCore.database.config.DatabaseConfig;
import pl.Ljimmex.fractionCore.database.config.DatabaseType;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {

    private static final String CONFIG_FILE = "config.yml";
    private static final String MODULES_DIR = "modules";
    private static final List<String> MODULE_CONFIG_FILES = List.of(
            "guild.yml", "cuboid.yml", "egg.yml", "economy.yml", "ranking.yml",
            "gui.yml", "tab.yml", "map.yml", "villagers.yml", "backup.yml", "webhook.yml"
    );

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File modulesDirectory;
    private final Map<String, ModuleConfig> moduleConfigs = new HashMap<>();

    private YamlConfigurationLoader mainLoader;
    private CommentedConfigurationNode mainNode;
    private PluginConfig pluginConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.modulesDirectory = new File(plugin.getDataFolder(), MODULES_DIR);
    }

    public void initialize() {
        loadMainConfig();
        validateMainConfig();

        if (!modulesDirectory.exists()) {
            modulesDirectory.mkdirs();
        }

        for (String fileName : MODULE_CONFIG_FILES) {
            loadModuleConfig(fileName);
        }
    }

    public void reload() {
        loadMainConfig();
        validateMainConfig();

        for (String fileName : MODULE_CONFIG_FILES) {
            loadModuleConfig(fileName);
        }
    }

    public void saveMainConfig() {
        try {
            mainLoader.save(mainNode);
        } catch (ConfigurateException e) {
            logger.log(Level.SEVERE, "Failed to save main config", e);
        }
    }

    public void setDebugEnabled(boolean enabled) {
        pluginConfig.getGeneral().setDebug(enabled);
        try {
            mainNode.node("general", "debug").set(enabled);
            mainLoader.save(mainNode);
        } catch (ConfigurateException e) {
            logger.log(Level.SEVERE, "Failed to save debug setting", e);
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public DatabaseConfig getDatabaseConfig() {
        var db = pluginConfig.getDatabase();
        DatabaseType type;
        try {
            type = DatabaseType.valueOf(db.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown database type '" + db.getType() + "', falling back to SQLITE.");
            type = DatabaseType.SQLITE;
        }

        ConnectionSettings connection = type == DatabaseType.POSTGRESQL ? db.getPostgresql() : db.getMysql();
        return new DatabaseConfig(
                type,
                db.getSqlite().getFile(),
                connection.getHost(),
                connection.getPort(),
                connection.getDatabase(),
                connection.getUsername(),
                connection.getPassword(),
                db.getPool().getMaxSize(),
                db.getPool().getConnectionTimeout(),
                db.getPool().getIdleTimeout(),
                db.getPool().getMaxLifetime()
        );
    }

    public ModuleConfig getModuleConfig(String moduleName) {
        return moduleConfigs.get(moduleName.toLowerCase() + ".yml");
    }

    public File getModulesDirectory() {
        return modulesDirectory;
    }

    private void loadMainConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, CONFIG_FILE);
        if (!configFile.exists()) {
            plugin.saveResource(CONFIG_FILE, false);
        }

        mainLoader = newLoader(configFile.toPath());
        try {
            mainNode = mainLoader.load();
            mergeDefaults(mainNode, newDefaultNode(PluginConfig.class, new PluginConfig()));
            mainLoader.save(mainNode);
            pluginConfig = mainNode.get(PluginConfig.class);
        } catch (ConfigurateException e) {
            logger.log(Level.SEVERE, "Failed to load main config, using defaults", e);
            pluginConfig = new PluginConfig();
        }
    }

    private void validateMainConfig() {
        var db = pluginConfig.getDatabase();

        validatePort("database.mysql.port", db.getMysql().getPort());
        validatePort("database.postgresql.port", db.getPostgresql().getPort());

        if (db.getPool().getMaxSize() <= 0) {
            logger.warning("Invalid database.pool.max-size, must be positive");
        }
        if (db.getPool().getConnectionTimeout() < 0 || db.getPool().getIdleTimeout() < 0 || db.getPool().getMaxLifetime() < 0) {
            logger.warning("Invalid database pool timeout values, must be non-negative");
        }
    }

    private void validatePort(String path, int port) {
        if (port <= 0 || port > 65535) {
            logger.warning("Invalid port at " + path + ": " + port + ", using default");
        }
    }

    private void loadModuleConfig(String fileName) {
        File file = new File(modulesDirectory, fileName);
        if (!file.exists()) {
            saveDefaultModuleConfig(fileName);
        }

        YamlConfigurationLoader loader = newLoader(file.toPath());
        try {
            CommentedConfigurationNode node = loader.load();
            URL resource = plugin.getClass().getResource("/" + MODULES_DIR + "/" + fileName);
            if (resource != null) {
                CommentedConfigurationNode defaults = newLoader(resource).load();
                migrateLegacyModuleValues(fileName, node, defaults);
                node.mergeFrom(defaults);
                loader.save(node);
            }
            moduleConfigs.put(fileName, new ModuleConfig(node, logger));
        } catch (ConfigurateException e) {
            logger.log(Level.SEVERE, "Failed to load module config " + fileName + ", using empty config", e);
            moduleConfigs.put(fileName, new ModuleConfig(loader.createNode(), logger));
        }
    }

    private void saveDefaultModuleConfig(String fileName) {
        try (var in = plugin.getResource(MODULES_DIR + "/" + fileName)) {
            if (in == null) {
                logger.warning("Missing default module config resource: " + fileName);
                return;
            }
            File target = new File(modulesDirectory, fileName);
            target.getParentFile().mkdirs();
            Files.copy(in, target.toPath());
            logger.info("Saved default module config: " + fileName);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save default module config " + fileName, e);
        }
    }

    private void migrateLegacyModuleValues(String fileName, CommentedConfigurationNode node, CommentedConfigurationNode defaults) {
        if (!"guild.yml".equals(fileName)) {
            return;
        }

        try {
            CommentedConfigurationNode chatFormatNode = node.node("chat", "format");
            String format = chatFormatNode.getString("");
            if (format.contains("{rank}") || format.contains("<yellow>[{rank_letter}]</yellow>") || format.contains("</")) {
                String newFormat = defaults.node("chat", "format").getString(
                        "<dark_gray>[{tag}]{rank_letter} <white>{player}<gray>: ");
                chatFormatNode.set(newFormat);
                logger.info("Migrated guild chat format to the new version.");
            }

            CommentedConfigurationNode neutralNode = node.node("relation-colors", "neutral");
            if ("<white>".equalsIgnoreCase(neutralNode.getString(""))) {
                String newColor = defaults.node("relation-colors", "neutral").getString("<gray>");
                neutralNode.set(newColor);
                logger.info("Migrated neutral relation color from <white> to " + newColor + ".");
            }
        } catch (ConfigurateException e) {
            logger.log(Level.WARNING, "Failed to migrate legacy guild config values", e);
        }
    }

    private static <T> CommentedConfigurationNode newDefaultNode(Class<T> clazz, T instance) throws ConfigurateException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().build();
        CommentedConfigurationNode node = loader.createNode();
        node.set(clazz, instance);
        return node;
    }

    private static void mergeDefaults(CommentedConfigurationNode node, CommentedConfigurationNode defaults) {
        node.mergeFrom(defaults);
    }

    private static YamlConfigurationLoader newLoader(Path path) {
        return YamlConfigurationLoader.builder()
                .path(path)
                .defaultOptions(opts -> opts
                        .shouldCopyDefaults(true)
                        .serializers(builder -> builder.registerAnnotatedObjects(ObjectMapper.factory())))
                .build();
    }

    private static YamlConfigurationLoader newLoader(URL url) {
        return YamlConfigurationLoader.builder()
                .url(url)
                .defaultOptions(opts -> opts
                        .shouldCopyDefaults(true)
                        .serializers(builder -> builder.registerAnnotatedObjects(ObjectMapper.factory())))
                .build();
    }
}
