package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildInvite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuildInviteDaoImpl implements GuildInviteDao {

    private final DatabaseManager databaseManager;

    public GuildInviteDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(GuildInvite invite) throws SQLException {
        String sql = "INSERT INTO guild_invites (guild_id, player_uuid, invited_by, invited_at, expires_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, invite.getGuildId().toString());
            statement.setString(2, invite.getPlayerUuid().toString());
            statement.setString(3, invite.getInvitedBy().toString());
            statement.setLong(4, invite.getInvitedAt());
            statement.setLong(5, invite.getExpiresAt());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID guildId, UUID playerUuid) throws SQLException {
        String sql = "DELETE FROM guild_invites WHERE guild_id = ? AND player_uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, playerUuid.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteByGuild(UUID guildId) throws SQLException {
        String sql = "DELETE FROM guild_invites WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<GuildInvite> find(UUID guildId, UUID playerUuid) throws SQLException {
        String sql = "SELECT * FROM guild_invites WHERE guild_id = ? AND player_uuid = ?";
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
    public List<GuildInvite> findByGuild(UUID guildId) throws SQLException {
        List<GuildInvite> invites = new ArrayList<>();
        String sql = "SELECT * FROM guild_invites WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    invites.add(mapResultSet(resultSet));
                }
            }
        }
        return invites;
    }

    @Override
    public List<GuildInvite> findByPlayer(UUID playerUuid) throws SQLException {
        List<GuildInvite> invites = new ArrayList<>();
        String sql = "SELECT * FROM guild_invites WHERE player_uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    invites.add(mapResultSet(resultSet));
                }
            }
        }
        return invites;
    }

    @Override
    public int countByGuild(UUID guildId) throws SQLException {
        String sql = "SELECT COUNT(*) AS count FROM guild_invites WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("count");
                }
            }
        }
        return 0;
    }

    private GuildInvite mapResultSet(ResultSet resultSet) throws SQLException {
        return new GuildInvite(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("guild_id")),
                UUID.fromString(resultSet.getString("player_uuid")),
                UUID.fromString(resultSet.getString("invited_by")),
                resultSet.getLong("invited_at"),
                resultSet.getLong("expires_at")
        );
    }
}
