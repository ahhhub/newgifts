package com.weiding.gifts.config;

import com.weiding.gifts.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 配置管理器 - 管理 config.yml, gifts.yml, players.yml
 */
public class ConfigManager {

    private final Main plugin;

    private FileConfiguration config;
    private FileConfiguration giftsConfig;
    private FileConfiguration playersConfig;

    private File giftsFile;
    private File playersFile;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    /**
     * 加载所有配置文件
     */
    private void loadConfigs() {
        // config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // gifts.yml
        giftsFile = new File(plugin.getDataFolder(), "gifts.yml");
        if (!giftsFile.exists()) {
            plugin.saveResource("gifts.yml", false);
        }
        giftsConfig = YamlConfiguration.loadConfiguration(giftsFile);

        // players.yml - 记录已领取礼包的玩家
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 players.yml: " + e.getMessage());
            }
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    /**
     * 重新加载所有配置
     */
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        if (giftsFile.exists()) {
            giftsConfig = YamlConfiguration.loadConfiguration(giftsFile);
        }

        if (playersFile.exists()) {
            playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        }
    }

    // ========== config.yml 读取 ==========

    public String getStorageType() {
        return config.getString("storage.type", "yaml");
    }

    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "minecraft");
    }

    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "");
    }

    public String getSQLiteFile() {
        return config.getString("database.sqlite.file", "gifts.db");
    }

    // ========== 消息配置 ==========

    public String getMessage(String key) {
        String prefix = config.getString("messages.prefix", "&8[&6新手礼包&8] &r");
        String msg = config.getString("messages." + key, "");
        return colorize(prefix + msg);
    }

    public String getMessageRaw(String key) {
        return colorize(config.getString("messages." + key, ""));
    }

    // ========== gifts.yml 读取 ==========

    /**
     * 获取 gifts.yml 中所有礼包物品的配置
     * 返回有序 Map (按编号排序, 最多27个)
     */
    public Map<Integer, Map<String, Object>> getGiftItemsFromYaml() {
        Map<Integer, Map<String, Object>> items = new LinkedHashMap<>();

        if (giftsConfig.contains("gifts")) {
            Set<String> keys = Objects.requireNonNull(giftsConfig.getConfigurationSection("gifts")).getKeys(false);
            List<Integer> sortedKeys = new ArrayList<>();
            for (String key : keys) {
                try {
                    sortedKeys.add(Integer.parseInt(key));
                } catch (NumberFormatException ignored) {
                }
            }
            Collections.sort(sortedKeys);

            int count = 0;
            for (Integer key : sortedKeys) {
                if (count >= 27) break; // 最多27个物品
                org.bukkit.configuration.ConfigurationSection section =
                        giftsConfig.getConfigurationSection("gifts." + key);
                if (section != null) {
                    // getValues(true) 深度转换嵌套的 ConfigurationSection 为 Map
                    Map<String, Object> itemData = section.getValues(true);
                    items.put(count, itemData); // 使用0-based索引
                    count++;
                }
            }
        }

        return items;
    }

    /**
     * 从 gifts.yml 读取物品 (返回原始配置Map列表)
     */
    public List<Map<String, Object>> getRawGiftItems() {
        List<Map<String, Object>> items = new ArrayList<>();
        Map<Integer, Map<String, Object>> map = getGiftItemsFromYaml();
        for (int i = 0; i < map.size(); i++) {
            items.add(map.get(i));
        }
        return items;
    }

    // ========== players.yml 管理 ==========

    /**
     * 检查玩家是否已领取过礼包
     */
    public boolean hasPlayerReceived(Player player) {
        return playersConfig.getBoolean("players." + player.getUniqueId() + ".received", false);
    }

    /**
     * 标记玩家已领取礼包
     */
    public void markPlayerReceived(Player player) {
        playersConfig.set("players." + player.getUniqueId() + ".name", player.getName());
        playersConfig.set("players." + player.getUniqueId() + ".received", true);
        playersConfig.set("players." + player.getUniqueId() + ".time", System.currentTimeMillis());
        savePlayers();
    }

    /**
     * 重置所有玩家的领取记录
     */
    public void resetPlayerData() {
        playersConfig.set("players", null);
        savePlayers();
    }

    /**
     * 保存 players.yml
     */
    public void savePlayers() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 players.yml: " + e.getMessage());
        }
    }

    private String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }
}
