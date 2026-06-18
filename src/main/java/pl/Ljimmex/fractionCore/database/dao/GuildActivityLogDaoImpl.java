package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuildActivityLogDaoImpl implements GuildActivityLogDao {

    private final DatabaseManager databaseManager;

    public GuildActivityLogDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(GuildActivityLog log) throws SQLException {
        String sql = "INSERT INTO guild_activity_log (guild_id, actor_uuid, action, details, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, log.getGuildId().toString());
            statement.setString(2, log.getActorUuid() != null ? log.getActorUuid().toString() : null);
            statement.setString(3, log.getAction());
            statement.setString(4, log.getDetails());
            statement.setLong(5, log.getTimestamp());
            statement.executeUpdate();
        }
    }

    @Override
    public List<GuildActivityLog> findByGuild(UUID guildId) throws SQLException {
        return findByGuild(guildId, 100);
    }

    @Override
    public List<GuildActivityLog> findByGuild(UUID guildId, int limit) throws SQLException {
        List<GuildActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM guild_activity_log WHERE guild_id = ? ORDER BY timestamp DESC LIMIT ?";
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

    private GuildActivityLog mapResultSet(ResultSet resultSet) throws SQLException {
        String actorUuid = resultSet.getString("actor_uuid");
        return new GuildActivityLog(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("guild_id")),
                actorUuid != null ? UUID.fromString(actorUuid) : null,
                resultSet.getString("action"),
                resultSet.getString("details"),
                resultSet.getLong("timestamp")
        );
    }
}
