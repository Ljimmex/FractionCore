package pl.Ljimmex.fractionCore.lang;

import org.bukkit.entity.Player;
import pl.Ljimmex.fractionCore.database.entity.Guild;

import java.util.Map;

public final class PlaceholderResolver {

    private static final String NOT_AVAILABLE = "N/A";

    private PlaceholderResolver() {
    }

    public static String resolve(String message, PlaceholderContext context) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String result = message;

        // Custom placeholders first so command-provided values override defaults.
        for (Map.Entry<String, Object> entry : context.getPlaceholders().entrySet()) {
            result = replace(result, entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : NOT_AVAILABLE);
        }

        Player player = context.getPlayer();
        Guild guild = context.getGuild();

        result = replace(result, "player", player != null ? player.getName() : NOT_AVAILABLE);
        result = replace(result, "uuid", player != null ? player.getUniqueId().toString() : NOT_AVAILABLE);
        result = replace(result, "guild", guild != null ? guild.getName() : NOT_AVAILABLE);
        result = replace(result, "tag", guild != null ? guild.getTag() : NOT_AVAILABLE);
        result = replace(result, "tag_color", guild != null ? "[" + guild.getTag() + "]" : NOT_AVAILABLE);
        result = replace(result, "points", guild != null ? String.valueOf(guild.getPoints()) : NOT_AVAILABLE);
        result = replace(result, "level", guild != null ? String.valueOf(guild.getLevel()) : NOT_AVAILABLE);
        result = replace(result, "members", guild != null ? String.valueOf(guild.getPoints()) : NOT_AVAILABLE);

        return result;
    }

    private static String replace(String message, String key, String value) {
        return message.replace("{" + key + "}", value != null ? value : NOT_AVAILABLE);
    }
}
