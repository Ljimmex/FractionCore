package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.GuildFlag;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuildFlagDao {

    void save(GuildFlag flag) throws SQLException;

    void update(GuildFlag flag) throws SQLException;

    void delete(UUID guildId, String flagName) throws SQLException;

    void deleteByGuild(UUID guildId) throws SQLException;

    Optional<GuildFlag> find(UUID guildId, String flagName) throws SQLException;

    List<GuildFlag> findByGuild(UUID guildId) throws SQLException;
}
