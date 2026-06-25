package pl.Ljimmex.fractionCore.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class ModuleEntry {

    private boolean enabled = true;

    public ModuleEntry() {
    }

    public ModuleEntry(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
