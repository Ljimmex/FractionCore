package pl.Ljimmex.fractionCore.module.modules.guild.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildService;

/**
 * Keeps the in-memory player guild cache warm by loading data when a player
 * joins and evicting it when they quit.
 */
public class GuildConnectionListener implements Listener {

    private final GuildService guildService;

    public GuildConnectionListener(GuildService guildService) {
        this.guildService = guildService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        guildService.refreshPlayerCache(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        guildService.invalidatePlayerCache(event.getPlayer().getUniqueId());
    }
}
