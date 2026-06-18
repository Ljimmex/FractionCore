package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildEggLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuildEggLogDaoImpl implements GuildEggLogDao {

    private final DatabaseManager databaseManager;

    public GuildEggLogDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(GuildEggLog log) throws SQLException {
        String sql = "INSERT INTO guild_egg_logs (guild_id, player_uuid, event_type, old_state, new_state, metadata, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, log.getGuildId().toString());
            statement.setString(2, log.getPlayerUuid() != null ? log.getPlayerUuid().toString() : null);
            statement.setString(3, log.getEventType());
            statement.setObject(4, log.getOldState());
            statement.setObject(5, log.getNewState());
            statement.setString(6, log.getMetadata());
            statement.setLong(7, log.getTimestamp());
            statement.executeUpdate();
        }
    }

    @Override
    public List<GuildEggLog> findByGuild(UUID guildId) throws SQLException {
        return findByGuild(guildId, 100);
    }

    @Override
    public List<GuildEggLog> findByGuild(UUID guildId, int limit) throws SQLException {
        List<GuildEggLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM guild_egg_logs WHERE guild_id = ? ORDER BY timestamp DESC LIMIT ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapResultSet(resultSet));
                }
            }
        }
        return logs;
    }

    @Override
    public void deleteOlderThan(long timestamp) throws SQLException {
        String sql = "DELETE FROM guild_egg_logs WHERE timestamp < ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, timestamp);
            statement.executeUpdate();
        }
    }

    private GuildEggLog mapResultSet(ResultSet resultSet) throws SQLException {
        String playerUuid = resultSet.getString("player_uuid");
        Object oldState = resultSet.getObject("old_state");
        Object newState = resultSet.getObject("new_state");
        return new GuildEggLog(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("guild_id")),
                playerUuid != null ? UUID.fromString(playerUuid) : null,
                resultSet.getString("event_type"),
                oldState != null ? ((Number) oldState).intValue() : null,
                newState != null ? ((Number) newState).intValue() : null,
                resultSet.getString("metadata"),
                resultSet.getLong("timestamp")
        );
    }
}
