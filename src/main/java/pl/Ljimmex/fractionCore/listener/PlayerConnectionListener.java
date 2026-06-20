package pl.Ljimmex.fractionCore.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.lang.MessageParser;

public class PlayerConnectionListener implements Listener {

    private final JavaPlugin plugin;

    public PlayerConnectionListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("general.custom-join-message.enabled", true)) {
            return;
        }
        String format = config.getString("general.custom-join-message.format",
                "<dark_gray>[<green><bold>+</bold></green>] <gray>{player}");
        event.joinMessage(formatMessage(format, event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("general.custom-quit-message.enabled", true)) {
            return;
        }
        String format = config.getString("general.custom-quit-message.format",
                "<dark_gray>[<red><bold>-</bold></red>] <gray>{player}");
        event.quitMessage(formatMessage(format, event.getPlayer()));
    }

    private Component formatMessage(String format, Player player) {
        String message = format.replace("{player}", player.getName());
        return MessageParser.parse(message);
    }
}
