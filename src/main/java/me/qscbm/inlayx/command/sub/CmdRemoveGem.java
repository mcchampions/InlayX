package me.qscbm.inlayx.command.sub;

import java.util.List;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.GemManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 管理员命令: 移除指定宝石
 */
public class CmdRemoveGem extends SubCommand {
    public CmdRemoveGem(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "removegem";
    }

    @Override
    public String permission() {
        return "inlayx.removegem";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected int minArgs() {
        return 1;
    }

    @Override
    protected String usage() {
        return "/gem removegem <宝石ID>";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        String gemId = args[0];
        ItemStack item = player.getInventory().getItemInMainHand();
        GemManager gm = plugin.getGemManager();

        if (item == null || item.getType() == Material.AIR || !gm.hasSocketedGems(item)) {
            player.sendMessage(ChatColor.RED + "你手中的装备没有镶嵌宝石!");
            return;
        }
        if (gm.getGem(gemId) == null) {
            player.sendMessage(ChatColor.RED + "找不到宝石: " + gemId);
            return;
        }
        if (!gm.getSocketedGems(item).contains(gemId)) {
            player.sendMessage(ChatColor.RED + "该装备上没有镶嵌「" + gemId + "」宝石!");
            return;
        }

        if (!gm.removeGem(item, gemId)) {
            player.sendMessage(ChatColor.RED + "宝石移除失败!");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "已从装备上移除宝石「" + gemId + "」");
        plugin.getConfigManager().getSocketSuccessSound().play(player);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            return plugin.getGemManager().getSocketedGems(item);
        }
        return List.of();
    }
}
