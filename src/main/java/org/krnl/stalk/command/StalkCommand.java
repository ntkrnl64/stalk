package org.krnl.stalk.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
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
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage:", NamedTextColor.RED));
            sender.sendMessage(Component.text("/stalk search <player> [limit] [--na actions] [--npu]", NamedTextColor.RED));
            sender.sendMessage(Component.text("/stalk block (Looks at target block)", NamedTextColor.RED));
            return true;
        }

        // 1. 方块历史查询
        if (args[0].equalsIgnoreCase("block")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Player only command.", NamedTextColor.RED));
                return true;
            }
            Block targetBlock = player.getTargetBlockExact(10);
            if (targetBlock == null) {
                sender.sendMessage(Component.text("Please look at a block.", NamedTextColor.RED));
                return true;
            }
            plugin.getLogManager().searchBlock(player, targetBlock.getLocation(), 10);
            return true;
        }

        // 2. 玩家日志查询
        if (args[0].equalsIgnoreCase("search")) {
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /stalk search <player> ...", NamedTextColor.RED));
                return true;
            }

            String targetName = args[1];
            int limit = 20;
            Set<String> ignoredActions = new HashSet<>();
            boolean hideUuid = false;

            for (int i = 2; i < args.length; i++) {
                String arg = args[i];
                if (arg.equalsIgnoreCase("--na") || arg.equalsIgnoreCase("--no-action")) {
                    if (i + 1 < args.length) {
                        String[] types = args[i + 1].toUpperCase().split(",");
                        Collections.addAll(ignoredActions, types);
                        i++;
                    }
                } else if (arg.equalsIgnoreCase("--npu") || arg.equalsIgnoreCase("--no-player-uuid")) {
                    hideUuid = true;
                } else {
                    try {
                        limit = Integer.parseInt(arg);
                    } catch (NumberFormatException ignored) { }
                }
            }

            if (limit > 500) limit = 500;
            plugin.getLogManager().searchLogs(sender, targetName, limit, ignoredActions, hideUuid);
            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("stalk.admin")) return List.of();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if ("search".startsWith(args[0].toLowerCase())) completions.add("search");
            if ("block".startsWith(args[0].toLowerCase())) completions.add("block");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("search")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        } else if (args.length > 2 && args[0].equalsIgnoreCase("search")) {
            String current = args[args.length - 1];
            String previous = args[args.length - 2];

            if (previous.equalsIgnoreCase("--na") || previous.equalsIgnoreCase("--no-action")) {
                String prefix = "";
                String lastPart = current.toUpperCase();
                if (current.contains(",")) {
                    int idx = current.lastIndexOf(",");
                    prefix = current.substring(0, idx + 1);
                    lastPart = current.substring(idx + 1).toUpperCase();
                }
                for (String type : ACTION_TYPES) {
                    if (type.startsWith(lastPart)) completions.add(prefix + type);
                }
            } else {
                if ("--na".startsWith(current.toLowerCase())) completions.add("--na");
                if ("--npu".startsWith(current.toLowerCase())) completions.add("--npu");
                if (current.isEmpty() || current.matches("\\d+")) {
                    completions.add("20");
                    completions.add("50");
                }
            }
        }
        return completions;
    }
}
