package me.qscbm.inlayx.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.qscbm.inlayx.InlayX;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 镶嵌 GUI 的构建与装饰物品.
 */
public class SocketGuiFactory {
    private final InlayX plugin;

    private final ItemStack equipSlotItem;
    private final ItemStack gemSlotItem;
    private final ItemStack confirmItem;
    private final ItemStack cancelItem;
    private final ItemStack backgroundItem;

    public SocketGuiFactory(InlayX plugin) {
        this.plugin = plugin;
        this.equipSlotItem =
                guiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.GOLD + "放置装备", ChatColor.GRAY + "请将需要镶嵌宝石的装备放在这里");
        this.gemSlotItem =
                guiItem(Material.BLUE_STAINED_GLASS_PANE, ChatColor.AQUA + "放置宝石", ChatColor.GRAY + "请将要镶嵌的宝石放在这里");
        this.confirmItem = guiItem(
                Material.LIME_WOOL,
                ChatColor.GREEN + "确认镶嵌",
                ChatColor.GRAY + "点击确认将宝石镶嵌到装备上",
                ChatColor.RED + "警告: 此操作不可逆!");
        this.cancelItem = guiItem(Material.RED_WOOL, ChatColor.RED + "取消", ChatColor.GRAY + "点击取消并关闭界面");
        this.backgroundItem = guiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    public Inventory createSocketGUI() {
        Inventory gui = Bukkit.createInventory(
                new GemSocketHolder(), 54, plugin.getConfigManager().getGuiTitle());
        fillBackground(gui);
        placeAt(gui, equipSlotItem, 2, 3, 4, 11, 15, 20, 21, 22);
        placeAt(gui, gemSlotItem, 29, 30, 32, 33, 38, 39, 40, 41, 42);
        gui.setItem(13, null);
        gui.setItem(31, null);
        gui.setItem(49, confirmItem);
        gui.setItem(51, cancelItem);
        return gui;
    }

    // ==================== GUI辅助 ====================

    private static ItemStack guiItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>(Arrays.asList(loreLines));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory gui) {
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, backgroundItem);
        }
    }

    private static void placeAt(Inventory gui, ItemStack item, int... slots) {
        for (int slot : slots) {
            gui.setItem(slot, item);
        }
    }
}
