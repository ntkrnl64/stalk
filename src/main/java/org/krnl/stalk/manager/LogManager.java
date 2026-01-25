package org.krnl.stalk.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.krnl.stalk.Stalk;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogManager {

    private final Stalk plugin;
    private final File dbFile;
    private final ExecutorService ioExecutor;
    private Connection connection;

    private final Set<String> disabledActions = new HashSet<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public LogManager(Stalk plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "stalk_data.db");
        this.ioExecutor = Executors.newSingleThreadExecutor();

        loadSettings();
        initDatabase();
    }

    public void loadSettings() {
        disabledActions.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("logging");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (!section.getBoolean(key)) {
                    disabledActions.add(key);
                }
            }
        }
        if (!disabledActions.isEmpty()) {
            plugin.getLogger().info("Disabled log actions: " + disabledActions);
        }
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
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_coords ON logs(world, x, y, z);");
                }
                plugin.getLogger().info("SQLite database initialized successfully.");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 默认日志方法：使用玩家当前位置
     */
    public void log(Player player, String action, String details) {
        log(player.getName(), player.getUniqueId().toString(), action, details, player.getLocation());
    }

    /**
     * 底层日志方法：指定具体位置 (用于记录方块破坏、放置等)
     */
    public void log(String playerName, String playerUUID, String action, String details, Location loc) {
        // 1. 检查配置是否禁用了该动作
        if (disabledActions.contains(action)) return;

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
                pstmt.setString(2, playerName);
                pstmt.setString(3, playerUUID);
                pstmt.setString(4, action);
                pstmt.setString(5, details);
                pstmt.setString(6, world);
                pstmt.setInt(7, x);
                pstmt.setInt(8, y);
                pstmt.setInt(9, z);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Log write error: " + e.getMessage());
            }
        });
    }

    /**
     * 查询指定玩家的日志
     */
    public void searchLogs(CommandSender sender, String playerName, int limit, Set<String> ignoredActions, boolean hideUuid) {
        ioExecutor.submit(() -> {
            if (connection == null) {
                sender.sendMessage(Component.text("Database not connected.", NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text("Searching: " + playerName + "...", NamedTextColor.YELLOW));

            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM logs WHERE player_name LIKE ?");
            if (!ignoredActions.isEmpty()) {
                sqlBuilder.append(" AND action NOT IN (");
                for (int i = 0; i < ignoredActions.size(); i++) {
                    sqlBuilder.append(i == 0 ? "?" : ", ?");
                }
                sqlBuilder.append(")");
            }
            sqlBuilder.append(" ORDER BY time_stamp DESC LIMIT ?");

            try (PreparedStatement pstmt = connection.prepareStatement(sqlBuilder.toString())) {
                int paramIndex = 1;
                pstmt.setString(paramIndex++, playerName + "%");
                for (String ignore : ignoredActions) {
                    pstmt.setString(paramIndex++, ignore);
                }
                pstmt.setInt(paramIndex, limit);

                try (ResultSet rs = pstmt.executeQuery()) {
                    printResultSet(sender, rs, hideUuid);
                }
            } catch (SQLException e) {
                sender.sendMessage(Component.text("Query error: " + e.getMessage(), NamedTextColor.RED));
                e.printStackTrace();
            }
        });
    }

    /**
     * 查询特定方块位置的历史
     */
    public void searchBlock(CommandSender sender, Location loc, int limit) {
        ioExecutor.submit(() -> {
            if (connection == null) {
                sender.sendMessage(Component.text("Database not connected.", NamedTextColor.RED));
                return;
            }
            String world = loc.getWorld().getName();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            sender.sendMessage(Component.text(String.format("Checking block history at [%s %d,%d,%d]...", world, x, y, z), NamedTextColor.YELLOW));

            String sql = "SELECT * FROM logs WHERE world = ? AND x = ? AND y = ? AND z = ? ORDER BY time_stamp DESC LIMIT ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, x);
                pstmt.setInt(3, y);
                pstmt.setInt(4, z);
                pstmt.setInt(5, limit);

                try (ResultSet rs = pstmt.executeQuery()) {
                    printResultSet(sender, rs, false);
                }
            } catch (SQLException e) {
                sender.sendMessage(Component.text("Query error: " + e.getMessage(), NamedTextColor.RED));
            }
        });
    }

    private void printResultSet(CommandSender sender, ResultSet rs, boolean hideUuid) throws SQLException {
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

            String timeStr = timeFormat.format(new Date(timestamp));
            String locStr = String.format("[w:%s x:%d y:%d z:%d]", w, x, y, z);
            String uuidPart = hideUuid ? "" : (" " + pUuid);

            String msg = String.format("[%s] [%s]%s | %s | %s | Loc: %s",
                    timeStr, pName, uuidPart, act, det, locStr);

            sender.sendMessage(Component.text(msg, NamedTextColor.GRAY));
            count++;
        }
        if (count == 0) {
            sender.sendMessage(Component.text("No records found.", NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text("Shown " + count + " records.", NamedTextColor.GREEN));
        }
    }

    public void shutdown() {
        ioExecutor.submit(() -> {
            try {
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (SQLException e) { e.printStackTrace(); }
        });
        ioExecutor.shutdown();
    }
}
