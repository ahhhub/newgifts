package com.weiding.gifts.listener;

import com.weiding.gifts.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * 玩家加入事件监听器 - 自动发放新手礼包
 */
public class PlayerJoinListener implements Listener {

    private final Main plugin;

    public PlayerJoinListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 检查是否已领取过
        if (plugin.getConfigManager().hasPlayerReceived(player)) {
            return;
        }

        // 延迟执行，确保玩家数据完全加载
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            List<ItemStack> gifts = plugin.getGiftStorage().getGiftItems();
            if (gifts.isEmpty()) {
                // 没有配置礼包，也标记为已领取，避免重复检测
                plugin.getConfigManager().markPlayerReceived(player);
                return;
            }

            // 计算所需背包空间
            int requiredSlots = calculateRequiredSlots(gifts, player);
            int availableSlots = getAvailableSlots(player);

            if (availableSlots >= requiredSlots) {
                // 空间足够，正常发放
                player.getInventory().addItem(gifts.toArray(new ItemStack[0]));
                player.sendMessage(plugin.getConfigManager().getMessage("first-join-gift"));
                plugin.getConfigManager().markPlayerReceived(player);
                plugin.getLogger().info("已向玩家 " + player.getName() + " 发放新手礼包");
            } else {
                // 空间不足，提示玩家
                player.sendMessage(plugin.getConfigManager().getMessage("first-join-full"));
                plugin.getLogger().info("玩家 " + player.getName()
                        + " 背包空间不足，礼包未发放 (需要" + requiredSlots + "格，可用" + availableSlots + "格)");
                // 不标记已领取，让玩家可以使用 /gifts load 重新领取
            }
        }, 40L); // 延迟2秒
    }

    /**
     * 计算发放礼包所需的背包槽位
     */
    private int calculateRequiredSlots(List<ItemStack> gifts, Player player) {
        int slots = 0;
        for (ItemStack gift : gifts) {
            if (gift == null || gift.getType().isAir()) continue;

            // 检查是否可以与背包中现有物品堆叠
            int remaining = gift.getAmount();
            ItemStack[] contents = player.getInventory().getStorageContents();

            for (ItemStack existing : contents) {
                if (existing != null && existing.isSimilar(gift)) {
                    int space = existing.getMaxStackSize() - existing.getAmount();
                    if (space > 0) {
                        remaining -= space;
                        if (remaining <= 0) break;
                    }
                }
            }

            if (remaining > 0) {
                // 需要新槽位
                slots += (int) Math.ceil((double) remaining / gift.getMaxStackSize());
            }
        }
        return slots;
    }

    /**
     * 获取背包可用槽位
     */
    private int getAvailableSlots(Player player) {
        int empty = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                empty++;
            }
        }
        return empty;
    }
}
