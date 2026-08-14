package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 给予宝石
 */
public class CmdGive extends SubCommand {
    public CmdGive(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "give";
    }

    @Override
    public String permission() {
        return "inlayx.give";
    }

    @Override
    protected int minArgs() {
        return 2;
    }

    @Override
    protected String usage() {
        return "/gem give <玩家名> <宝石ID> [数量]";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        String playerName = args[0];
        String gemId = args[1];
        int amount = argInt(args, 2, 1, 64);
        Player target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "找不到玩家: " + playerName);
            return;
        }
        GemManager gm = plugin.getGemManager();
        Gem gem = gm.getGem(gemId);
        if (gem == null) {
            sender.sendMessage(ChatColor.RED + "找不到宝石: " + gemId);
            return;
        }
        ItemStack item = gm.createGemItem(gemId);
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "创建宝石失败: " + gemId);
            return;
        }
        item.setAmount(amount);
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
        int rest = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
        int given = amount - rest;
        if (given > 0) {
            sender.sendMessage(ChatColor.GREEN + "已将 " + given + " 个 " + gem.getName() + ChatColor.GREEN + " 给予 "
                    + target.getName());
            target.sendMessage(ChatColor.GREEN + "你收到了 " + given + " 个 " + gem.getName() + "!");
        }
        if (rest > 0) {
            if (plugin.getConfigManager().isDropGemOnFullInventory()) {
                for (ItemStack left : leftover.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), left);
                }
                target.sendMessage(
                        ChatColor.YELLOW + "你的背包已满, " + rest + " 个 " + gem.getName() + ChatColor.YELLOW + " 已掉落在你旁边!");
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " 的背包已满, " + rest + " 个 " + gem.getName()
                        + ChatColor.YELLOW + " 已掉落在其旁边");
            } else {
                target.sendMessage(
                        ChatColor.YELLOW + "你的背包已满, " + rest + " 个 " + gem.getName() + ChatColor.YELLOW + " 未能发放!");
                sender.sendMessage(ChatColor.YELLOW + target.getName() + " 的背包已满, " + rest + " 个 " + gem.getName()
                        + ChatColor.YELLOW + " 未发放");
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return plugin.getGemManager().getAllGems().stream().map(Gem::getId).collect(Collectors.toList());
        }
        if (args.length == 3) {
            return IntStream.rangeClosed(1, 64).mapToObj(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }
}
