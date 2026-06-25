package pl.Ljimmex.fractionCore.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.Ljimmex.fractionCore.config.ConfigManager;
import pl.Ljimmex.fractionCore.config.model.PluginConfig;
import pl.Ljimmex.fractionCore.lang.MessageParser;

public class PlayerConnectionListener implements Listener {

    private final ConfigManager configManager;

    public PlayerConnectionListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PluginConfig config = configManager.getPluginConfig();
        if (!config.getGeneral().getCustomJoinMessage().isEnabled()) {
            return;
        }
        String format = config.getGeneral().getCustomJoinMessage().getFormat();
        event.joinMessage(formatMessage(format, event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PluginConfig config = configManager.getPluginConfig();
        if (!config.getGeneral().getCustomQuitMessage().isEnabled()) {
            return;
        }
        String format = config.getGeneral().getCustomQuitMessage().getFormat();
        event.quitMessage(formatMessage(format, event.getPlayer()));
    }

    private Component formatMessage(String format, Player player) {
        String message = format.replace("{player}", player.getName());
        return MessageParser.parse(message);
    }
}
