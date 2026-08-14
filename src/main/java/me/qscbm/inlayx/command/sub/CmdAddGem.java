package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.stream.Collectors;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.socket.SocketResult;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 管理员命令: 镶嵌指定宝石
 */
public class CmdAddGem extends SubCommand {
    public CmdAddGem(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "addgem";
    }

    @Override
    public String permission() {
        return "inlayx.addgem";
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
        return "/gem addgem <宝石ID>";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        String gemId = args[0];
        ItemStack item = player.getInventory().getItemInMainHand();
        GemManager gm = plugin.getGemManager();

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "你必须手持一件装备!");
            return;
        }
        Gem gem = gm.getGem(gemId);
        if (gem == null) {
            player.sendMessage(ChatColor.RED + "找不到宝石: " + gemId);
            return;
        }

        SocketResult result = gm.addGem(item, gemId);
        switch (result.getStatus()) {
            case SUCCESS -> {
                player.getInventory().setItemInMainHand(result.getItem());
                player.sendMessage(ChatColor.GREEN + "已直接将宝石「" + gem.getName() + "」镶嵌到装备上!");
                plugin.getConfigManager().getSocketSuccessSound().play(player);
            }
            case NO_SOCKET -> player.sendMessage(ChatColor.RED + "该装备没有可用的宝石槽位!");
            case TYPE_MISMATCH ->
                player.sendMessage(ChatColor.RED + "该装备没有「" + gem.getType().getName() + "」类型的空槽位!");
            case OVER_CAP_LIMIT -> player.sendMessage(ChatColor.RED + "该装备的宝石槽位数量异常, 无法镶嵌!");
            default -> player.sendMessage(ChatColor.RED + "无法镶嵌, 请检查装备与宝石!");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getGemManager().getAllGems().stream().map(Gem::getId).collect(Collectors.toList());
        }
        return List.of();
    }
}
