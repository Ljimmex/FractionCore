package pl.Ljimmex.fractionCore.module.modules.guild.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildTagManager;

public class GuildQuitListener implements Listener {

    private final GuildTagManager tagManager;

    public GuildQuitListener(GuildTagManager tagManager) {
        this.tagManager = tagManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tagManager.clearPlayerTag(event.getPlayer());
        tagManager.updateAllTags();
    }
}
