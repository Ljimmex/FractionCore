package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.Guild;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuildDao {

    void save(Guild guild) throws SQLException;

    void update(Guild guild) throws SQLException;

    void delete(UUID id) throws SQLException;

    Optional<Guild> findById(UUID id) throws SQLException;

    Optional<Guild> findByName(String name) throws SQLException;

    Optional<Guild> findByTag(String tag) throws SQLException;

    List<Guild> findAll() throws SQLException;

    boolean existsByName(String name) throws SQLException;

    boolean existsByTag(String tag) throws SQLException;
}
