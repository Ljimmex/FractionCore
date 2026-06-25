package pl.Ljimmex.fractionCore.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class LangSettings {

    @Setting("default")
    @Comment("Default language code")
    private String defaultLanguage = "pl_PL";

    public LangSettings() {
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }
}
