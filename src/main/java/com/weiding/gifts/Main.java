package com.weiding.gifts;

import com.weiding.gifts.command.GiftsCommand;
import com.weiding.gifts.config.ConfigManager;
import com.weiding.gifts.database.DatabaseManager;
import com.weiding.gifts.gui.GiftEditorGUI;
import com.weiding.gifts.listener.PlayerJoinListener;
import com.weiding.gifts.storage.DatabaseGiftStorage;
import com.weiding.gifts.storage.GiftStorage;
import com.weiding.gifts.storage.YamlGiftStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * NewGifts 主类 - 新手礼包插件
 */
public final class Main extends JavaPlugin {

    private static Main instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private GiftStorage giftStorage;
    private GiftEditorGUI giftEditorGUI;

    @Override
    public void onEnable() {
        instance = this;

        // 保存默认配置
        saveDefaultConfig();
        saveResource("gifts.yml", false);

        // 初始化配置管理器
        configManager = new ConfigManager(this);

        // 初始化存储
        initStorage();

        // 初始化 GUI 编辑器
        giftEditorGUI = new GiftEditorGUI(this);

        // 注册命令
        GiftsCommand giftsCommand = new GiftsCommand(this);
        Objects.requireNonNull(getCommand("gifts")).setExecutor(giftsCommand);
        Objects.requireNonNull(getCommand("gifts")).setTabCompleter(giftsCommand);

        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(giftEditorGUI, this);

        getLogger().info("NewGifts 插件已启用！");
        getLogger().info("存储方式: " + (isDatabaseMode() ? "数据库 (" + getDatabaseType() + ")" : "YAML配置文件"));
    }

    @Override
    public void onDisable() {
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("NewGifts 插件已禁用！");
    }

    /**
     * 初始化/重新初始化存储
     */
    public void initStorage() {
        String storageType = configManager.getStorageType();

        if ("database".equalsIgnoreCase(storageType)) {
            // 关闭旧连接
            if (databaseManager != null) {
                databaseManager.close();
            }
            // 初始化数据库
            databaseManager = new DatabaseManager(this);
            databaseManager.init();
            giftStorage = new DatabaseGiftStorage(this, databaseManager);
        } else {
            // 使用 YAML 存储
            if (databaseManager != null) {
                databaseManager.close();
                databaseManager = null;
            }
            giftStorage = new YamlGiftStorage(this);
        }
    }

    /**
     * 重新加载所有配置和存储
     */
    public void reload() {
        configManager.reload();
        initStorage();
        getLogger().info("配置和存储已重新加载，当前物品数: " + giftStorage.getGiftItems().size());
    }

    /**
     * 重置所有玩家的领取记录
     */
    public void resetPlayerData() {
        configManager.resetPlayerData();
    }

    public static Main getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public GiftStorage getGiftStorage() {
        return giftStorage;
    }

    public GiftEditorGUI getGiftEditorGUI() {
        return giftEditorGUI;
    }

    public boolean isDatabaseMode() {
        return "database".equalsIgnoreCase(configManager.getStorageType());
    }

    public String getDatabaseType() {
        return configManager.getDatabaseType();
    }
}