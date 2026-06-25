package pl.Ljimmex.fractionCore.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class GeneralSettings {

    @Comment("Default language code")
    private String language = "pl_PL";

    @Comment("Enables debug logging")
    private boolean debug = false;

    @Comment("Plugin prefix used in messages")
    private String prefix = "<dark_gray>[<aqua>FGC<dark_gray>] <gray>";

    @Setting("custom-join-message")
    @Comment("Custom join message settings")
    private CustomMessage customJoinMessage = new CustomMessage(
            true,
            "<dark_gray>[<green><bold>+</bold></green>] <gray>{player}"
    );

    @Setting("custom-quit-message")
    @Comment("Custom quit message settings")
    private CustomMessage customQuitMessage = new CustomMessage(
            true,
            "<dark_gray>[<red><bold>-</bold></red>] <gray>{player}"
    );

    public GeneralSettings() {
    }

    public String getLanguage() {
        return language;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getPrefix() {
        return prefix;
    }

    public CustomMessage getCustomJoinMessage() {
        return customJoinMessage;
    }

    public CustomMessage getCustomQuitMessage() {
        return customQuitMessage;
    }
}
