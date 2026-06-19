package pl.Ljimmex.fractionCore.lang;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class LangManager {

    private static final String FALLBACK_LANGUAGE = "en_US";

    private final JavaPlugin plugin;
    private final File langDirectory;
    private final Map<String, Map<String, String>> languages = new HashMap<>();
    private String defaultLanguage;

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.langDirectory = new File(plugin.getDataFolder(), "lang");
        this.defaultLanguage = "pl_PL";
    }

    public void loadConfiguration(FileConfiguration config) {
        this.defaultLanguage = config.getString("lang.default", "pl_PL");
    }

    public void loadLanguages() {
        languages.clear();

        if (!langDirectory.exists()) {
            langDirectory.mkdirs();
        }

        saveDefaultLanguageFiles();

        File[] files = langDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No language files found in " + langDirectory.getPath());
            return;
        }

        for (File file : files) {
            String languageCode = file.getName().replace(".yml", "");
            try {
                FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                Map<String, String> messages = flattenConfiguration(configuration);
                languages.put(languageCode, messages);
                plugin.getLogger().info("Loaded language '" + languageCode + "' (" + messages.size() + " keys).");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load language file " + file.getName(), e);
            }
        }
    }

    public void reload() {
        loadLanguages();
        plugin.getLogger().info("Languages reloaded.");
    }

    public Component getMessage(String key) {
        return getMessage(key, MessageType.INFO, PlaceholderContext.empty());
    }

    public Component getMessage(String key, MessageType type) {
        return getMessage(key, type, PlaceholderContext.empty());
    }

    public Component getMessage(String key, PlaceholderContext context) {
        return getMessage(key, MessageType.INFO, context);
    }

    public Component getMessage(String key, MessageType type, PlaceholderContext context) {
        return getMessage(key, type, context, defaultLanguage);
    }

    public Component getMessage(String key, MessageType type, PlaceholderContext context, String languageCode) {
        String rawMessage = getRawMessage(key, languageCode, true);
        if (rawMessage == null) {
            rawMessage = key;
        }

        String withPlaceholders = PlaceholderResolver.resolve(rawMessage, context);
        String withPrefix = applyPrefix(withPlaceholders, type, languageCode);

        return MessageParser.parse(withPrefix);
    }

    public String getRawMessage(String key) {
        return getRawMessage(key, defaultLanguage, true);
    }

    public String getRawMessage(String key, String languageCode) {
        return getRawMessage(key, languageCode, true);
    }

    private String getRawMessage(String key, String languageCode, boolean useFallback) {
        Map<String, String> messages = languages.get(languageCode);
        if (messages != null && messages.containsKey(key)) {
            return messages.get(key);
        }

        if (useFallback && !FALLBACK_LANGUAGE.equalsIgnoreCase(languageCode)) {
            return getRawMessage(key, FALLBACK_LANGUAGE, false);
        }

        return null;
    }

    private String applyPrefix(String message, MessageType type, String languageCode) {
        String prefixKey = "prefix." + type.name().toLowerCase();
        String prefix = getRawMessage(prefixKey, languageCode, true);
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + " " + message;
        }
        return message;
    }

    private Map<String, String> flattenConfiguration(FileConfiguration configuration) {
        Map<String, String> result = new HashMap<>();
        for (String key : configuration.getKeys(true)) {
            if (configuration.isString(key)) {
                result.put(key, configuration.getString(key));
            }
        }
        return result;
    }

    private void saveDefaultLanguageFiles() {
        saveDefaultLanguageFile("pl_PL.yml");
        saveDefaultLanguageFile("en_US.yml");
    }

    private void saveDefaultLanguageFile(String fileName) {
        File file = new File(langDirectory, fileName);
        if (file.exists()) {
            return;
        }

        try (InputStream inputStream = plugin.getResource("lang/" + fileName)) {
            if (inputStream != null) {
                Files.copy(inputStream, file.toPath());
                plugin.getLogger().info("Saved default language file: " + fileName);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save default language file " + fileName, e);
        }
    }

    public Set<String> getLoadedLanguages() {
        return Collections.unmodifiableSet(languages.keySet());
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
}
