package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildAllyRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuildAllyRequestDaoImpl implements GuildAllyRequestDao {

    private final DatabaseManager databaseManager;

    public GuildAllyRequestDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(GuildAllyRequest request) throws SQLException {
        String sql = "INSERT INTO guild_ally_requests (guild_id, target_guild_id, requested_at) VALUES (?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.getGuildId().toString());
            statement.setString(2, request.getTargetGuildId().toString());
            statement.setLong(3, request.getRequestedAt());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID guildId, UUID targetGuildId) throws SQLException {
        String sql = "DELETE FROM guild_ally_requests WHERE guild_id = ? AND target_guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, targetGuildId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteByGuild(UUID guildId) throws SQLException {
        String sql = "DELETE FROM guild_ally_requests WHERE guild_id = ? OR target_guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, guildId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<GuildAllyRequest> find(UUID guildId, UUID targetGuildId) throws SQLException {
        String sql = "SELECT * FROM guild_ally_requests WHERE guild_id = ? AND target_guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, targetGuildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<GuildAllyRequest> findByTargetGuild(UUID targetGuildId) throws SQLException {
        List<GuildAllyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM guild_ally_requests WHERE target_guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetGuildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapResultSet(resultSet));
                }
            }
        }
        return requests;
    }

    @Override
    public List<GuildAllyRequest> findByGuild(UUID guildId) throws SQLException {
        List<GuildAllyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM guild_ally_requests WHERE guild_id = ?";
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
    public boolean exists(UUID guildId, UUID targetGuildId) throws SQLException {
        return find(guildId, targetGuildId).isPresent();
    }

    private GuildAllyRequest mapResultSet(ResultSet resultSet) throws SQLException {
        return new GuildAllyRequest(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("guild_id")),
                UUID.fromString(resultSet.getString("target_guild_id")),
                resultSet.getLong("requested_at")
        );
    }
}
