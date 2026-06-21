package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.GuildRelation;
import pl.Ljimmex.fractionCore.database.entity.RelationType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuildRelationDao {

    void save(GuildRelation relation) throws SQLException;

    void delete(UUID guild1Id, UUID guild2Id) throws SQLException;

    void deleteByGuild(UUID guildId) throws SQLException;

    Optional<GuildRelation> find(UUID guild1Id, UUID guild2Id) throws SQLException;

    List<GuildRelation> findByGuild(UUID guildId) throws SQLException;

    boolean exists(UUID guild1Id, UUID guild2Id) throws SQLException;

    void setType(UUID guild1Id, UUID guild2Id, RelationType type) throws SQLException;
}
