package pl.Ljimmex.fractionCore.cuboid.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CuboidFlagValueTest {

    @Test
    void valuesAreInDeclarationOrder() {
        assertEquals(5, CuboidFlagValue.values().length);
        assertEquals(CuboidFlagValue.ALLOW, CuboidFlagValue.valueOf("ALLOW"));
        assertEquals(CuboidFlagValue.DENY, CuboidFlagValue.valueOf("DENY"));
        assertEquals(CuboidFlagValue.MEMBERS, CuboidFlagValue.valueOf("MEMBERS"));
        assertEquals(CuboidFlagValue.ALLIES, CuboidFlagValue.valueOf("ALLIES"));
        assertEquals(CuboidFlagValue.LEADER, CuboidFlagValue.valueOf("LEADER"));
    }
}
