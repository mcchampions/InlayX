package me.qscbm.inlayx.command.sub;

import java.util.List;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.socket.ExtractResult;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 提取宝石
 */
public class CmdExtract extends SubCommand {
    public CmdExtract(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "extract";
    }

    @Override
    public String permission() {
        return "inlayx.extract";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected String usage() {
        return "/gem extract [宝石ID]";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (args.length == 0) {
            player.openInventory(plugin.getGemManager().getExtractGuiFactory().createGUI());
            return;
        }
        String gemId = args[0];
        ItemStack item = player.getInventory().getItemInMainHand();
        GemManager gm = plugin.getGemManager();

        if (item == null || item.getType() == Material.AIR || !gm.hasSocketedGems(item)) {
            player.sendMessage(ChatColor.RED + "你手中的装备没有镶嵌宝石!");
            return;
        }
        if (gm.getGem(gemId) == null) {
            if (!gm.getSocketedGems(item).contains(gemId)) {
                player.sendMessage(ChatColor.RED + "该装备上没有镶嵌「" + gemId + "」宝石!");
                return;
            }
            if (!gm.removeGem(item, gemId)) {
                player.sendMessage(ChatColor.RED + "移除未知宝石失败!");
                return;
            }
            player.getInventory().setItemInMainHand(item);
            player.sendMessage(ChatColor.GREEN + "已移除未知宝石「" + gemId + "」(不会返还)");
            plugin.getInteractionFeedback().playExtractSound(player, true);
            return;
        }

        ExtractResult result = gm.extractGem(player, item, gemId);
        switch (result.getStatus()) {
            case SUCCESS -> {
                player.getInventory().setItemInMainHand(item);
                giveOrDrop(player, gm.createGemItem(result.getGemId()));
                player.sendMessage(ChatColor.GREEN + "宝石提取成功!");
                plugin.getInteractionFeedback().playExtractSound(player, true);
            }
            case FAILED -> {
                player.getInventory().setItemInMainHand(item);
                player.sendMessage(ChatColor.RED + "提取失败!宝石已碎裂.");
                plugin.getInteractionFeedback().playExtractSound(player, false);
            }
            case CANCELLED -> player.sendMessage(ChatColor.RED + "提取已被取消!");
            case NOT_FOUND -> player.sendMessage(ChatColor.RED + "该装备上没有镶嵌「" + gemId + "」宝石!");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            return plugin.getGemManager().getSocketedGems(item);
        }
        return List.of();
    }

    private static void giveOrDrop(Player player, ItemStack item) {
        if (item == null) return;
        if (player.getInventory().firstEmpty() != -1) player.getInventory().addItem(item);
        else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage(ChatColor.YELLOW + "你的物品栏已满, 物品已掉落在地上!");
        }
    }
}
