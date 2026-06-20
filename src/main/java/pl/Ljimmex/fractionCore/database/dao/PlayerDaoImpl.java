package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerDaoImpl implements PlayerDao {

    private final DatabaseManager databaseManager;

    public PlayerDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(PlayerData player) throws SQLException {
        String sql = "INSERT INTO players (uuid, name, guild_id, rank, kills, deaths, assists, points, joined_guild_at, left_guild_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getUuid().toString());
            statement.setString(2, player.getName());
            statement.setString(3, player.getGuildId() != null ? player.getGuildId().toString() : null);
            statement.setString(4, player.getRank() != null ? player.getRank().name() : null);
            statement.setInt(5, player.getKills());
            statement.setInt(6, player.getDeaths());
            statement.setInt(7, player.getAssists());
            statement.setInt(8, player.getPoints());
            statement.setLong(9, player.getJoinedGuildAt());
            statement.setLong(10, player.getLeftGuildAt());
            statement.executeUpdate();
        }
    }

    @Override
    public void update(PlayerData player) throws SQLException {
        String sql = "UPDATE players SET name = ?, guild_id = ?, rank = ?, kills = ?, deaths = ?, assists = ?, points = ?, joined_guild_at = ?, left_guild_at = ? WHERE uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getName());
            statement.setString(2, player.getGuildId() != null ? player.getGuildId().toString() : null);
            statement.setString(3, player.getRank() != null ? player.getRank().name() : null);
            statement.setInt(4, player.getKills());
            statement.setInt(5, player.getDeaths());
            statement.setInt(6, player.getAssists());
            statement.setInt(7, player.getPoints());
            statement.setLong(8, player.getJoinedGuildAt());
            statement.setLong(9, player.getLeftGuildAt());
            statement.setString(10, player.getUuid().toString());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID uuid) throws SQLException {
        String sql = "DELETE FROM players WHERE uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<PlayerData> findByUuid(UUID uuid) throws SQLException {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PlayerData> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM players WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<PlayerData> findByGuild(UUID guildId) throws SQLException {
        List<PlayerData> players = new ArrayList<>();
        String sql = "SELECT * FROM players WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    players.add(mapResultSet(resultSet));
                }
            }
        }
        return players;
    }

    @Override
    public List<PlayerData> findAll() throws SQLException {
        List<PlayerData> players = new ArrayList<>();
        String sql = "SELECT * FROM players";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                players.add(mapResultSet(resultSet));
            }
        }
        return players;
    }

    private PlayerData mapResultSet(ResultSet resultSet) throws SQLException {
        String guildIdStr = resultSet.getString("guild_id");
        String rankStr = resultSet.getString("rank");
        GuildRank rank = null;
        if (rankStr != null) {
            try {
                rank = GuildRank.valueOf(rankStr);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new PlayerData(
                UUID.fromString(resultSet.getString("uuid")),
                resultSet.getString("name"),
                guildIdStr != null ? UUID.fromString(guildIdStr) : null,
                rank,
                resultSet.getInt("kills"),
                resultSet.getInt("deaths"),
                resultSet.getInt("assists"),
                resultSet.getInt("points"),
                resultSet.getLong("joined_guild_at"),
                resultSet.getLong("left_guild_at")
        );
    }
}
