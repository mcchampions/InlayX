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
    public String description() {
        return i18n("command.info.description");
    }

    @Override
    protected String usage() {
        return i18n("command.info.usage");
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
            player.sendMessage(i18n("command.info.must_hold"));
            return;
        }
        Material itemType = item.getType();
        if (itemType == Material.AIR) {
            player.sendMessage(i18n("command.info.must_hold"));
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
            player.sendMessage(i18n("command.info.invalid_gem"));
            return;
        }
        player.sendMessage(i18n("command.info.gem_title"));
        player.sendMessage(i18n("command.info.id", gem.getId()));
        player.sendMessage(i18n("command.info.name", gem.getName()));
        player.sendMessage(i18n("command.info.type", gem.getType().name()));
        player.sendMessage(i18n("command.info.level", gem.getLevel()));
        player.sendMessage("");
        player.sendMessage(i18n("command.info.attributes"));
        gem.getAttributeLore()
                .forEach(line ->
                        player.sendMessage(ChatColor.GREEN + TextUtils.translateAlternateColorCodes("  " + line)));
    }

    private void showEquipmentInfo(Player player, ItemStack item, Material itemType) {
        player.sendMessage(i18n("command.info.equip_title"));
        player.sendMessage(i18n("command.info.equipment", itemType.name()));

        GemManager gm = plugin.getGemManager();
        List<SocketSlot> slots = gm.getSocketSlots(item);
        if (slots.isEmpty()) {
            player.sendMessage(i18n("command.info.no_socket_slot"));
            return;
        }
        int slotNo = 0;
        boolean hasGem = false;
        for (SocketSlot slot : slots) {
            slotNo++;
            if (slot.getGemId() == null) {
                player.sendMessage(i18n("command.info.slot_empty", slotNo));
                continue;
            }
            Gem gem = gm.getGem(slot.getGemId());
            if (gem != null) {
                hasGem = true;
                player.sendMessage(i18n(
                        "command.info.slot_filled",
                        slotNo,
                        gem.getName(),
                        gem.getType().name(),
                        gem.getLevel()));
                gem.getAttributeLore()
                        .forEach(attr -> player.sendMessage(
                                ChatColor.GRAY + TextUtils.translateAlternateColorCodes("    " + attr)));
            } else {
                player.sendMessage(i18n("command.info.slot_unknown", slotNo));
            }
        }
        if (!hasGem) {
            player.sendMessage(i18n("command.info.no_socketed_gem"));
        }
    }
}
