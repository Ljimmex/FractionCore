package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildJoinRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuildJoinRequestDaoImpl implements GuildJoinRequestDao {

    private final DatabaseManager databaseManager;

    public GuildJoinRequestDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(GuildJoinRequest request) throws SQLException {
        String sql = "INSERT INTO guild_join_requests (guild_id, player_uuid, requested_at) VALUES (?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.getGuildId().toString());
            statement.setString(2, request.getPlayerUuid().toString());
            statement.setLong(3, request.getRequestedAt());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID guildId, UUID playerUuid) throws SQLException {
        String sql = "DELETE FROM guild_join_requests WHERE guild_id = ? AND player_uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, playerUuid.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteByGuild(UUID guildId) throws SQLException {
        String sql = "DELETE FROM guild_join_requests WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<GuildJoinRequest> find(UUID guildId, UUID playerUuid) throws SQLException {
        String sql = "SELECT * FROM guild_join_requests WHERE guild_id = ? AND player_uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<GuildJoinRequest> findByGuild(UUID guildId) throws SQLException {
        List<GuildJoinRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM guild_join_requests WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapResultSet(resultSet));
                }
            }
        }
        return requests;
    }

    @Override
    public boolean exists(UUID guildId, UUID playerUuid) throws SQLException {
        String sql = "SELECT 1 FROM guild_join_requests WHERE guild_id = ? AND player_uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private GuildJoinRequest mapResultSet(ResultSet resultSet) throws SQLException {
        return new GuildJoinRequest(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("guild_id")),
                UUID.fromString(resultSet.getString("player_uuid")),
                resultSet.getLong("requested_at")
        );
    }
}
