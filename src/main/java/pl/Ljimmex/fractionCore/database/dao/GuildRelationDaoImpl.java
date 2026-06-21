package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildRelation;
import pl.Ljimmex.fractionCore.database.entity.RelationType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuildRelationDaoImpl implements GuildRelationDao {

    private final DatabaseManager databaseManager;

    public GuildRelationDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(GuildRelation relation) throws SQLException {
        String sql = "INSERT INTO guild_relations (guild1_id, guild2_id, type, created_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            UUID[] normalized = normalize(relation.getGuild1Id(), relation.getGuild2Id());
            statement.setString(1, normalized[0].toString());
            statement.setString(2, normalized[1].toString());
            statement.setString(3, relation.getType().name());
            statement.setLong(4, relation.getCreatedAt());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID guild1Id, UUID guild2Id) throws SQLException {
        String sql = "DELETE FROM guild_relations WHERE guild1_id = ? AND guild2_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            UUID[] normalized = normalize(guild1Id, guild2Id);
            statement.setString(1, normalized[0].toString());
            statement.setString(2, normalized[1].toString());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteByGuild(UUID guildId) throws SQLException {
        String sql = "DELETE FROM guild_relations WHERE guild1_id = ? OR guild2_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, guildId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<GuildRelation> find(UUID guild1Id, UUID guild2Id) throws SQLException {
        String sql = "SELECT * FROM guild_relations WHERE guild1_id = ? AND guild2_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            UUID[] normalized = normalize(guild1Id, guild2Id);
            statement.setString(1, normalized[0].toString());
            statement.setString(2, normalized[1].toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<GuildRelation> findByGuild(UUID guildId) throws SQLException {
        List<GuildRelation> relations = new ArrayList<>();
        String sql = "SELECT * FROM guild_relations WHERE guild1_id = ? OR guild2_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    relations.add(mapResultSet(resultSet));
                }
            }
        }
        return relations;
    }

    @Override
    public boolean exists(UUID guild1Id, UUID guild2Id) throws SQLException {
        return find(guild1Id, guild2Id).isPresent();
    }

    @Override
    public void setType(UUID guild1Id, UUID guild2Id, RelationType type) throws SQLException {
        Optional<GuildRelation> existing = find(guild1Id, guild2Id);
        if (existing.isPresent()) {
            String sql = "UPDATE guild_relations SET type = ? WHERE guild1_id = ? AND guild2_id = ?";
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                UUID[] normalized = normalize(guild1Id, guild2Id);
                statement.setString(1, type.name());
                statement.setString(2, normalized[0].toString());
                statement.setString(3, normalized[1].toString());
                statement.executeUpdate();
            }
        } else {
            save(new GuildRelation(0, guild1Id, guild2Id, type, System.currentTimeMillis() / 1000));
        }
    }

    private GuildRelation mapResultSet(ResultSet resultSet) throws SQLException {
        return new GuildRelation(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("guild1_id")),
                UUID.fromString(resultSet.getString("guild2_id")),
                RelationType.valueOf(resultSet.getString("type")),
                resultSet.getLong("created_at")
        );
    }

    private UUID[] normalize(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? new UUID[]{a, b} : new UUID[]{b, a};
    }
}
