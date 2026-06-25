package pl.Ljimmex.fractionCore.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class SQLiteSettings {

    @Comment("Path to the SQLite database file, relative to the plugin data folder")
    private String file = "data/database.db";

    public SQLiteSettings() {
    }

    public String getFile() {
        return file;
    }
}
