package pl.Ljimmex.fractionCore.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModuleConfig {

    private final CommentedConfigurationNode root;
    private final Logger logger;

    public ModuleConfig(CommentedConfigurationNode root, Logger logger) {
        this.root = root;
        this.logger = logger;
    }

    public CommentedConfigurationNode node(String path) {
        return root.node((Object[]) path.split("\\."));
    }

    public String getString(String path, String def) {
        return node(path).getString(def);
    }

    public int getInt(String path, int def) {
        return node(path).getInt(def);
    }

    public long getLong(String path, long def) {
        return node(path).getLong(def);
    }

    public boolean getBoolean(String path, boolean def) {
        return node(path).getBoolean(def);
    }

    public double getDouble(String path, double def) {
        return node(path).getDouble(def);
    }

    public List<String> getStringList(String path) {
        try {
            List<String> list = node(path).getList(String.class);
            return list != null ? list : Collections.emptyList();
        } catch (SerializationException e) {
            logger.log(Level.WARNING, "Failed to read string list at " + path, e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMapList(String path) {
        try {
            List<Map<String, Object>> list = (List<Map<String, Object>>) (Object) node(path).getList(Map.class);
            return list != null ? list : Collections.emptyList();
        } catch (SerializationException e) {
            logger.log(Level.WARNING, "Failed to read map list at " + path, e);
            return Collections.emptyList();
        }
    }

    public CommentedConfigurationNode getRoot() {
        return root;
    }
}
