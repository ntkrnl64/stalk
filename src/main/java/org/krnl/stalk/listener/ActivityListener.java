package org.krnl.stalk.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.krnl.stalk.Stalk;

public class ActivityListener implements Listener {

    private final Stalk plugin;

    public ActivityListener(Stalk plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        String itemStr = formatItem(event.getPlayer().getInventory().getItemInMainHand());
        plugin.getLogManager().log(event.getPlayer(), "BLOCK_BREAK",
                b.getType().name() + " | Tool: " + itemStr);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        plugin.getLogManager().log(event.getPlayer(), "BLOCK_PLACE", b.getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null && event.getAction().name().contains("AIR")) return;

        String action = event.getAction().name();
        String target = (event.getClickedBlock() != null) ? event.getClickedBlock().getType().name() : "AIR";
        String handItem = formatItem(event.getItem());

        if (event.getClickedBlock() != null && event.getClickedBlock().getType() != Material.AIR) {
            plugin.getLogManager().log(event.getPlayer(), "INTERACT",
                    String.format("%s on %s | Hand: %s", action, target, handItem));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        plugin.getLogManager().log(event.getPlayer(), "DROP_ITEM",
                formatItem(event.getItemDrop().getItemStack()) + " (Toss)");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            Entity victim = event.getEntity();
            String victimInfo;

            if (victim instanceof Player victimPlayer) {
                victimInfo = String.format("Player: %s (UUID: %s)", victimPlayer.getName(), victimPlayer.getUniqueId());
            } else {
                victimInfo = String.format("Entity: %s (UUID: %s)", victim.getType().name(), victim.getUniqueId());
            }

            String weapon = formatItem(player.getInventory().getItemInMainHand());

            plugin.getLogManager().log(player, "ATTACK",
                    String.format("Target: [%s] | Dmg: %.2f | Weapon: %s", victimInfo, event.getFinalDamage(), weapon));
        }
    }

    private String formatItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "EMPTY_HAND";
        return item.getType().name() + " x" + item.getAmount();
    }
}
