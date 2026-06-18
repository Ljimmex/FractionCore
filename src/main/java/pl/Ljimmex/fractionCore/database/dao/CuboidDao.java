package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.CuboidData;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuboidDao {

    void save(CuboidData cuboid) throws SQLException;

    void update(CuboidData cuboid) throws SQLException;

    void delete(UUID guildId) throws SQLException;

    Optional<CuboidData> findByGuildId(UUID guildId) throws SQLException;

    List<CuboidData> findAll() throws SQLException;
}
