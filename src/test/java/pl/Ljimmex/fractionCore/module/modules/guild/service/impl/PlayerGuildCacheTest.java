package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import org.junit.jupiter.api.Test;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerGuildCacheTest {

    private final PlayerGuildCache cache = new PlayerGuildCache();

    @Test
    void refreshPutsEntryWhenPlayerHasGuild() {
        UUID playerUuid = UUID.randomUUID();
        UUID guildId = UUID.randomUUID();
        PlayerData data = mock(PlayerData.class);
        when(data.getGuildId()).thenReturn(guildId);
        when(data.getRank()).thenReturn(GuildRank.MODERATOR);

        cache.refresh(playerUuid, data);

        var entry = cache.get(playerUuid);
        assertTrue(entry.isPresent());
        assertEquals(guildId, entry.get().guildId());
        assertEquals(GuildRank.MODERATOR, entry.get().rank());
    }

    @Test
    void refreshRemovesEntryWhenDataIsNull() {
        UUID playerUuid = UUID.randomUUID();
        cache.refresh(playerUuid, playerData(UUID.randomUUID(), GuildRank.MEMBER));

        cache.refresh(playerUuid, null);

        assertTrue(cache.get(playerUuid).isEmpty());
    }

    @Test
    void refreshRemovesEntryWhenGuildIdIsNull() {
        UUID playerUuid = UUID.randomUUID();
        cache.refresh(playerUuid, playerData(UUID.randomUUID(), GuildRank.MEMBER));

        PlayerData data = mock(PlayerData.class);
        when(data.getGuildId()).thenReturn(null);

        cache.refresh(playerUuid, data);

        assertTrue(cache.get(playerUuid).isEmpty());
    }

    @Test
    void removeRemovesEntry() {
        UUID playerUuid = UUID.randomUUID();
        cache.refresh(playerUuid, playerData(UUID.randomUUID(), GuildRank.LEADER));

        cache.remove(playerUuid);

        assertTrue(cache.get(playerUuid).isEmpty());
    }

    @Test
    void getReturnsEmptyWhenPlayerMissing() {
        assertTrue(cache.get(UUID.randomUUID()).isEmpty());
    }

    @Test
    void clearEmptiesAllEntries() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        cache.refresh(uuid1, playerData(UUID.randomUUID(), GuildRank.RECRUIT));
        cache.refresh(uuid2, playerData(UUID.randomUUID(), GuildRank.CO_LEADER));

        cache.clear();

        assertTrue(cache.get(uuid1).isEmpty());
        assertTrue(cache.get(uuid2).isEmpty());
    }

    private PlayerData playerData(UUID guildId, GuildRank rank) {
        PlayerData data = new PlayerData();
        data.setGuildId(guildId);
        data.setRank(rank);
        return data;
    }
}
