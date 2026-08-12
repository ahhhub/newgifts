package com.weiding.gifts.database;

import com.weiding.gifts.Main;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库管理器 - 支持 SQLite 和 MySQL
 */
public class DatabaseManager {

    private final Main plugin;
    private Connection connection;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化数据库连接和表结构
     */
    public void init() {
        String dbType = plugin.getConfigManager().getDatabaseType().toLowerCase();

        try {
            if ("mysql".equals(dbType)) {
                initMySQL();
            } else {
                initSQLite();
            }

            // 创建表
            createTables();

            plugin.getLogger().info("数据库连接成功: " + dbType);
        } catch (Exception e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initSQLite() throws SQLException {
        // 由于 shade relocation，需要显式注册驱动
        try {
            Class.forName("com.weiding.gifts.libs.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite 驱动加载失败", e);
        }

        String fileName = plugin.getConfigManager().getSQLiteFile();
        File dbFile = new File(plugin.getDataFolder(), fileName);

        // 确保父目录存在
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
    }

    private void initMySQL() throws SQLException {
        // 由于 shade relocation，需要显式注册驱动
        try {
            Class.forName("com.weiding.gifts.libs.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL 驱动加载失败", e);
        }

        String host = plugin.getConfigManager().getMySQLHost();
        int port = plugin.getConfigManager().getMySQLPort();
        String database = plugin.getConfigManager().getMySQLDatabase();
        String username = plugin.getConfigManager().getMySQLUsername();
        String password = plugin.getConfigManager().getMySQLPassword();

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true";

        connection = DriverManager.getConnection(url, username, password);
    }

    private void createTables() throws SQLException {
        String dbType = plugin.getConfigManager().getDatabaseType().toLowerCase();

        String sql;
        if ("mysql".equals(dbType)) {
            sql = "CREATE TABLE IF NOT EXISTS gifts ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "slot INT NOT NULL, "
                    + "item_data LONGTEXT NOT NULL"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS gifts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "slot INTEGER NOT NULL, "
                    + "item_data TEXT NOT NULL"
                    + ")";
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            init();
        }
        return connection;
    }

    /**
     * 测试数据库连接是否有效
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("数据库连接已关闭");
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }
}
