package org.krnl.stalk.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.krnl.stalk.Stalk;

public class MovementListener implements Listener {

    private final Stalk plugin;

    public MovementListener(Stalk plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        int fromChunkX = event.getFrom().getBlockX() >> 4;
        int fromChunkZ = event.getFrom().getBlockZ() >> 4;
        int toChunkX = event.getTo().getBlockX() >> 4;
        int toChunkZ = event.getTo().getBlockZ() >> 4;

        if (fromChunkX != toChunkX || fromChunkZ != toChunkZ) {
            plugin.getLogManager().log(
                    event.getPlayer(),
                    "CHUNK_MOVE",
                    String.format("From [%d,%d] To [%d,%d]", fromChunkX, fromChunkZ, toChunkX, toChunkZ)
            );
        }
    }
}
