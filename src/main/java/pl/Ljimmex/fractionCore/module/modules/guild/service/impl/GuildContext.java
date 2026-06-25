package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Ljimmex.fractionCore.config.ModuleConfig;
import pl.Ljimmex.fractionCore.database.dao.CuboidDao;
import pl.Ljimmex.fractionCore.database.dao.GuildAllyRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildBanDao;
import pl.Ljimmex.fractionCore.database.dao.GuildDao;
import pl.Ljimmex.fractionCore.database.dao.GuildDisbandHistoryDao;
import pl.Ljimmex.fractionCore.database.dao.GuildFlagDao;
import pl.Ljimmex.fractionCore.database.dao.GuildInviteDao;
import pl.Ljimmex.fractionCore.database.dao.GuildJoinRequestDao;
import pl.Ljimmex.fractionCore.database.dao.GuildRelationDao;
import pl.Ljimmex.fractionCore.database.dao.PlayerDao;
import pl.Ljimmex.fractionCore.database.entity.GuildRank;
import pl.Ljimmex.fractionCore.database.entity.PlayerData;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildTagManager;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GuildContext {

    final JavaPlugin plugin;
    final GuildDao guildDao;
    final PlayerDao playerDao;
    final CuboidDao cuboidDao;
    final GuildBanDao guildBanDao;
    final GuildInviteDao guildInviteDao;
    final GuildJoinRequestDao guildJoinRequestDao;
    final GuildRelationDao guildRelationDao;
    final GuildAllyRequestDao guildAllyRequestDao;
    final GuildDisbandHistoryDao guildDisbandHistoryDao;
    final GuildFlagDao guildFlagDao;
    final ModuleConfig guildConfig;
    final LangManager langManager;
    final GuildTagManager tagManager;
    final GuildRelationManager relationManager;
    final CuboidManager cuboidManager;

    public GuildContext(JavaPlugin plugin, GuildDao guildDao, PlayerDao playerDao, CuboidDao cuboidDao,
                        GuildBanDao guildBanDao, GuildInviteDao guildInviteDao, GuildJoinRequestDao guildJoinRequestDao,
                        GuildRelationDao guildRelationDao, GuildAllyRequestDao guildAllyRequestDao,
                        GuildDisbandHistoryDao guildDisbandHistoryDao,
                        GuildFlagDao guildFlagDao,
                        ModuleConfig guildConfig, LangManager langManager) {
        this.plugin = plugin;
        this.guildDao = guildDao;
        this.playerDao = playerDao;
        this.cuboidDao = cuboidDao;
        this.guildBanDao = guildBanDao;
        this.guildInviteDao = guildInviteDao;
        this.guildJoinRequestDao = guildJoinRequestDao;
        this.guildRelationDao = guildRelationDao;
        this.guildAllyRequestDao = guildAllyRequestDao;
        this.guildDisbandHistoryDao = guildDisbandHistoryDao;
        this.guildFlagDao = guildFlagDao;
        this.guildConfig = guildConfig;
        this.langManager = langManager;
        this.relationManager = new GuildRelationManager(this);
        this.tagManager = new GuildTagManager(playerDao, guildDao, guildConfig, relationManager);
        this.cuboidManager = new CuboidManager(this);
    }

    public JavaPlugin getPlugin() {
        return plugin;
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

    public GuildDisbandHistoryDao getGuildDisbandHistoryDao() {
        return guildDisbandHistoryDao;
    }

    public ModuleConfig getGuildConfig() {
        return guildConfig;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public GuildTagManager getTagManager() {
        return tagManager;
    }

    public GuildRelationManager getRelationManager() {
        return relationManager;
    }

    public CuboidManager getCuboidManager() {
        return cuboidManager;
    }

    public boolean isModeratorOrHigher(GuildRank rank) {
        return rank == GuildRank.LEADER || rank == GuildRank.CO_LEADER || rank == GuildRank.MODERATOR;
    }

    public boolean isLeaderOrCoLeader(GuildRank rank) {
        return rank == GuildRank.LEADER || rank == GuildRank.CO_LEADER;
    }

    public void send(Audience audience, String key, MessageType type, PlaceholderContext context) {
        String raw = langManager.getRawMessage(key);
        if (raw == null) {
            audience.sendMessage(Component.text("[Brak tlumaczenia: " + key + "]").color(NamedTextColor.RED));
            return;
        }
        audience.sendMessage(langManager.getMessage(key, type, context));
    }

    public void broadcastToGuild(UUID guildId, Component message, Player exclude) {
        try {
            List<PlayerData> members = playerDao.findByGuild(guildId);
            for (PlayerData member : members) {
                Player online = Bukkit.getPlayer(member.getUuid());
                if (online == null || (exclude != null && online.getUniqueId().equals(exclude.getUniqueId()))) {
                    continue;
                }
                online.sendMessage(message);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to broadcast to guild: " + e.getMessage());
        }
    }

    public PlayerData getOrCreatePlayerData(Player player) throws SQLException {
        Optional<PlayerData> existing = playerDao.findByUuid(player.getUniqueId());
        if (existing.isPresent()) {
            PlayerData data = existing.get();
            data.setName(player.getName());
            return data;
        }
        PlayerData data = new PlayerData(player.getUniqueId(), player.getName(), null, null, 0, 0, 0, 1000, 0, 0);
        playerDao.save(data);
        return data;
    }

    public Optional<PlayerData> resolveTargetData(String name) throws SQLException {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            Optional<PlayerData> data = playerDao.findByUuid(online.getUniqueId());
            if (data.isPresent()) {
                return data;
            }
            PlayerData created = new PlayerData(online.getUniqueId(), online.getName(), null, null, 0, 0, 0, 1000, 0, 0);
            playerDao.save(created);
            return Optional.of(created);
        }
        return playerDao.findByName(name);
    }

    public void removeFromGuild(PlayerData data) throws SQLException {
        data.setGuildId(null);
        data.setRank(null);
        data.setJoinedGuildAt(0);
        data.setLeftGuildAt(System.currentTimeMillis() / 1000);
        playerDao.update(data);
    }

    public String formatDate(long timestampSeconds) {
        return Instant.ofEpochSecond(timestampSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString();
    }

    public int getMemberCount(UUID guildId) throws SQLException {
        return playerDao.findByGuild(guildId).size();
    }

    public boolean isJoinCostEnabled() {
        return guildConfig.getBoolean("member-management.join-cost.enabled", false);
    }

    public boolean hasItems(Player player, List<CostItem> costItems) {
        for (CostItem costItem : costItems) {
            if (!player.getInventory().containsAtLeast(new ItemStack(costItem.material()), costItem.amount())) {
                return false;
            }
        }
        return true;
    }

    public boolean deductItems(Player player, List<CostItem> costItems) {
        if (!hasItems(player, costItems)) {
            return false;
        }
        plugin.getLogger().info("[FractionCore] Deducting cost from " + player.getName() + ": " + costItems.stream()
                .map(costItem -> costItem.amount() + "x " + costItem.material())
                .toList());
        for (CostItem costItem : costItems) {
            int remaining = costItem.amount();
            ItemStack[] storage = player.getInventory().getStorageContents();
            for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
                ItemStack stack = storage[slot];
                if (stack == null || stack.getType() != costItem.material()) {
                    continue;
                }
                int remove = Math.min(remaining, stack.getAmount());
                int newAmount = stack.getAmount() - remove;
                if (newAmount <= 0) {
                    player.getInventory().setItem(slot, new ItemStack(Material.AIR));
                } else {
                    ItemStack newStack = stack.clone();
                    newStack.setAmount(newAmount);
                    player.getInventory().setItem(slot, newStack);
                }
                remaining -= remove;
            }
            if (remaining > 0) {
                ItemStack offHand = player.getInventory().getItemInOffHand();
                if (offHand.getType() == costItem.material()) {
                    int remove = Math.min(remaining, offHand.getAmount());
                    int newAmount = offHand.getAmount() - remove;
                    if (newAmount <= 0) {
                        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                    } else {
                        ItemStack newStack = offHand.clone();
                        newStack.setAmount(newAmount);
                        player.getInventory().setItemInOffHand(newStack);
                    }
                    remaining -= remove;
                }
            }
            plugin.getLogger().info("[FractionCore] After deducting " + costItem.material() + " remaining=" + remaining);
            if (remaining > 0) {
                return false;
            }
        }
        player.updateInventory();
        return true;
    }

    public String formatCostItems(List<CostItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(i -> i.amount() + "x " + i.material().name())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public List<CostItem> loadCostItemsAtPath(String path) {
        List<Map<String, Object>> items = guildConfig.getMapList(path);
        if (items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(map -> {
                    String materialName = String.valueOf(map.get("material")).toUpperCase();
                    Object amountObj = map.get("amount");
                    int amount = amountObj instanceof Number number ? number.intValue() : 1;
                    Material material;
                    try {
                        material = Material.valueOf(materialName);
                    } catch (IllegalArgumentException e) {
                        material = Material.AIR;
                    }
                    return new CostItem(material, amount);
                })
                .filter(item -> item.material() != Material.AIR && item.amount() > 0)
                .toList();
    }

    public List<CostItem> loadJoinCostItems() {
        return loadCostItemsAtPath("member-management.join-cost.items");
    }

    public List<CostItem> loadFoundationCostItems() {
        return loadCostItemsAtPath("foundation-cost.items");
    }

    public void giveItems(Player player, List<CostItem> costItems) {
        for (CostItem costItem : costItems) {
            if (costItem.amount() <= 0 || costItem.material() == Material.AIR) {
                continue;
            }
            ItemStack stack = new ItemStack(costItem.material(), costItem.amount());
            java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        player.updateInventory();
    }
}
