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
        String sql = "INSERT INTO guilds (id, name, tag, color, leader_uuid, points, level, created_at, home_world, home_x, home_y, home_z) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guild.getId().toString());
            statement.setString(2, guild.getName());
            statement.setString(3, guild.getTag());
            statement.setString(4, guild.getColor());
            statement.setString(5, guild.getLeaderUuid().toString());
            statement.setInt(6, guild.getPoints());
            statement.setInt(7, guild.getLevel());
            statement.setLong(8, guild.getCreatedAt());
            statement.setString(9, guild.getHomeWorld());
            statement.setDouble(10, guild.getHomeX());
            statement.setDouble(11, guild.getHomeY());
            statement.setDouble(12, guild.getHomeZ());
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Guild guild) throws SQLException {
        String sql = "UPDATE guilds SET name = ?, tag = ?, color = ?, leader_uuid = ?, points = ?, level = ?, created_at = ?, home_world = ?, home_x = ?, home_y = ?, home_z = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guild.getName());
            statement.setString(2, guild.getTag());
            statement.setString(3, guild.getColor());
            statement.setString(4, guild.getLeaderUuid().toString());
            statement.setInt(5, guild.getPoints());
            statement.setInt(6, guild.getLevel());
            statement.setLong(7, guild.getCreatedAt());
            statement.setString(8, guild.getHomeWorld());
            statement.setDouble(9, guild.getHomeX());
            statement.setDouble(10, guild.getHomeY());
            statement.setDouble(11, guild.getHomeZ());
            statement.setString(12, guild.getId().toString());
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
        Guild guild = new Guild(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("name"),
                resultSet.getString("tag"),
                resultSet.getString("color"),
                UUID.fromString(resultSet.getString("leader_uuid")),
                resultSet.getInt("points"),
                resultSet.getInt("level"),
                resultSet.getLong("created_at")
        );
        guild.setHomeWorld(resultSet.getString("home_world"));
        guild.setHomeX(resultSet.getDouble("home_x"));
        guild.setHomeY(resultSet.getDouble("home_y"));
        guild.setHomeZ(resultSet.getDouble("home_z"));
        return guild;
    }
}
