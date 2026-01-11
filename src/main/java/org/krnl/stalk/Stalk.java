package org.krnl.stalk;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;
import org.krnl.stalk.command.StalkCommand;
import org.krnl.stalk.listener.ActivityListener;
import org.krnl.stalk.listener.EntityListener;
import org.krnl.stalk.listener.InventoryListener;
import org.krnl.stalk.listener.MovementListener;
import org.krnl.stalk.listener.SocialListener;
import org.krnl.stalk.manager.LogManager;

public class Stalk extends JavaPlugin {

    private LogManager logManager;

    @Override
    public void onEnable() {
        this.logManager = new LogManager(this);

        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new SocialListener(this), this);
        getServer().getPluginManager().registerEvents(new ActivityListener(this), this);

        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);

        PluginCommand command = getCommand("stalk");
        if (command != null) {
            StalkCommand stalkCmd = new StalkCommand(this);
            command.setExecutor(stalkCmd);
            command.setTabCompleter(stalkCmd);
        }

        getLogger().info("Stalk 插件已启动");
    }

    @Override
    public void onDisable() {
        if (logManager != null) {
            logManager.shutdown();
        }
    }

    public LogManager getLogManager() {
        return logManager;
    }
}
