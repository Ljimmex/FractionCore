package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.GuildBan;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuildBanDao {

    void save(GuildBan ban) throws SQLException;

    void delete(UUID guildId, UUID playerUuid) throws SQLException;

    Optional<GuildBan> find(UUID guildId, UUID playerUuid) throws SQLException;

    List<GuildBan> findByGuild(UUID guildId) throws SQLException;

    boolean exists(UUID guildId, UUID playerUuid) throws SQLException;
}
