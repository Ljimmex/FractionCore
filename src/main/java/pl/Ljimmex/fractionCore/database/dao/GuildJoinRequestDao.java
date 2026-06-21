package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.GuildJoinRequest;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuildJoinRequestDao {

    void save(GuildJoinRequest request) throws SQLException;

    void delete(UUID guildId, UUID playerUuid) throws SQLException;

    void deleteByGuild(UUID guildId) throws SQLException;

    Optional<GuildJoinRequest> find(UUID guildId, UUID playerUuid) throws SQLException;

    List<GuildJoinRequest> findByGuild(UUID guildId) throws SQLException;

    boolean exists(UUID guildId, UUID playerUuid) throws SQLException;
}
