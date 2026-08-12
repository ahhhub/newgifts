package com.weiding.gifts.command;

import com.weiding.gifts.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /gifts 命令处理器
 */
public class GiftsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public GiftsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gui":
                return handleGui(sender);
            case "reload":
                return handleReload(sender, args);
            case "load":
                return handleLoad(sender);
            default:
                sender.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                return true;
        }
    }

    /**
     * /gifts gui - 打开礼包编辑GUI
     */
    private boolean handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (!player.hasPermission("gifts.command.gui")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // 非数据库模式下拦截
        if (!plugin.isDatabaseMode()) {
            player.sendMessage(plugin.getConfigManager().getMessage("gui-unavailable"));
            return true;
        }

        plugin.getGiftEditorGUI().open(player);
        return true;
    }

    /**
     * /gifts reload [time] - 重载配置或重置玩家时间
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length >= 2 && "time".equalsIgnoreCase(args[1])) {
            // /gifts reload time
            if (!sender.hasPermission("gifts.command.reload.time")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            plugin.resetPlayerData();
            sender.sendMessage(plugin.getConfigManager().getMessage("reload-time-success"));
            plugin.getLogger().info(sender.getName() + " 重置了所有玩家的游玩时间");
        } else {
            // /gifts reload
            if (!sender.hasPermission("gifts.command.reload")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            plugin.reload();
            int count = plugin.getGiftStorage().getItemCount();
            String msg = plugin.getConfigManager().getMessageRaw("reload-success")
                    .replace("{count}", String.valueOf(count));
            sender.sendMessage(plugin.getConfigManager().getMessageRaw("prefix") + msg);
        }
        return true;
    }

    /**
     * /gifts load - 玩家手动领取礼包（无视背包空间）
     */
    private boolean handleLoad(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        if (!player.hasPermission("gifts.command.load")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        List<ItemStack> gifts = plugin.getGiftStorage().getGiftItems();
        if (gifts.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("gift-no-items"));
            return true;
        }

        // 无视背包空间，直接发放
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(
                gifts.toArray(new ItemStack[0]));

        if (leftover.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("gift-load-success"));
        } else {
            // 放不下的掉落在玩家位置
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            player.sendMessage(plugin.getConfigManager().getMessage("gift-load-fail"));
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== 新手礼包插件 =====");
        if (sender.hasPermission("gifts.command.gui")) {
            sender.sendMessage("§e/gifts gui §7- 打开礼包编辑GUI (仅数据库模式)");
        }
        if (sender.hasPermission("gifts.command.reload")) {
            sender.sendMessage("§e/gifts reload §7- 重载配置和礼包数据");
        }
        if (sender.hasPermission("gifts.command.reload.time")) {
            sender.sendMessage("§e/gifts reload time §7- 重置所有玩家的游玩时间");
        }
        sender.sendMessage("§e/gifts load §7- 领取新手礼包 (无视背包空间)");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("gifts.command.gui")) completions.add("gui");
            if (sender.hasPermission("gifts.command.reload")) completions.add("reload");
            completions.add("load");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && "reload".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission("gifts.command.reload.time")) {
                return Collections.singletonList("time");
            }
        }

        return Collections.emptyList();
    }
}
