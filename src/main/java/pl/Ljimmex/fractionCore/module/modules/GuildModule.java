package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.module.BaseModule;
import pl.Ljimmex.fractionCore.module.modules.guild.listener.GuildChatListener;
import pl.Ljimmex.fractionCore.module.modules.guild.listener.GuildConnectionListener;
import pl.Ljimmex.fractionCore.module.modules.guild.listener.GuildHomeListener;
import pl.Ljimmex.fractionCore.module.modules.guild.listener.GuildJoinListener;
import pl.Ljimmex.fractionCore.module.modules.guild.listener.GuildQuitListener;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildService;

import java.util.Arrays;
import java.util.List;

public class GuildModule extends BaseModule {

    private GuildService guildService;

    public GuildModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "guild";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("database", "lang");
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public GuildService getGuildService() {
        return guildService;
    }

    public void setGuildService(GuildService guildService) {
        this.guildService = guildService;
        registerListener(new GuildJoinListener(guildService.getTagManager()));
        registerListener(new GuildQuitListener(guildService.getTagManager()));
        registerListener(new GuildHomeListener(guildService));
        registerListener(new GuildChatListener(getPlugin(), guildService.getPlayerDao(), guildService.getGuildDao(),
                guildService.getGuildConfig(), guildService.getRelationManager(), guildService.getContext().getPlayerGuildCache()));
        registerListener(new GuildConnectionListener(guildService));
    }
}
