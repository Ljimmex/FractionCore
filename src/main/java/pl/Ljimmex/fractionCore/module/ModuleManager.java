package pl.Ljimmex.fractionCore.module;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.config.model.PluginConfig;

import java.util.*;

public class ModuleManager {

    private final JavaPlugin plugin;
    private final Map<String, BaseModule> modules = new LinkedHashMap<>();
    private final Map<String, Boolean> userEnabledConfig = new HashMap<>();

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerModule(BaseModule module) {
        modules.put(module.getName().toLowerCase(), module);
    }

    public BaseModule getModule(String name) {
        return modules.get(name.toLowerCase());
    }

    public Collection<BaseModule> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public void loadConfiguration(PluginConfig config) {
        userEnabledConfig.clear();
        for (Map.Entry<String, ?> entry : config.getModules().entrySet()) {
            userEnabledConfig.put(entry.getKey().toLowerCase(),
                    entry.getValue() instanceof pl.Ljimmex.fractionCore.config.model.ModuleEntry moduleEntry
                            ? moduleEntry.isEnabled() : Boolean.TRUE);
        }
    }

    public void enableModules() {
        List<BaseModule> sorted = topologicalSort();
        for (BaseModule module : sorted) {
            enableModule(module.getName());
        }
    }

    public void disableModules() {
        List<BaseModule> sorted = new ArrayList<>(modules.values());
        Collections.reverse(sorted);
        for (BaseModule module : sorted) {
            disableModule(module.getName());
        }
    }

    public boolean enableModule(String name) {
        BaseModule module = getModule(name);
        if (module == null) {
            plugin.getLogger().warning("Module '" + name + "' not found.");
            return false;
        }

        if (module.getState() == ModuleState.RUNNING) {
            return true;
        }

        if (!isEnabledInConfig(module)) {
            plugin.getLogger().info("Module '" + module.getName() + "' is disabled in config.");
            module.setState(ModuleState.DISABLED);
            return false;
        }

        for (String dependency : module.getDependencies()) {
            BaseModule depModule = getModule(dependency);
            if (depModule == null) {
                plugin.getLogger().severe("Module '" + module.getName() + "' depends on missing module '" + dependency + "'.");
                module.setState(ModuleState.DISABLED);
                return false;
            }
            if (depModule.getState() != ModuleState.RUNNING) {
                if (!enableModule(dependency)) {
                    plugin.getLogger().severe("Module '" + module.getName() + "' could not be enabled because dependency '" + dependency + "' failed.");
                    module.setState(ModuleState.DISABLED);
                    return false;
                }
            }
        }

        module.setState(ModuleState.LOADING);
        try {
            module.onEnable();
            module.setState(ModuleState.RUNNING);
            plugin.getLogger().info("Module '" + module.getName() + "' enabled.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable module '" + module.getName() + "': " + e.getMessage());
            e.printStackTrace();
            module.setState(ModuleState.DISABLED);
            return false;
        }
    }

    public boolean disableModule(String name) {
        BaseModule module = getModule(name);
        if (module == null) {
            return false;
        }

        if (module.getState() == ModuleState.DISABLED) {
            return true;
        }

        for (BaseModule other : modules.values()) {
            if (other.getState() == ModuleState.RUNNING && other.getDependencies().contains(module.getName())) {
                disableModule(other.getName());
            }
        }

        module.unregisterAll();
        module.onDisable();
        module.setState(ModuleState.DISABLED);
        plugin.getLogger().info("Module '" + module.getName() + "' disabled.");
        return true;
    }

    public boolean reloadModule(String name) {
        BaseModule module = getModule(name);
        if (module == null) {
            return false;
        }

        if (module.getState() == ModuleState.RUNNING) {
            module.onReload();
            plugin.getLogger().info("Module '" + module.getName() + "' reloaded.");
            return true;
        }

        return enableModule(name);
    }

    public void reloadAllModules() {
        for (BaseModule module : new ArrayList<>(modules.values())) {
            reloadModule(module.getName());
        }
    }

    private boolean isEnabledInConfig(BaseModule module) {
        String key = module.getName().toLowerCase();
        return userEnabledConfig.getOrDefault(key, module.isEnabledByDefault());
    }

    private List<BaseModule> topologicalSort() {
        List<BaseModule> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();

        for (BaseModule module : modules.values()) {
            if (!visited.contains(module.getName().toLowerCase())) {
                visit(module, visited, stack, result);
            }
        }

        return result;
    }

    private void visit(BaseModule module, Set<String> visited, Set<String> stack, List<BaseModule> result) {
        String name = module.getName().toLowerCase();
        if (stack.contains(name)) {
            throw new IllegalStateException("Cyclic dependency detected involving module '" + module.getName() + "'");
        }
        if (visited.contains(name)) {
            return;
        }

        stack.add(name);
        for (String dependency : module.getDependencies()) {
            BaseModule depModule = getModule(dependency);
            if (depModule != null) {
                visit(depModule, visited, stack, result);
            }
        }
        stack.remove(name);
        visited.add(name);
        result.add(module);
    }
}
