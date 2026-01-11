package org.krnl.stalk.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.krnl.stalk.Stalk;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogManager {

    private final Stalk plugin;
    private final File dbFile;
    private final ExecutorService ioExecutor;
    private Connection connection;

    // 用于显示的格式化工具
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LogManager(Stalk plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "stalk_data.db");

        this.ioExecutor = Executors.newSingleThreadExecutor();

        initDatabase();
    }

    private void initDatabase() {
        ioExecutor.submit(() -> {
            try {
                Class.forName("org.sqlite.JDBC");

                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                connection = DriverManager.getConnection(url);

                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL;");
                    stmt.execute("PRAGMA synchronous=NORMAL;");

                    String sql = "CREATE TABLE IF NOT EXISTS logs (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "time_stamp LONG NOT NULL, " +
                            "player_name TEXT NOT NULL, " +
                            "player_uuid TEXT NOT NULL, " +
                            "action TEXT NOT NULL, " +
                            "details TEXT, " +
                            "world TEXT, " +
                            "x INTEGER, " +
                            "y INTEGER, " +
                            "z INTEGER" +
                            ");";
                    stmt.execute(sql);

                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_player ON logs(player_name);");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_time ON logs(time_stamp);");
                }

                plugin.getLogger().info("SQLite 数据库已连接并初始化。");

            } catch (Exception e) {
                plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 记录操作到数据库
     */
    public void log(Player player, String action, String details) {
        Location loc = player.getLocation();
        long now = System.currentTimeMillis();

        String world = loc.getWorld().getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        ioExecutor.submit(() -> {
            if (connection == null) return;

            String sql = "INSERT INTO logs (time_stamp, player_name, player_uuid, action, details, world, x, y, z) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setLong(1, now);
                pstmt.setString(2, player.getName());
                pstmt.setString(3, player.getUniqueId().toString());
                pstmt.setString(4, action);
                pstmt.setString(5, details);
                pstmt.setString(6, world);
                pstmt.setInt(7, x);
                pstmt.setInt(8, y);
                pstmt.setInt(9, z);

                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("写入日志失败: " + e.getMessage());
            }
        });
    }

    /**
     * 查询日志 (使用 SQL 过滤)
     */
    public void searchLogs(CommandSender sender, String playerName, int limit, Set<String> ignoredActions, boolean hideUuid) {
        ioExecutor.submit(() -> {
            if (connection == null) {
                sender.sendMessage(Component.text("数据库未连接。", NamedTextColor.RED));
                return;
            }

            sender.sendMessage(Component.text("正在查询数据库: " + playerName + "...", NamedTextColor.YELLOW));

            // 构建 SQL 查询
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM logs WHERE player_name LIKE ?");

            // 动态添加过滤条件
            if (!ignoredActions.isEmpty()) {
                sqlBuilder.append(" AND action NOT IN (");
                for (int i = 0; i < ignoredActions.size(); i++) {
                    sqlBuilder.append(i == 0 ? "?" : ", ?");
                }
                sqlBuilder.append(")");
            }

            // 排序和限制 (最新的在前)
            sqlBuilder.append(" ORDER BY time_stamp DESC LIMIT ?");

            try (PreparedStatement pstmt = connection.prepareStatement(sqlBuilder.toString())) {
                int paramIndex = 1;
                // 支持模糊搜索，如果输入完整名字也兼容
                pstmt.setString(paramIndex++, playerName + "%");

                // 填充忽略的动作
                for (String ignore : ignoredActions) {
                    pstmt.setString(paramIndex++, ignore);
                }

                pstmt.setInt(paramIndex, limit);

                try (ResultSet rs = pstmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        long timestamp = rs.getLong("time_stamp");
                        String pName = rs.getString("player_name");
                        String pUuid = rs.getString("player_uuid");
                        String act = rs.getString("action");
                        String det = rs.getString("details");
                        String w = rs.getString("world");
                        int x = rs.getInt("x");
                        int y = rs.getInt("y");
                        int z = rs.getInt("z");

                        // 格式化输出字符串，保持与原来文本日志相似的格式
                        String locStr = String.format("[w:%s x:%d y:%d z:%d]", w, x, y, z);
                        String timeStr = timeFormat.format(new Date(timestamp));

                        String uuidPart = hideUuid ? "" : (" " + pUuid);

                        // [12:00:00] [Steve] UUID | ACTION | Details | Loc: ...
                        String msg = String.format("[%s] [%s]%s | %s | %s | Loc: %s",
                                timeStr, pName, uuidPart, act, det, locStr);

                        sender.sendMessage(Component.text(msg, NamedTextColor.GRAY));
                        count++;
                    }

                    if (count == 0) {
                        sender.sendMessage(Component.text("未找到符合条件的记录。", NamedTextColor.RED));
                    } else {
                        sender.sendMessage(Component.text("已显示最近 " + count + " 条记录。", NamedTextColor.GREEN));
                    }
                }
            } catch (SQLException e) {
                sender.sendMessage(Component.text("查询出错: " + e.getMessage(), NamedTextColor.RED));
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        ioExecutor.submit(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        ioExecutor.shutdown();
    }
}
