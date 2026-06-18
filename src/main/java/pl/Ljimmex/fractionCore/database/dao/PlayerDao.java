package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.PlayerData;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerDao {

    void save(PlayerData player) throws SQLException;

    void update(PlayerData player) throws SQLException;

    void delete(UUID uuid) throws SQLException;

    Optional<PlayerData> findByUuid(UUID uuid) throws SQLException;

    Optional<PlayerData> findByName(String name) throws SQLException;

    List<PlayerData> findByGuild(UUID guildId) throws SQLException;

    List<PlayerData> findAll() throws SQLException;
}
