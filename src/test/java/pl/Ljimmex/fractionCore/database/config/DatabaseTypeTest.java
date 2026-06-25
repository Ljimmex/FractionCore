package pl.Ljimmex.fractionCore.database.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class DatabaseTypeTest {

    @Test
    void valuesAreInDeclarationOrder() {
        assertArrayEquals(
                new DatabaseType[]{DatabaseType.SQLITE, DatabaseType.MYSQL, DatabaseType.POSTGRESQL},
                DatabaseType.values()
        );
    }
}
