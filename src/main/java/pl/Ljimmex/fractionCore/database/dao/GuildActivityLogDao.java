package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.GuildActivityLog;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface GuildActivityLogDao {

    void save(GuildActivityLog log) throws SQLException;

    List<GuildActivityLog> findByGuild(UUID guildId) throws SQLException;

    List<GuildActivityLog> findByGuild(UUID guildId, int limit) throws SQLException;
}
