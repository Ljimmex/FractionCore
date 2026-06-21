package pl.Ljimmex.fractionCore.module.modules;

import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.database.DatabaseManager;
import pl.Ljimmex.fractionCore.database.dao.CuboidDao;
import pl.Ljimmex.fractionCore.database.dao.CuboidDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.GuildActivityLogDao;
import pl.Ljimmex.fractionCore.database.dao.GuildActivityLogDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.GuildBanDao;
import pl.Ljimmex.fractionCore.database.dao.GuildBanDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.GuildInviteDao;
import pl.Ljimmex.fractionCore.database.dao.GuildInviteDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.GuildJoinRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildJoinRequestDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.GuildRelationDao;
import pl.Ljimmex.fractionCore.database.dao.GuildRelationDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.GuildAllyRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildAllyRequestDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.GuildDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.GuildDisbandHistoryDao;
import pl.Ljimmex.fractionCore.database.dao.GuildDisbandHistoryDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.GuildEggLogDao;
import pl.Ljimmex.fractionCore.database.dao.GuildEggLogDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDaoImpl;
import pl.Ljimmex.fractionCore.database.dao.SeasonDao;
import pl.Ljimmex.fractionCore.database.dao.SeasonDaoImpl;
import pl.Ljimmex.fractionCore.module.BaseModule;

import java.sql.SQLException;

public class DatabaseModule extends BaseModule {

    private DatabaseManager databaseManager;
    private GuildDao guildDao;
    private PlayerDao playerDao;
    private CuboidDao cuboidDao;
    private GuildBanDao guildBanDao;
    private GuildInviteDao guildInviteDao;
    private GuildJoinRequestDao guildJoinRequestDao;
    private GuildRelationDao guildRelationDao;
    private GuildAllyRequestDao guildAllyRequestDao;
    private GuildActivityLogDao guildActivityLogDao;
    private GuildEggLogDao guildEggLogDao;
    private GuildDisbandHistoryDao guildDisbandHistoryDao;
    private SeasonDao seasonDao;

    public DatabaseModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public void onEnable() {
        databaseManager = new DatabaseManager(getPlugin());
        databaseManager.loadConfiguration(getPlugin().getConfig());
        try {
            databaseManager.connect();
            playerDao = new PlayerDaoImpl(databaseManager);
            guildDao = new GuildDaoImpl(databaseManager);
            cuboidDao = new CuboidDaoImpl(databaseManager);
            guildBanDao = new GuildBanDaoImpl(databaseManager);
            guildInviteDao = new GuildInviteDaoImpl(databaseManager);
            guildJoinRequestDao = new GuildJoinRequestDaoImpl(databaseManager);
            guildRelationDao = new GuildRelationDaoImpl(databaseManager);
            guildAllyRequestDao = new GuildAllyRequestDaoImpl(databaseManager);
            guildActivityLogDao = new GuildActivityLogDaoImpl(databaseManager);
            guildEggLogDao = new GuildEggLogDaoImpl(databaseManager);
            guildDisbandHistoryDao = new GuildDisbandHistoryDaoImpl(databaseManager);
            seasonDao = new SeasonDaoImpl(databaseManager);
        } catch (SQLException e) {
            getPlugin().getLogger().severe("Failed to connect to database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public GuildDao getGuildDao() {
        return guildDao;
    }

    public PlayerDao getPlayerDao() {
        return playerDao;
    }

    public CuboidDao getCuboidDao() {
        return cuboidDao;
    }

    public GuildBanDao getGuildBanDao() {
        return guildBanDao;
    }

    public GuildInviteDao getGuildInviteDao() {
        return guildInviteDao;
    }

    public GuildJoinRequestDao getGuildJoinRequestDao() {
        return guildJoinRequestDao;
    }

    public GuildRelationDao getGuildRelationDao() {
        return guildRelationDao;
    }

    public GuildAllyRequestDao getGuildAllyRequestDao() {
        return guildAllyRequestDao;
    }

    public GuildActivityLogDao getGuildActivityLogDao() {
        return guildActivityLogDao;
    }

    public GuildEggLogDao getGuildEggLogDao() {
        return guildEggLogDao;
    }

    public GuildDisbandHistoryDao getGuildDisbandHistoryDao() {
        return guildDisbandHistoryDao;
    }

    public SeasonDao getSeasonDao() {
        return seasonDao;
    }
}
