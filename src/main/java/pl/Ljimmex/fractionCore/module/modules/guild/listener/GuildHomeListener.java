package pl.Ljimmex.fractionCore.module.modules.guild.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildService;

public class GuildHomeListener implements Listener {

    private final GuildService guildService;

    public GuildHomeListener(GuildService guildService) {
        this.guildService = guildService;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        guildService.cancelHomeTeleport(event.getPlayer());
    }
}
