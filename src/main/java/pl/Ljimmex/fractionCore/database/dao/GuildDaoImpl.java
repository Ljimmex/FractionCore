package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.Guild;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuildDaoImpl implements GuildDao {

    private final DatabaseManager databaseManager;

    public GuildDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(Guild guild) throws SQLException {
        String sql = "INSERT INTO guilds (id, name, tag, leader_uuid, points, level, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guild.getId().toString());
            statement.setString(2, guild.getName());
            statement.setString(3, guild.getTag());
            statement.setString(4, guild.getLeaderUuid().toString());
            statement.setInt(5, guild.getPoints());
            statement.setInt(6, guild.getLevel());
            statement.setLong(7, guild.getCreatedAt());
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Guild guild) throws SQLException {
        String sql = "UPDATE guilds SET name = ?, tag = ?, leader_uuid = ?, points = ?, level = ?, created_at = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guild.getName());
            statement.setString(2, guild.getTag());
            statement.setString(3, guild.getLeaderUuid().toString());
            statement.setInt(4, guild.getPoints());
            statement.setInt(5, guild.getLevel());
            statement.setLong(6, guild.getCreatedAt());
            statement.setString(7, guild.getId().toString());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID id) throws SQLException {
        String sql = "DELETE FROM guilds WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<Guild> findById(UUID id) throws SQLException {
        String sql = "SELECT * FROM guilds WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Guild> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM guilds WHERE name = ?";
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
    public Optional<Guild> findByTag(String tag) throws SQLException {
        String sql = "SELECT * FROM guilds WHERE tag = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tag);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Guild> findAll() throws SQLException {
        List<Guild> guilds = new ArrayList<>();
        String sql = "SELECT * FROM guilds";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                guilds.add(mapResultSet(resultSet));
            }
        }
        return guilds;
    }

    @Override
    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT 1 FROM guilds WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @Override
    public boolean existsByTag(String tag) throws SQLException {
        String sql = "SELECT 1 FROM guilds WHERE tag = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tag);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Guild mapResultSet(ResultSet resultSet) throws SQLException {
        return new Guild(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("name"),
                resultSet.getString("tag"),
                UUID.fromString(resultSet.getString("leader_uuid")),
                resultSet.getInt("points"),
                resultSet.getInt("level"),
                resultSet.getLong("created_at")
        );
    }
}
