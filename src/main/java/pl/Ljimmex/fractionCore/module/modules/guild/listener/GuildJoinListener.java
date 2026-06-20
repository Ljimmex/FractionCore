package pl.Ljimmex.fractionCore.module.modules.guild.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildTagManager;

public class GuildJoinListener implements Listener {

    private final GuildTagManager tagManager;

    public GuildJoinListener(GuildTagManager tagManager) {
        this.tagManager = tagManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Refresh tags for everyone so the new player is visible to guild mates
        // and sees tags of their own guild members.
        tagManager.updateAllTags();
    }
}
