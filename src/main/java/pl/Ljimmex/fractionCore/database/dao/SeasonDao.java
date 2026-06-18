package pl.Ljimmex.fractionCore.database.dao;

import pl.Ljimmex.fractionCore.database.entity.Season;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SeasonDao {

    void save(Season season) throws SQLException;

    void update(Season season) throws SQLException;

    Optional<Season> findByNumber(int number) throws SQLException;

    List<Season> findAll() throws SQLException;

    Optional<Season> findLatest() throws SQLException;
}
