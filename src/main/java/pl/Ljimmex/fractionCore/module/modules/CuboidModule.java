package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;
import pl.Ljimmex.fractionCore.module.modules.guild.listener.CuboidProtectionListener;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildService;

import java.util.Arrays;
import java.util.List;

public class CuboidModule extends BaseModule {

    public CuboidModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "cuboid";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("guild");
    }

    @Override
    public void onEnable() {
        // Listener is registered after GuildService is created in FractionCore.
    }

    public void registerProtectionListener(GuildService guildService) {
        if (guildService == null || guildService.getCuboidManager() == null) {
            getPlugin().getLogger().warning("[CuboidModule] Guild service is not available, cuboid protection will not be active.");
            return;
        }
        registerListener(new CuboidProtectionListener(guildService.getCuboidManager(), guildService.getLangManager()));
        getPlugin().getLogger().info("[CuboidModule] Cuboid protection listener registered.");
    }

    @Override
    public void onDisable() {
        unregisterAll();
    }
}
