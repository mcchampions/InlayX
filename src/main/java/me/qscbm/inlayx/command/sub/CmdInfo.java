package me.qscbm.inlayx.command.sub;

import java.util.List;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.socket.SocketSlot;
import me.qscbm.inlayx.util.TextUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 获取装备或宝石信息
 */
public class CmdInfo extends SubCommand {
    public CmdInfo(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public String permission() {
        return "inlayx.info";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) {
            player.sendMessage(ChatColor.RED + "请手持宝石或已镶嵌宝石的装备!");
            return;
        }
        Material itemType = item.getType();
        if (itemType == Material.AIR) {
            player.sendMessage(ChatColor.RED + "请手持宝石或已镶嵌宝石的装备!");
            return;
        }
        GemManager gm = plugin.getGemManager();
        if (gm.isGem(item)) showGemInfo(player, item);
        else showEquipmentInfo(player, item, itemType);
    }

    private void showGemInfo(Player player, ItemStack item) {
        GemManager gm = plugin.getGemManager();
        Gem gem = gm.getGem(gm.getGemId(item));
        if (gem == null) {
            player.sendMessage(ChatColor.RED + "无效的宝石!");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "===== 宝石信息 =====");
        player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + gem.getId());
        player.sendMessage(ChatColor.YELLOW + "名称: " + ChatColor.WHITE + gem.getName());
        player.sendMessage(
                ChatColor.YELLOW + "类型: " + ChatColor.WHITE + gem.getType().getName());
        player.sendMessage(ChatColor.YELLOW + "等级: " + ChatColor.WHITE + gem.getLevel());
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "属性加成:");
        gem.getAttributeLore()
                .forEach(line ->
                        player.sendMessage(ChatColor.GREEN + "  " + TextUtils.translateAlternateColorCodes(line)));
    }

    private void showEquipmentInfo(Player player, ItemStack item, Material itemType) {
        player.sendMessage(ChatColor.GOLD + "===== 装备宝石信息 =====");
        player.sendMessage(ChatColor.YELLOW + "装备: " + ChatColor.WHITE + itemType.name());

        GemManager gm = plugin.getGemManager();
        List<SocketSlot> slots = gm.getSocketSlots(item);
        if (slots.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  该装备没有宝石槽位");
            return;
        }
        int slotNo = 0;
        boolean hasGem = false;
        for (SocketSlot slot : slots) {
            slotNo++;
            if (slot.getGemId() == null) {
                player.sendMessage(ChatColor.YELLOW + "  槽位" + slotNo + ": " + ChatColor.GRAY + "(空)");
                continue;
            }
            Gem gem = gm.getGem(slot.getGemId());
            if (gem != null) {
                hasGem = true;
                player.sendMessage(ChatColor.YELLOW + "  槽位" + slotNo + ": "
                        + ChatColor.GREEN + gem.getName()
                        + ChatColor.WHITE + " [" + gem.getType().getName() + " Lv." + gem.getLevel() + "]");
                gem.getAttributeLore()
                        .forEach(attr -> player.sendMessage(
                                ChatColor.GRAY + "    " + TextUtils.translateAlternateColorCodes(attr)));
            } else {
                player.sendMessage(ChatColor.YELLOW + "  槽位" + slotNo + ": " + ChatColor.RED + "未知宝石");
            }
        }
        if (!hasGem) {
            player.sendMessage(ChatColor.GRAY + "  该装备没有镶嵌宝石");
        }
    }
}
