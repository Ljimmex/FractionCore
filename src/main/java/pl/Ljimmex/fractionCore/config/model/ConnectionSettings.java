package pl.Ljimmex.fractionCore.config.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class ConnectionSettings {

    private String host = "localhost";
    private int port = 3306;
    private String database = "fractioncore";
    private String username = "root";
    private String password = "";

    public ConnectionSettings() {
    }

    public ConnectionSettings(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
