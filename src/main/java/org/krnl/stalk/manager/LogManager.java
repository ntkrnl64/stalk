package org.krnl.stalk.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.krnl.stalk.Stalk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class LogManager {

    private final Stalk plugin;
    private final File logFolder;
    private final ExecutorService ioExecutor;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public LogManager(Stalk plugin) {
        this.plugin = plugin;
        this.logFolder = new File(plugin.getDataFolder(), "logs");
        if (!logFolder.exists()) logFolder.mkdirs();
        this.ioExecutor = Executors.newSingleThreadExecutor();
    }

    public void log(Player player, String action, String details) {
        Location loc = player.getLocation();
        String locationStr = String.format("w:%s x:%d y:%d z:%d",
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        // 格式: [Time] [Name] UUID | Action | Details | Loc
        String logLine = String.format("[%s] [%s] %s | %s | %s | Loc: [%s]",
                timeFormat.format(new Date()),
                player.getName(),
                player.getUniqueId(),
                action,
                details,
                locationStr
        );

        ioExecutor.submit(() -> writeToFile(logLine));
    }

    private void writeToFile(String line) {
        try {
            String fileName = dateFormat.format(new Date()) + ".log";
            File file = new File(logFolder, fileName);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法写入日志: " + e.getMessage());
        }
    }

    /**
     * 查询日志
     * @param ignoredActions 要忽略的动作集合
     * @param hideUuid 是否在输出中隐藏 UUID
     */
    public void searchLogs(CommandSender sender, String playerName, int limit, Set<String> ignoredActions, boolean hideUuid) {
        ioExecutor.submit(() -> {
            String todayFile = dateFormat.format(new Date()) + ".log";
            File file = new File(logFolder, todayFile);

            if (!file.exists()) {
                sender.sendMessage(Component.text("今日暂无日志文件。", NamedTextColor.RED));
                return;
            }

            sender.sendMessage(Component.text("查询结果: " + playerName +
                    (hideUuid ? " (无UUID)" : "") +
                    (ignoredActions.isEmpty() ? "" : " (忽略: " + ignoredActions + ")"), NamedTextColor.YELLOW));

            try (Stream<String> lines = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
                List<String> results = lines
                        .filter(line -> line.contains("[" + playerName + "]"))
                        .filter(line -> {
                            if (ignoredActions.isEmpty()) return true;
                            for (String ignore : ignoredActions) {
                                if (line.contains(" | " + ignore + " | ")) return false;
                            }
                            return true;
                        })
                        .map(line -> {
                            if (hideUuid) {
                                return line.replaceAll("\\s[0-9a-fA-F-]{36}\\s\\|", " |");
                            }
                            return line;
                        })
                        .toList();

                int size = results.size();
                int start = Math.max(0, size - limit);

                if (size == 0) {
                    sender.sendMessage(Component.text("未找到符合条件的记录。", NamedTextColor.RED));
                } else {
                    for (int i = start; i < size; i++) {
                        sender.sendMessage(Component.text(results.get(i), NamedTextColor.GRAY));
                    }
                }

            } catch (IOException e) {
                sender.sendMessage(Component.text("读取日志失败。", NamedTextColor.RED));
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        ioExecutor.shutdown();
    }
}
