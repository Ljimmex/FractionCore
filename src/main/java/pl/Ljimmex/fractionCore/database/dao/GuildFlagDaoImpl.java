package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.entity.GuildFlag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuildFlagDaoImpl implements GuildFlagDao {

    private final DatabaseManager databaseManager;

    public GuildFlagDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(GuildFlag flag) throws SQLException {
        String sql = "INSERT INTO guild_flags (guild_id, flag_name, flag_value) VALUES (?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, flag.getGuildId().toString());
            statement.setString(2, flag.getFlagName());
            statement.setString(3, flag.getFlagValue());
            statement.executeUpdate();
        }
    }

    @Override
    public void update(GuildFlag flag) throws SQLException {
        String sql = "UPDATE guild_flags SET flag_value = ? WHERE guild_id = ? AND flag_name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, flag.getFlagValue());
            statement.setString(2, flag.getGuildId().toString());
            statement.setString(3, flag.getFlagName());
            statement.executeUpdate();
        }
    }

    @Override
    public void delete(UUID guildId, String flagName) throws SQLException {
        String sql = "DELETE FROM guild_flags WHERE guild_id = ? AND flag_name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, flagName);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteByGuild(UUID guildId) throws SQLException {
        String sql = "DELETE FROM guild_flags WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<GuildFlag> find(UUID guildId, String flagName) throws SQLException {
        String sql = "SELECT * FROM guild_flags WHERE guild_id = ? AND flag_name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            statement.setString(2, flagName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSet(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<GuildFlag> findByGuild(UUID guildId) throws SQLException {
        List<GuildFlag> flags = new ArrayList<>();
        String sql = "SELECT * FROM guild_flags WHERE guild_id = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guildId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    flags.add(mapResultSet(resultSet));
                }
            }
        }
        return flags;
    }

    private GuildFlag mapResultSet(ResultSet resultSet) throws SQLException {
        return new GuildFlag(
                UUID.fromString(resultSet.getString("guild_id")),
                resultSet.getString("flag_name"),
                resultSet.getString("flag_value")
        );
    }
}
