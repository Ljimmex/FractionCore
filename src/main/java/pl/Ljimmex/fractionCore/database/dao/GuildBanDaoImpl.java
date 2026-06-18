package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildBan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuildBanDaoImpl implements GuildBanDao {

    private final DatabaseManager databaseManager;

    public GuildBanDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(GuildBan ban) throws SQLException {
        String sql = "INSERT INTO guild_bans (guild_id, player_uuid, reason, banned_by, banned_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ban.getGuildId().toString());
            statement.setString(2, ban.getPlayerUuid().toString());
            statement.setString(3, ban.getReason());
            statement.setString(4, ban.getBannedBy().toString());
            statement.setLong(5, ban.getBannedAt());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID guildId, UUID playerUuid) throws SQLException {
        String sql = "DELETE FROM guild_bans WHERE guild_id = ? AND player_uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, playerUuid.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<GuildBan> find(UUID guildId, UUID playerUuid) throws SQLException {
        String sql = "SELECT * FROM guild_bans WHERE guild_id = ? AND player_uuid = ?";
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
    public List<GuildBan> findByGuild(UUID guildId) throws SQLException {
        List<GuildBan> bans = new ArrayList<>();
        String sql = "SELECT * FROM guild_bans WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bans.add(mapResultSet(resultSet));
                }
            }
        }
        return bans;
    }

    @Override
    public boolean exists(UUID guildId, UUID playerUuid) throws SQLException {
        String sql = "SELECT 1 FROM guild_bans WHERE guild_id = ? AND player_uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private GuildBan mapResultSet(ResultSet resultSet) throws SQLException {
        return new GuildBan(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("guild_id")),
                UUID.fromString(resultSet.getString("player_uuid")),
                resultSet.getString("reason"),
                UUID.fromString(resultSet.getString("banned_by")),
                resultSet.getLong("banned_at")
        );
    }
}
