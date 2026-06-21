package pl.Ljimmex.fractionCore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.command.GuildCommand;
import pl.Ljimmex.fractionCore.config.ConfigManager;
import pl.Ljimmex.fractionCore.config.DebugManager;
import pl.Ljimmex.fractionCore.database.dao.CuboidDao;
import pl.Ljimmex.fractionCore.database.dao.GuildBanDao;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.GuildDisbandHistoryDao;
import pl.Ljimmex.fractionCore.database.dao.GuildInviteDao;
import pl.Ljimmex.fractionCore.database.dao.GuildAllyRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildJoinRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildRelationDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.module.ModuleManager;
import pl.Ljimmex.fractionCore.module.modules.CuboidModule;
import pl.Ljimmex.fractionCore.module.modules.DatabaseModule;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.listener.PlayerConnectionListener;
import pl.Ljimmex.fractionCore.module.modules.EconomyModule;
import pl.Ljimmex.fractionCore.module.modules.EggModule;
import pl.Ljimmex.fractionCore.module.modules.GuiModule;
import pl.Ljimmex.fractionCore.module.modules.GuildModule;
import pl.Ljimmex.fractionCore.module.modules.LangModule;
import pl.Ljimmex.fractionCore.module.modules.MapModule;
import pl.Ljimmex.fractionCore.module.modules.RankingModule;
import pl.Ljimmex.fractionCore.module.modules.TabModule;
import pl.Ljimmex.fractionCore.module.modules.VillagersModule;
import pl.Ljimmex.fractionCore.module.modules.WebhookModule;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildService;
import pl.Ljimmex.fractionCore.module.modules.BackupModule;

import java.util.Objects;

public final class FractionCore extends JavaPlugin {

    private ConfigManager configManager;
    private DebugManager debugManager;
    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.initialize();

        debugManager = new DebugManager(this);
        debugManager.loadConfiguration(getConfig());

        moduleManager = new ModuleManager(this);
        registerModules();

        FileConfiguration config = getConfig();
        moduleManager.loadConfiguration(config);
        moduleManager.enableModules();

        GuildService guildService = createGuildService();
        GuildModule guildModule = (GuildModule) moduleManager.getModule("guild");
        if (guildModule != null) {
            guildModule.setGuildService(guildService);
        }

        LangManager langManager = ((LangModule) moduleManager.getModule("lang")).getLangManager();
        GuildCommand guildCommand = new GuildCommand(this, moduleManager, guildService, langManager);
        org.bukkit.command.PluginCommand guildPluginCommand = Objects.requireNonNull(getCommand("guild"));
        guildPluginCommand.setExecutor(guildCommand);
        guildPluginCommand.setTabCompleter(guildCommand);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        printStartupBanner();
        getLogger().info("FractionCore enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableModules();
        }
        getLogger().info("FractionCore disabled.");
    }

    private void registerModules() {
        moduleManager.registerModule(new DatabaseModule(this));
        moduleManager.registerModule(new LangModule(this));
        moduleManager.registerModule(new EconomyModule(this));
        moduleManager.registerModule(new GuildModule(this));
        moduleManager.registerModule(new CuboidModule(this));
        moduleManager.registerModule(new EggModule(this));
        moduleManager.registerModule(new RankingModule(this));
        moduleManager.registerModule(new GuiModule(this));
        moduleManager.registerModule(new TabModule(this));
        moduleManager.registerModule(new MapModule(this));
        moduleManager.registerModule(new VillagersModule(this));
        moduleManager.registerModule(new BackupModule(this));
        moduleManager.registerModule(new WebhookModule(this));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DebugManager getDebugManager() {
        return debugManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    private GuildService createGuildService() {
        DatabaseModule databaseModule = (DatabaseModule) moduleManager.getModule("database");
        if (databaseModule == null) {
            throw new IllegalStateException("Database module is required for guild service");
        }
        LangModule langModule = (LangModule) moduleManager.getModule("lang");
        if (langModule == null || langModule.getLangManager() == null) {
            throw new IllegalStateException("Lang module is required for guild service");
        }
        LangManager langManager = langModule.getLangManager();
        GuildDao guildDao = databaseModule.getGuildDao();
        PlayerDao playerDao = databaseModule.getPlayerDao();
        CuboidDao cuboidDao = databaseModule.getCuboidDao();
        GuildBanDao guildBanDao = databaseModule.getGuildBanDao();
        GuildInviteDao guildInviteDao = databaseModule.getGuildInviteDao();
        GuildJoinRequestDao guildJoinRequestDao = databaseModule.getGuildJoinRequestDao();
        GuildRelationDao guildRelationDao = databaseModule.getGuildRelationDao();
        GuildAllyRequestDao guildAllyRequestDao = databaseModule.getGuildAllyRequestDao();
        GuildDisbandHistoryDao guildDisbandHistoryDao = databaseModule.getGuildDisbandHistoryDao();
        return new GuildService(this, guildDao, playerDao, cuboidDao, guildBanDao, guildInviteDao, guildJoinRequestDao,
                guildRelationDao, guildAllyRequestDao, guildDisbandHistoryDao, configManager.getModuleConfig("guild"), langManager);
    }

    private void printStartupBanner() {
        String version = getPluginMeta().getVersion();
        String author = getPluginMeta().getAuthors().isEmpty() ? "Ljimex" : getPluginMeta().getAuthors().get(0);

        Component border = Component.text("+============================+").color(NamedTextColor.GOLD);
        Component separator = Component.text("|----------------------------|").color(NamedTextColor.YELLOW);
        Component title = MiniMessage.miniMessage().deserialize("<gradient:gold:yellow:aqua>|        FractionCore        |</gradient>").decorate(TextDecoration.BOLD);
        Component subtitle = Component.text("| Fraction Guild Clans v2.0  |").color(NamedTextColor.GRAY);
        Component versionLine = Component.text("|        Version " + version + "       |").color(NamedTextColor.GRAY);
        Component authorLine = Component.text("|         by " + author + "          |").color(NamedTextColor.GRAY);

        getServer().getConsoleSender().sendMessage(border);
        getServer().getConsoleSender().sendMessage(title);
        getServer().getConsoleSender().sendMessage(separator);
        getServer().getConsoleSender().sendMessage(subtitle);
        getServer().getConsoleSender().sendMessage(versionLine);
        getServer().getConsoleSender().sendMessage(authorLine);
        getServer().getConsoleSender().sendMessage(border);
    }
}
