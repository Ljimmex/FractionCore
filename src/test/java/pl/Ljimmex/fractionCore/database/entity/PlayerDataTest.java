package pl.Ljimmex.fractionCore.database.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDataTest {

    @Test
    void gettersAndSettersWork() {
        UUID uuid = UUID.randomUUID();
        UUID guildId = UUID.randomUUID();

        PlayerData data = new PlayerData();
        data.setUuid(uuid);
        data.setName("Steve");
        data.setGuildId(guildId);
        data.setRank(GuildRank.LEADER);
        data.setKills(5);
        data.setDeaths(2);
        data.setAssists(3);
        data.setPoints(100);
        data.setJoinedGuildAt(1_234_567_890L);
        data.setLeftGuildAt(1_234_567_899L);

        assertEquals(uuid, data.getUuid());
        assertEquals("Steve", data.getName());
        assertEquals(guildId, data.getGuildId());
        assertEquals(GuildRank.LEADER, data.getRank());
        assertEquals(5, data.getKills());
        assertEquals(2, data.getDeaths());
        assertEquals(3, data.getAssists());
        assertEquals(100, data.getPoints());
        assertEquals(1_234_567_890L, data.getJoinedGuildAt());
        assertEquals(1_234_567_899L, data.getLeftGuildAt());
    }

    @Test
    void allArgsConstructorInitializesFields() {
        UUID uuid = UUID.randomUUID();
        UUID guildId = UUID.randomUUID();

        PlayerData data = new PlayerData(uuid, "Alex", guildId, GuildRank.MODERATOR,
                1, 0, 0, 50, 1000L, 2000L);

        assertEquals(uuid, data.getUuid());
        assertEquals("Alex", data.getName());
        assertEquals(guildId, data.getGuildId());
        assertEquals(GuildRank.MODERATOR, data.getRank());
        assertEquals(1, data.getKills());
        assertEquals(0, data.getDeaths());
        assertEquals(0, data.getAssists());
        assertEquals(50, data.getPoints());
        assertEquals(1000L, data.getJoinedGuildAt());
        assertEquals(2000L, data.getLeftGuildAt());
    }
}
