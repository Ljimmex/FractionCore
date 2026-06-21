package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.GuildAllyRequest;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuildAllyRequestDao {

    void save(GuildAllyRequest request) throws SQLException;

    void delete(UUID guildId, UUID targetGuildId) throws SQLException;

    void deleteByGuild(UUID guildId) throws SQLException;

    Optional<GuildAllyRequest> find(UUID guildId, UUID targetGuildId) throws SQLException;

    List<GuildAllyRequest> findByTargetGuild(UUID targetGuildId) throws SQLException;

    List<GuildAllyRequest> findByGuild(UUID guildId) throws SQLException;

    boolean exists(UUID guildId, UUID targetGuildId) throws SQLException;
}
