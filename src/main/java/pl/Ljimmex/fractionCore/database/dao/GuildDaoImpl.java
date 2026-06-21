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
        String sql = "INSERT INTO guilds (id, name, tag, color, leader_uuid, points, level, created_at, home_world, home_x, home_y, home_z, description, is_public, allow_join_requests, show_home) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guild.getId().toString());
            setGuildStatement(statement, guild, 2);
            statement.executeUpdate();
        }
    }

    @Override
    public void update(Guild guild) throws SQLException {
        String sql = "UPDATE guilds SET name = ?, tag = ?, color = ?, leader_uuid = ?, points = ?, level = ?, created_at = ?, home_world = ?, home_x = ?, home_y = ?, home_z = ?, description = ?, is_public = ?, allow_join_requests = ?, show_home = ? WHERE id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = setGuildStatement(statement, guild, 1);
            statement.setString(index, guild.getId().toString());
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

    private int setGuildStatement(PreparedStatement statement, Guild guild, int startIndex) throws SQLException {
        int i = startIndex;
        statement.setString(i++, guild.getName());
        statement.setString(i++, guild.getTag());
        statement.setString(i++, guild.getColor());
        statement.setString(i++, guild.getLeaderUuid().toString());
        statement.setInt(i++, guild.getPoints());
        statement.setInt(i++, guild.getLevel());
        statement.setLong(i++, guild.getCreatedAt());
        statement.setString(i++, guild.getHomeWorld());
        statement.setDouble(i++, guild.getHomeX());
        statement.setDouble(i++, guild.getHomeY());
        statement.setDouble(i++, guild.getHomeZ());
        statement.setString(i++, guild.getDescription());
        statement.setInt(i++, guild.isPublic() ? 1 : 0);
        statement.setInt(i++, guild.isAllowJoinRequests() ? 1 : 0);
        statement.setInt(i++, guild.isShowHome() ? 1 : 0);
        return i;
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
        guild.setDescription(resultSet.getString("description"));
        guild.setPublic(resultSet.getInt("is_public") == 1);
        guild.setAllowJoinRequests(resultSet.getInt("allow_join_requests") == 1);
        guild.setShowHome(resultSet.getInt("show_home") == 1);
        return guild;
    }
}
