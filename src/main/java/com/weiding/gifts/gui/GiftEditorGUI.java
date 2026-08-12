package com.weiding.gifts.gui;

import com.weiding.gifts.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 新手礼包GUI编辑器 - 3x9箱子界面
 */
public class GiftEditorGUI implements Listener, InventoryHolder {

    private final Main plugin;
    private static final Component GUI_TITLE = LegacyComponentSerializer.legacySection()
            .deserialize("§8新手礼包编辑");
    private static final int GUI_SIZE = 27; // 3x9

    public GiftEditorGUI(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 为玩家打开礼包编辑GUI
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, GUI_SIZE, GUI_TITLE);

        // 加载已有物品到GUI
        List<ItemStack> items = plugin.getGiftStorage().getGiftItems();
        for (int i = 0; i < items.size() && i < GUI_SIZE; i++) {
            ItemStack item = items.get(i);
            if (item != null && !item.getType().isAir()) {
                inv.setItem(i, item.clone());
            }
        }

        player.openInventory(inv);
    }

    /**
     * 关闭GUI时保存物品到数据库
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GiftEditorGUI)) return;

        // 仅在数据库模式下保存
        if (!plugin.isDatabaseMode()) return;

        List<ItemStack> items = new ArrayList<>();
        ItemStack[] contents = event.getInventory().getContents();

        for (int i = 0; i < GUI_SIZE; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }

        // 异步保存以避免阻塞主线程
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getGiftStorage().saveGiftItems(items);
        });
    }

    /**
     * 阻止物品拖拽到GUI外
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GiftEditorGUI) {
            // 只允许在 GUI 内部拖拽
            for (int slot : event.getRawSlots()) {
                if (slot >= GUI_SIZE) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(this, GUI_SIZE, GUI_TITLE);
    }
}
