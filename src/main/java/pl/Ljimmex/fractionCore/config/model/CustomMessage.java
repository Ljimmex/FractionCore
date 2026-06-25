package pl.Ljimmex.fractionCore.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class CustomMessage {

    private boolean enabled = true;
    private String format = "";

    public CustomMessage() {
    }

    public CustomMessage(boolean enabled, String format) {
        this.enabled = enabled;
        this.format = format;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getFormat() {
        return format;
    }
}
