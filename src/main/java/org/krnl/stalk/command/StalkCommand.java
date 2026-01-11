package org.krnl.stalk.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.krnl.stalk.Stalk;

import java.util.*;
import java.util.stream.Collectors;

public class StalkCommand implements CommandExecutor, TabCompleter {

    private final Stalk plugin;
    private final List<String> ACTION_TYPES = List.of(
            "CHUNK_MOVE", "CHAT", "COMMAND", "SESSION",
            "BLOCK_BREAK", "BLOCK_PLACE", "INTERACT", "DROP_ITEM",
            "ATTACK", "DEATH", "DEATH_PLAYER", "KILL_ENTITY",
            "CONTAINER_OPEN", "CONTAINER_CLOSE", "CONTAINER_TRANSACTION",
            "PICKUP_ITEM", "INV_CLICK"
    );

    public StalkCommand(Stalk plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("stalk.admin")) {
            sender.sendMessage(Component.text("无权执行此命令。", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("search")) {
            sender.sendMessage(Component.text("用法: /stalk search <玩家名> [条数] [--na 动作] [--no-player-uuid]", NamedTextColor.RED));
            return true;
        }

        String targetName = args[1];
        int limit = 10;
        Set<String> ignoredActions = new HashSet<>();
        boolean hideUuid = false;

        // 参数解析循环
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];

            if (arg.equalsIgnoreCase("--na") || arg.equalsIgnoreCase("--no-action")) {
                if (i + 1 < args.length) {
                    String[] types = args[i + 1].toUpperCase().split(",");
                    Collections.addAll(ignoredActions, types);
                    i++;
                }
            }
            else if (arg.equalsIgnoreCase("--no-player-uuid") || arg.equalsIgnoreCase("--npu")) {
                hideUuid = true;
            }
            else {
                try {
                    limit = Integer.parseInt(arg);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (limit > 200) limit = 200;

        plugin.getLogManager().searchLogs(sender, targetName, limit, ignoredActions, hideUuid);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("stalk.admin")) return List.of();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if ("search".startsWith(args[0].toLowerCase())) completions.add("search");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("search")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length > 2) {
            String current = args[args.length - 1];
            String previous = args[args.length - 2];

            if (previous.equalsIgnoreCase("--na") || previous.equalsIgnoreCase("--no-action")) {
                String prefix = "";
                String lastPart = current.toUpperCase();
                if (current.contains(",")) {
                    int lastComma = current.lastIndexOf(",");
                    prefix = current.substring(0, lastComma + 1);
                    lastPart = current.substring(lastComma + 1).toUpperCase();
                }
                for (String type : ACTION_TYPES) {
                    if (type.startsWith(lastPart)) completions.add(prefix + type);
                }
            } else {
                if ("--na".startsWith(current.toLowerCase())) completions.add("--na");
                if ("--no-player-uuid".startsWith(current.toLowerCase())) completions.add("--no-player-uuid");

                if (isNumeric(current) || current.isEmpty()) {
                    completions.add("20");
                    completions.add("50");
                }
            }
        }

        return completions;
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
