package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildDisbandHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuildDisbandHistoryDaoImpl implements GuildDisbandHistoryDao {

    private final DatabaseManager databaseManager;

    public GuildDisbandHistoryDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(GuildDisbandHistory history) throws SQLException {
        String sql = "INSERT INTO guild_disband_history (original_guild_id, name, tag, color, leader_uuid, points, level, created_at, disbanded_at, disbanded_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, history.getOriginalGuildId().toString());
            statement.setString(2, history.getName());
            statement.setString(3, history.getTag());
            statement.setString(4, history.getColor());
            statement.setString(5, history.getLeaderUuid().toString());
            statement.setInt(6, history.getPoints());
            statement.setInt(7, history.getLevel());
            statement.setLong(8, history.getCreatedAt());
            statement.setLong(9, history.getDisbandedAt());
            statement.setString(10, history.getDisbandedBy().toString());
            statement.executeUpdate();
        }
    }

    @Override
    public List<GuildDisbandHistory> findAll() throws SQLException {
        List<GuildDisbandHistory> histories = new ArrayList<>();
        String sql = "SELECT * FROM guild_disband_history ORDER BY disbanded_at DESC";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                histories.add(mapResultSet(resultSet));
            }
        }
        return histories;
    }

    private GuildDisbandHistory mapResultSet(ResultSet resultSet) throws SQLException {
        return new GuildDisbandHistory(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("original_guild_id")),
                resultSet.getString("name"),
                resultSet.getString("tag"),
                resultSet.getString("color"),
                UUID.fromString(resultSet.getString("leader_uuid")),
                resultSet.getInt("points"),
                resultSet.getInt("level"),
                resultSet.getLong("created_at"),
                resultSet.getLong("disbanded_at"),
                UUID.fromString(resultSet.getString("disbanded_by"))
        );
    }
}
