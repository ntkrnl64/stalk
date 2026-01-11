package org.krnl.stalk.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.krnl.stalk.Stalk;

public class SocialListener implements Listener {

    private final Stalk plugin;

    public SocialListener(Stalk plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.getLogManager().log(event.getPlayer(), "CHAT", message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        plugin.getLogManager().log(event.getPlayer(), "COMMAND", event.getMessage());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getLogManager().log(event.getPlayer(), "SESSION", "Joined the server IP: " +
                (event.getPlayer().getAddress() != null ? event.getPlayer().getAddress().toString() : "Unknown"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getLogManager().log(event.getPlayer(), "SESSION", "Left the server");
    }
}
