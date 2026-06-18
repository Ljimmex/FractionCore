package pl.Ljimmex.fractionCore.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageParser {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private MessageParser() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        if (containsMiniMessage(input)) {
            return MINI_MESSAGE.deserialize(input);
        }

        return LEGACY_SERIALIZER.deserialize(input);
    }

    private static boolean containsMiniMessage(String input) {
        if (!input.contains("<") || !input.contains(">")) {
            return false;
        }

        int openIndex = input.indexOf('<');
        while (openIndex != -1) {
            int closeIndex = input.indexOf('>', openIndex);
            if (closeIndex == -1) {
                return false;
            }
            String tag = input.substring(openIndex + 1, closeIndex);
            if (!tag.isEmpty() && isValidMiniMessageTag(tag)) {
                return true;
            }
            openIndex = input.indexOf('<', closeIndex);
        }
        return false;
    }

    private static boolean isValidMiniMessageTag(String tag) {
        String normalized = tag.toLowerCase();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return false;
        }

        return normalized.matches("[a-z0-9_]+(:[^>]+)?")
                || normalized.startsWith("gradient:")
                || normalized.startsWith("click:")
                || normalized.startsWith("hover:")
                || normalized.startsWith("translatable:")
                || normalized.startsWith("keybind:")
                || normalized.startsWith("insertion:")
                || normalized.startsWith("font:");
    }
}
