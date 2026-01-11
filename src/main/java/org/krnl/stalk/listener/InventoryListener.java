package org.krnl.stalk.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.krnl.stalk.Stalk;

import java.util.Arrays;
import java.util.stream.Collectors;

public class InventoryListener implements Listener {

    private final Stalk plugin;

    public InventoryListener(Stalk plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            Inventory top = event.getInventory();
            if (isValidContainer(top)) {
                String locStr = getContainerLoc(top);
                plugin.getLogManager().log(player, "CONTAINER_OPEN",
                        "Type: " + top.getType().name() + " | Title: " + event.getView().getTitle() + " | " + locStr);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            Inventory top = event.getInventory();
            if (isValidContainer(top)) {
                String locStr = getContainerLoc(top);
                String contents = getInventoryContents(top);

                plugin.getLogManager().log(player, "CONTAINER_CLOSE",
                        "Type: " + top.getType().name() + " | " + locStr + " | Contents: " + contents);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.getLogManager().log(player, "PICKUP_ITEM",
                    formatItem(event.getItem().getItemStack()) + " (From Ground)");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        boolean hasItem = (currentItem != null && currentItem.getType() != Material.AIR) ||
                (cursorItem != null && cursorItem.getType() != Material.AIR);
        if (!hasItem) return;

        String action = event.getAction().name();
        String invType = clickedInv.getType().name();
        String itemInfo;
        if (event.isShiftClick()) itemInfo = "Shift-Move: " + formatItem(currentItem);
        else if (cursorItem != null && cursorItem.getType() != Material.AIR) itemInfo = "Place/Swap: " + formatItem(cursorItem);
        else itemInfo = "Click/Take: " + formatItem(currentItem);

        String context = "INV_CLICK";
        String locStr = "";
        if (clickedInv.getType() != InventoryType.PLAYER) {
            context = "CONTAINER_TRANSACTION";
            locStr = " | " + getContainerLoc(clickedInv);
        }
        plugin.getLogManager().log(player, context,
                String.format("[%s] Action: %s | %s | Slot: %d%s", invType, action, itemInfo, event.getSlot(), locStr));
    }

    private boolean isValidContainer(Inventory inv) {
        return inv.getType() != InventoryType.CRAFTING && inv.getType() != InventoryType.PLAYER;
    }

    private String getContainerLoc(Inventory inv) {
        Location loc = inv.getLocation();
        if (loc == null && inv.getHolder() instanceof BlockState bs) loc = bs.getLocation();
        else if (loc == null && inv.getHolder() instanceof org.bukkit.entity.Entity entity) loc = entity.getLocation();

        if (loc != null) {
            return String.format("Block: [w:%s x:%d y:%d z:%d]",
                    loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
        return "Block: [Unknown]";
    }

    private String formatItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "AIR";
        return item.getType().name() + (item.getAmount() > 1 ? " x" + item.getAmount() : "");
    }

    private String getInventoryContents(Inventory inv) {
        return "[" + Arrays.stream(inv.getContents())
                .map(this::formatItem)
                .collect(Collectors.joining(", ")) + "]";
    }
}
