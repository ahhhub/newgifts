package com.weiding.gifts.storage;

import com.weiding.gifts.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * YAML 配置文件存储实现
 * 从 gifts.yml 读取礼包物品
 */
public class YamlGiftStorage implements GiftStorage {

    private final Main plugin;
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    public YamlGiftStorage(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<ItemStack> getGiftItems() {
        List<ItemStack> items = new ArrayList<>();
        List<Map<String, Object>> rawItems = plugin.getConfigManager().getRawGiftItems();

        for (Map<String, Object> itemData : rawItems) {
            try {
                ItemStack item = deserializeItem(itemData);
                if (item != null && item.getType() != Material.AIR) {
                    items.add(item);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "解析物品配置失败: " + e.getMessage());
            }
        }

        // 截断到最多27个
        if (items.size() > 27) {
            plugin.getLogger().warning("gifts.yml 中配置的物品超过27个，已截断至27个");
            items = items.subList(0, 27);
        }

        return items;
    }

    @Override
    public void saveGiftItems(List<ItemStack> items) {
        // YAML 模式下不支持通过插件保存
        throw new UnsupportedOperationException("YAML模式下不支持通过GUI保存物品，请直接编辑 gifts.yml");
    }

    @Override
    public int getItemCount() {
        return getGiftItems().size();
    }

    /**
     * 从配置Map反序列化ItemStack
     * 支持: type, amount, name, lore, enchantments, flags, custom-model-data, unbreakable
     */
    @SuppressWarnings("unchecked")
    private ItemStack deserializeItem(Map<String, Object> data) {
        // 获取材质
        String typeStr = (String) data.get("type");
        if (typeStr == null) {
            plugin.getLogger().warning("物品配置缺少 type 字段");
            return null;
        }

        Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(typeStr.toLowerCase()));
        if (material == null) {
            plugin.getLogger().warning("未知的物品材质: " + typeStr);
            return null;
        }

        // 获取数量 (默认1)
        int amount = 1;
        Object amountObj = data.get("amount");
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).intValue();
        }
        amount = Math.max(1, Math.min(amount, material.getMaxStackSize()));

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 显示名称
        String name = (String) data.get("name");
        if (name != null && !name.isEmpty()) {
            meta.displayName(SERIALIZER.deserialize(colorize(name)));
        }

        // Lore 描述
        Object loreObj = data.get("lore");
        if (loreObj instanceof List) {
            List<Component> lore = new ArrayList<>();
            for (Object line : (List<?>) loreObj) {
                if (line != null) {
                    lore.add(SERIALIZER.deserialize(colorize(line.toString())));
                }
            }
            meta.lore(lore);
        }

        // 附魔
        Object enchObj = data.get("enchantments");
        if (enchObj instanceof Map) {
            Map<String, Object> enchants = (Map<String, Object>) enchObj;
            for (Map.Entry<String, Object> entry : enchants.entrySet()) {
                NamespacedKey key = NamespacedKey.minecraft(entry.getKey().toLowerCase());
                Enchantment enchantment = Registry.ENCHANTMENT.get(key);
                if (enchantment != null && entry.getValue() instanceof Number) {
                    int level = ((Number) entry.getValue()).intValue();
                    meta.addEnchant(enchantment, level, true);
                } else {
                    plugin.getLogger().warning("未知附魔: " + entry.getKey());
                }
            }
        }

        // ItemFlags
        Object flagsObj = data.get("flags");
        if (flagsObj instanceof List) {
            for (Object flagObj : (List<?>) flagsObj) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagObj.toString().toUpperCase());
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("未知的 ItemFlag: " + flagObj);
                }
            }
        }

        // 自定义模型数据
        Object cmdObj = data.get("custom-model-data");
        if (cmdObj instanceof Number) {
            meta.setCustomModelData(((Number) cmdObj).intValue());
        }

        // 不可破坏
        Object unbreakableObj = data.get("unbreakable");
        if (unbreakableObj instanceof Boolean) {
            meta.setUnbreakable((Boolean) unbreakableObj);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }
}
