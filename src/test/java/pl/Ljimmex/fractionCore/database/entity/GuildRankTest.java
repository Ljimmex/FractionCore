package pl.Ljimmex.fractionCore.database.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuildRankTest {

    @Test
    void expectedRanksArePresent() {
        assertEquals(5, GuildRank.values().length);
        assertEquals(GuildRank.LEADER, GuildRank.valueOf("LEADER"));
        assertEquals(GuildRank.CO_LEADER, GuildRank.valueOf("CO_LEADER"));
        assertEquals(GuildRank.MODERATOR, GuildRank.valueOf("MODERATOR"));
        assertEquals(GuildRank.MEMBER, GuildRank.valueOf("MEMBER"));
        assertEquals(GuildRank.RECRUIT, GuildRank.valueOf("RECRUIT"));
    }

    @Test
    void displayNamesMatchPolishLabels() {
        assertEquals("Lider", GuildRank.LEADER.getDisplayName());
        assertEquals("Co-Lider", GuildRank.CO_LEADER.getDisplayName());
        assertEquals("Moderator", GuildRank.MODERATOR.getDisplayName());
        assertEquals("Członek", GuildRank.MEMBER.getDisplayName());
        assertEquals("Rekrut", GuildRank.RECRUIT.getDisplayName());
    }
}
