package pl.Ljimmex.fractionCore.lang;

import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderContext {

    private Player player;
    private Guild guild;
    private final Map<String, Object> placeholders = new HashMap<>();

    public PlaceholderContext() {
    }

    public static PlaceholderContext empty() {
        return new PlaceholderContext();
    }

    public static PlaceholderContext of(Player player) {
        PlaceholderContext context = new PlaceholderContext();
        context.player = player;
        return context;
    }

    public static PlaceholderContext of(Player player, Guild guild) {
        PlaceholderContext context = new PlaceholderContext();
        context.player = player;
        context.guild = guild;
        return context;
    }

    public PlaceholderContext with(String key, Object value) {
        this.placeholders.put(key, value);
        return this;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public Map<String, Object> getPlaceholders() {
        return placeholders;
    }
}
