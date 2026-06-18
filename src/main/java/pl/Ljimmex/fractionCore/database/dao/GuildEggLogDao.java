package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.GuildEggLog;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface GuildEggLogDao {

    void save(GuildEggLog log) throws SQLException;

    List<GuildEggLog> findByGuild(UUID guildId) throws SQLException;

    List<GuildEggLog> findByGuild(UUID guildId, int limit) throws SQLException;

    void deleteOlderThan(long timestamp) throws SQLException;
}
