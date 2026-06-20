package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.GuildInvite;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuildInviteDao {

    void save(GuildInvite invite) throws SQLException;

    void delete(UUID guildId, UUID playerUuid) throws SQLException;

    void deleteByGuild(UUID guildId) throws SQLException;

    Optional<GuildInvite> find(UUID guildId, UUID playerUuid) throws SQLException;

    List<GuildInvite> findByGuild(UUID guildId) throws SQLException;

    List<GuildInvite> findByPlayer(UUID playerUuid) throws SQLException;

    int countByGuild(UUID guildId) throws SQLException;
}
