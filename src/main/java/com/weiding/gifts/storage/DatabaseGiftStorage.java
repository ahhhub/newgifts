package com.weiding.gifts.storage;

import com.weiding.gifts.Main;
import com.weiding.gifts.database.DatabaseManager;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;

/**
 * 数据库存储实现
 * 使用 ItemStack.serializeAsBytes() + Base64 编码存储到数据库，完整保留 NBT 数据
 */
public class DatabaseGiftStorage implements GiftStorage {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    public DatabaseGiftStorage(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public List<ItemStack> getGiftItems() {
        List<ItemStack> items = new ArrayList<>();

        String sql = "SELECT item_data FROM gifts ORDER BY slot ASC LIMIT 27";

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String base64 = rs.getString("item_data");
                if (base64 != null && !base64.isEmpty()) {
                    try {
                        ItemStack item = deserializeFromBase64(base64);
                        if (item != null) {
                            items.add(item);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                "反序列化物品失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "从数据库读取礼包物品失败", e);
        }

        return items;
    }

    @Override
    public void saveGiftItems(List<ItemStack> items) {
        // 清空旧数据
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM gifts");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "清空礼包数据失败", e);
            return;
        }

        // 插入新数据
        String sql = "INSERT INTO gifts (slot, item_data) VALUES (?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int slot = 0;
            for (ItemStack item : items) {
                if (item != null && !item.getType().isAir()) {
                    String base64 = serializeToBase64(item);
                    ps.setInt(1, slot);
                    ps.setString(2, base64);
                    ps.addBatch();
                    slot++;
                }
                if (slot >= 27) break;
            }
            ps.executeBatch();
            plugin.getLogger().info("已保存 " + slot + " 件礼包物品到数据库");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "保存礼包物品到数据库失败", e);
        }
    }

    @Override
    public int getItemCount() {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM gifts")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "查询礼包数量失败", e);
        }
        return 0;
    }

    /**
     * 将 ItemStack 序列化为 Base64 字符串
     * 使用 Paper 新 API (serializeAsBytes)，完整保留 NBT 数据
     */
    public static String serializeToBase64(ItemStack item) {
        byte[] bytes = item.serializeAsBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 从 Base64 字符串反序列化为 ItemStack
     * 使用 Paper 新 API (deserializeBytes)
     */
    public static ItemStack deserializeFromBase64(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return ItemStack.deserializeBytes(bytes);
    }
}
