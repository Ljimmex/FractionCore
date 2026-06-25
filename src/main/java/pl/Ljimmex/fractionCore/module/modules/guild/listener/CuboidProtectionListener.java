package pl.Ljimmex.fractionCore.module.modules.guild.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.Ljimmex.fractionCore.cuboid.model.CuboidAction;
import pl.Ljimmex.fractionCore.cuboid.model.CuboidFlagType;
import pl.Ljimmex.fractionCore.cuboid.model.CuboidFlagValue;
import pl.Ljimmex.fractionCore.database.entity.CuboidData;
import pl.Ljimmex.fractionCore.lang.LangManager;
import pl.Ljimmex.fractionCore.lang.MessageType;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.module.modules.guild.service.impl.CuboidManager;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CuboidProtectionListener implements Listener {

    private final CuboidManager cuboidManager;
    private final LangManager langManager;
    private final Map<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 1000;

    public CuboidProtectionListener(CuboidManager cuboidManager, LangManager langManager) {
        this.cuboidManager = cuboidManager;
        this.langManager = langManager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!cuboidManager.isAllowed(event.getPlayer(), event.getBlock().getLocation(), CuboidAction.DESTROY)) {
            event.setCancelled(true);
            sendDenial(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!cuboidManager.isAllowed(event.getPlayer(), event.getBlock().getLocation(), CuboidAction.BUILD)) {
            event.setCancelled(true);
            sendDenial(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }
        Block clicked = event.getClickedBlock();
        CuboidAction action = (clicked != null && isUseBlock(clicked)) ? CuboidAction.USE : CuboidAction.INTERACT;
        Location location = clicked != null ? clicked.getLocation() : event.getPlayer().getLocation();
        if (!cuboidManager.isAllowed(event.getPlayer(), location, action)) {
            event.setCancelled(true);
            sendDenial(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        if (!(damager instanceof Player attacker) || !(victim instanceof Player target)) {
            return;
        }

        Optional<CuboidData> cuboidOpt = cuboidManager.findCuboidAt(victim.getLocation());
        if (cuboidOpt.isEmpty()) {
            return;
        }

        CuboidData cuboid = cuboidOpt.get();
        UUID guildId = cuboid.getGuildId();

        Optional<UUID> attackerGuild = cuboidManager.getPlayerGuildId(attacker);
        Optional<UUID> targetGuild = cuboidManager.getPlayerGuildId(target);
        if (attackerGuild.isEmpty() || targetGuild.isEmpty() || !attackerGuild.get().equals(targetGuild.get())) {
            return;
        }

        CuboidFlagValue flag = cuboidManager.getFlag(guildId, CuboidFlagType.FRIENDLY_FIRE);
        if (flag == CuboidFlagValue.ALLOW) {
            return;
        }
        if (flag == CuboidFlagValue.DENY) {
            event.setCancelled(true);
            sendDenial(attacker);
            return;
        }

        CuboidManager.AccessLevel access = cuboidManager.getAccessLevel(attacker, guildId);
        boolean allowed = switch (flag) {
            case MEMBERS -> access == CuboidManager.AccessLevel.LEADER
                    || access == CuboidManager.AccessLevel.MEMBER;
            case ALLIES -> access == CuboidManager.AccessLevel.LEADER
                    || access == CuboidManager.AccessLevel.MEMBER
                    || access == CuboidManager.AccessLevel.ALLY;
            case LEADER -> access == CuboidManager.AccessLevel.LEADER;
            default -> false;
        };

        if (!allowed) {
            event.setCancelled(true);
            sendDenial(attacker);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location location = event.getLocation();
        Optional<CuboidData> cuboidOpt = cuboidManager.findCuboidAt(location);
        if (cuboidOpt.isEmpty()) {
            return;
        }

        CuboidData cuboid = cuboidOpt.get();
        UUID guildId = cuboid.getGuildId();
        CuboidFlagValue flag = cuboidManager.getFlag(guildId, CuboidFlagType.TNT);

        if (flag == CuboidFlagValue.ALLOW) {
            cuboidManager.activateTntCooldown(guildId);
            return;
        }
        if (flag == CuboidFlagValue.DENY) {
            event.setCancelled(true);
            return;
        }

        Player source = null;
        Entity entity = event.getEntity();
        if (entity instanceof TNTPrimed tnt) {
            Entity tntSource = tnt.getSource();
            if (tntSource instanceof Player player) {
                source = player;
            }
        }

        CuboidManager.AccessLevel access = source != null
                ? cuboidManager.getAccessLevel(source, guildId)
                : CuboidManager.AccessLevel.OUTSIDER;

        // TNT flag works inversely to other flags: it blocks the listed group,
        // so e.g. MEMBERS blocks members from griefing their own guild while
        // allowing enemy players to raid during guild wars.
        boolean blocked = switch (flag) {
            case MEMBERS -> access == CuboidManager.AccessLevel.LEADER
                    || access == CuboidManager.AccessLevel.MEMBER;
            case ALLIES -> access == CuboidManager.AccessLevel.LEADER
                    || access == CuboidManager.AccessLevel.MEMBER
                    || access == CuboidManager.AccessLevel.ALLY;
            case LEADER -> access == CuboidManager.AccessLevel.LEADER;
            default -> false;
        };

        if (blocked) {
            event.setCancelled(true);
            if (source != null) {
                sendDenial(source);
            }
        } else {
            cuboidManager.activateTntCooldown(guildId);
        }
    }

    private boolean isUseBlock(Block block) {
        BlockData data = block.getBlockData();
        return block.getState() instanceof Container
                || data instanceof Openable
                || data instanceof Switch;
    }

    private void sendDenial(Player player) {
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(player.getUniqueId());
        if (last != null && now - last < MESSAGE_COOLDOWN_MS) {
            return;
        }
        messageCooldowns.put(player.getUniqueId(), now);

        UUID guildId = cuboidManager.getGuildAt(player.getLocation()).orElse(null);
        if (guildId != null && cuboidManager.isTntCooldownActive(guildId)) {
            long remaining = cuboidManager.getRemainingTntCooldownSeconds(guildId);
            Component message = langManager.getMessage("guild.cuboid.protection.tnt_cooldown", MessageType.ERROR,
                    PlaceholderContext.of(player).with("seconds", remaining));
            if (message != null) {
                player.sendMessage(message);
            }
            return;
        }

        Component message = langManager.getMessage("guild.cuboid.protection.denied", MessageType.ERROR,
                PlaceholderContext.of(player));
        if (message != null) {
            player.sendMessage(message);
        }
    }
}
