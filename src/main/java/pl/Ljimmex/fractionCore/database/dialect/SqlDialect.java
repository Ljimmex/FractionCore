package pl.Ljimmex.fractionCore.database.dialect;

import pl.Ljimmex.fractionCore.database.config.DatabaseType;

public final class SqlDialect {

    private final DatabaseType type;

    public SqlDialect(DatabaseType type) {
        this.type = type;
    }

    public String autoIncrementPrimaryKey() {
        return switch (type) {
            case SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT";
            case MYSQL -> "INTEGER PRIMARY KEY AUTO_INCREMENT";
            case POSTGRESQL -> "SERIAL PRIMARY KEY";
        };
    }

    public String integerPrimaryKey() {
        return "INTEGER PRIMARY KEY";
    }

    public String booleanType() {
        return switch (type) {
            case SQLITE -> "INTEGER";
            case MYSQL -> "TINYINT(1)";
            case POSTGRESQL -> "BOOLEAN";
        };
    }

    public String textType() {
        return "TEXT";
    }

    public String longTextType() {
        return switch (type) {
            case SQLITE -> "TEXT";
            case MYSQL -> "LONGTEXT";
            case POSTGRESQL -> "TEXT";
        };
    }

    public String timestampType() {
        return "INTEGER";
    }

    public String insertOrReplace(String table, String columns) {
        return switch (type) {
            case SQLITE -> "INSERT OR REPLACE INTO " + table + " (" + columns + ") VALUES ";
            case MYSQL -> "REPLACE INTO " + table + " (" + columns + ") VALUES ";
            case POSTGRESQL -> "INSERT INTO " + table + " (" + columns + ") VALUES ";
        };
    }

    public String upsertConflict(String table, String conflictColumn, String updateColumns) {
        if (type == DatabaseType.POSTGRESQL) {
            return " ON CONFLICT (" + conflictColumn + ") DO UPDATE SET " + updateColumns;
        }
        return "";
    }
}
