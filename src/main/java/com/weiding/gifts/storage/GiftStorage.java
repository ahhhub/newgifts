package com.weiding.gifts.storage;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 礼包存储接口
 */
public interface GiftStorage {

    /**
     * 获取所有礼包物品
     * @return 物品列表 (0-26, 最多27个)
     */
    List<ItemStack> getGiftItems();

    /**
     * 保存礼包物品 (GUI模式)
     * @param items 物品列表
     */
    void saveGiftItems(List<ItemStack> items);

    /**
     * 获取礼包物品数量
     */
    int getItemCount();
}
