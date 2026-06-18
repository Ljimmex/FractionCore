package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.CuboidData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CuboidDaoImpl implements CuboidDao {

    private final DatabaseManager databaseManager;

    public CuboidDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(CuboidData cuboid) throws SQLException {
        String sql = "INSERT INTO cuboids (guild_id, world, center_x, center_y, center_z, radius, level) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cuboid.getGuildId().toString());
            statement.setString(2, cuboid.getWorld());
            statement.setInt(3, cuboid.getCenterX());
            statement.setInt(4, cuboid.getCenterY());
            statement.setInt(5, cuboid.getCenterZ());
            statement.setInt(6, cuboid.getRadius());
            statement.setInt(7, cuboid.getLevel());
            statement.executeUpdate();
        }
    }

    @Override
    public void update(CuboidData cuboid) throws SQLException {
        String sql = "UPDATE cuboids SET world = ?, center_x = ?, center_y = ?, center_z = ?, radius = ?, level = ? WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cuboid.getWorld());
            statement.setInt(2, cuboid.getCenterX());
            statement.setInt(3, cuboid.getCenterY());
            statement.setInt(4, cuboid.getCenterZ());
            statement.setInt(5, cuboid.getRadius());
            statement.setInt(6, cuboid.getLevel());
            statement.setString(7, cuboid.getGuildId().toString());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID guildId) throws SQLException {
        String sql = "DELETE FROM cuboids WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<CuboidData> findByGuildId(UUID guildId) throws SQLException {
        String sql = "SELECT * FROM cuboids WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<CuboidData> findAll() throws SQLException {
        List<CuboidData> cuboids = new ArrayList<>();
        String sql = "SELECT * FROM cuboids";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                cuboids.add(mapResultSet(resultSet));
            }
        }
        return cuboids;
    }

    private CuboidData mapResultSet(ResultSet resultSet) throws SQLException {
        return new CuboidData(
                UUID.fromString(resultSet.getString("guild_id")),
                resultSet.getString("world"),
                resultSet.getInt("center_x"),
                resultSet.getInt("center_y"),
                resultSet.getInt("center_z"),
                resultSet.getInt("radius"),
                resultSet.getInt("level")
        );
    }
}
