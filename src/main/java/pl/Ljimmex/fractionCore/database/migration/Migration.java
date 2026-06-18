package pl.Ljimmex.fractionCore.database.migration;

import pl.Ljimmex.fractionCore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;

public class Migration {

    private final int version;
    private final String description;
    private final MigrationAction action;

    public Migration(int version, String description, MigrationAction action) {
        this.version = version;
        this.description = description;
        this.action = action;
    }

    public int getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public void apply(DatabaseManager databaseManager) throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            action.run(connection);
        }
    }

    @FunctionalInterface
    public interface MigrationAction {
        void run(Connection connection) throws SQLException;
    }
}
