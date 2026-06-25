package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache mapping online players to their guild affiliation and rank.
 * <p>
 * Cuboid checks call this cache instead of hitting the database on every
 * block break/place/interact. Entries are populated lazily and refreshed
 * when {@link #refresh(UUID)} is invoked (e.g. on login/logout/guild changes).
 */
public final class PlayerGuildCache {

    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    public void refresh(UUID playerUuid, PlayerData data) {
        if (data == null || data.getGuildId() == null) {
            cache.remove(playerUuid);
        } else {
            cache.put(playerUuid, new Entry(data.getGuildId(), data.getRank()));
        }
    }

    public void remove(UUID playerUuid) {
        cache.remove(playerUuid);
    }

    public Optional<Entry> get(UUID playerUuid) {
        return Optional.ofNullable(cache.get(playerUuid));
    }

    public void clear() {
        cache.clear();
    }

    public record Entry(UUID guildId, GuildRank rank) {
    }
}
