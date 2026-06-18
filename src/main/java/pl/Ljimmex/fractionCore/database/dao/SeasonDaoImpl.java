package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.Season;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SeasonDaoImpl implements SeasonDao {

    private final DatabaseManager databaseManager;

    public SeasonDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(Season season) throws SQLException {
        String sql = "INSERT INTO seasons (number, start_date, end_date, winner_guild_id, winner_name, winner_tag, winner_leader_uuid, winner_points, winner_members) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, season.getNumber());
            statement.setLong(2, season.getStartDate());
            statement.setObject(3, season.getEndDate());
            statement.setString(4, season.getWinnerGuildId() != null ? season.getWinnerGuildId().toString() : null);
            statement.setString(5, season.getWinnerName());
            statement.setString(6, season.getWinnerTag());
            statement.setString(7, season.getWinnerLeaderUuid() != null ? season.getWinnerLeaderUuid().toString() : null);
            statement.setObject(8, season.getWinnerPoints());
            statement.setObject(9, season.getWinnerMembers());
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Season season) throws SQLException {
        String sql = "UPDATE seasons SET start_date = ?, end_date = ?, winner_guild_id = ?, winner_name = ?, winner_tag = ?, winner_leader_uuid = ?, winner_points = ?, winner_members = ? WHERE number = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, season.getStartDate());
            statement.setObject(2, season.getEndDate());
            statement.setString(3, season.getWinnerGuildId() != null ? season.getWinnerGuildId().toString() : null);
            statement.setString(4, season.getWinnerName());
            statement.setString(5, season.getWinnerTag());
            statement.setString(6, season.getWinnerLeaderUuid() != null ? season.getWinnerLeaderUuid().toString() : null);
            statement.setObject(7, season.getWinnerPoints());
            statement.setObject(8, season.getWinnerMembers());
            statement.setInt(9, season.getNumber());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<Season> findByNumber(int number) throws SQLException {
        String sql = "SELECT * FROM seasons WHERE number = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, number);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Season> findAll() throws SQLException {
        List<Season> seasons = new ArrayList<>();
        String sql = "SELECT * FROM seasons ORDER BY number DESC";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                seasons.add(mapResultSet(resultSet));
            }
        }
        return seasons;
    }

    @Override
    public Optional<Season> findLatest() throws SQLException {
        String sql = "SELECT * FROM seasons ORDER BY number DESC LIMIT 1";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return Optional.of(mapResultSet(resultSet));
            }
        }
        return Optional.empty();
    }

    private Season mapResultSet(ResultSet resultSet) throws SQLException {
        String winnerGuildId = resultSet.getString("winner_guild_id");
        String winnerLeaderUuid = resultSet.getString("winner_leader_uuid");
        Object endDate = resultSet.getObject("end_date");
        Object winnerPoints = resultSet.getObject("winner_points");
        Object winnerMembers = resultSet.getObject("winner_members");
        return new Season(
                resultSet.getLong("id"),
                resultSet.getInt("number"),
                resultSet.getLong("start_date"),
                endDate != null ? ((Number) endDate).longValue() : null,
                winnerGuildId != null ? UUID.fromString(winnerGuildId) : null,
                resultSet.getString("winner_name"),
                resultSet.getString("winner_tag"),
                winnerLeaderUuid != null ? UUID.fromString(winnerLeaderUuid) : null,
                winnerPoints != null ? ((Number) winnerPoints).intValue() : null,
                winnerMembers != null ? ((Number) winnerMembers).intValue() : null
        );
    }
}
