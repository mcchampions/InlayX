package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.gem.GemType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 管理员命令: 移除指定槽位
 */
public class CmdRemoveSlot extends SubCommand {
    public CmdRemoveSlot(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "removeslot";
    }

    @Override
    public String permission() {
        return "inlayx.removeslot";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected String usage() {
        return "/gem removeslot <类型> [数量]";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        ItemStack item = player.getInventory().getItemInMainHand();
        GemManager gm = plugin.getGemManager();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "你必须手持一件装备");
            return;
        }
        GemType type = args.length > 0
                ? plugin.getConfigManager().getGemType(args[0])
                : plugin.getConfigManager().getDefaultGemType();
        if (type == null) {
            player.sendMessage(ChatColor.RED
                    + (args.length > 0 ? "无效的宝石类型: " + args[0] : "未配置任何宝石类型, 请先在 config.yml 的 settings.gem_types 中配置"));
            return;
        }
        int count = argInt(args, 1, 1, plugin.getConfigManager().getMaxSockets());
        int before = gm.getSocketCount(item);
        player.getInventory().setItemInMainHand(gm.removeSlotFromItem(item, count, type));
        int removed = before - gm.getSocketCount(player.getInventory().getItemInMainHand());
        if (removed == 0) {
            player.sendMessage(ChatColor.RED + "该装备没有可移除的「" + type.getName() + "」空宝石槽位!(只有空槽位可以被移除)");
            return;
        }
        if (removed < count) {
            player.sendMessage(ChatColor.GREEN + "已为装备移除 " + removed + " 个 " + type.getName() + " 宝石槽位"
                    + ChatColor.YELLOW + " (仅空槽位被移除, 已镶嵌宝石的槽位不受影响)");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "已为装备移除 " + removed + " 个 " + type.getName() + " 宝石槽位");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getConfigManager().getGemTypes().keySet().stream().toList();
        }
        if (args.length == 2) {
            return IntStream.rangeClosed(1, plugin.getConfigManager().getMaxSockets())
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
