package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.GuildDisbandHistory;

import java.sql.SQLException;
import java.util.List;

public interface GuildDisbandHistoryDao {

    void save(GuildDisbandHistory history) throws SQLException;

    List<GuildDisbandHistory> findAll() throws SQLException;
}
