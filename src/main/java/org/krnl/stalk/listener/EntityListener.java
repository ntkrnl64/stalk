package org.krnl.stalk.listener;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.krnl.stalk.Stalk;

public class EntityListener implements Listener {

    private final Stalk plugin;

    public EntityListener(Stalk plugin) {
        this.plugin = plugin;
    }

    // 玩家死亡
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String msg = PlainTextComponentSerializer.plainText().serialize(event.deathMessage());

        String killerName = "Environment";
        if (player.getKiller() != null) {
            killerName = player.getKiller().getName();
        }

        plugin.getLogManager().log(player, "DEATH_PLAYER",
                "Message: " + msg + " | Killer: " + killerName + " | Loc: " + formatLoc(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player) return;

        if (entity.getKiller() != null) {
            Player killer = entity.getKiller();
            String entityName = entity.getName();
            if (entity.getCustomName() != null) {
                entityName = PlainTextComponentSerializer.plainText().serialize(entity.customName()) + " (" + entity.getType().name() + ")";
            }

            plugin.getLogManager().log(killer, "KILL_ENTITY",
                    "Killed: " + entityName + " | UUID: " + entity.getUniqueId());
        }
    }

    private String formatLoc(Player p) {
        return p.getLocation().getBlockX() + "," + p.getLocation().getBlockY() + "," + p.getLocation().getBlockZ();
    }
}
