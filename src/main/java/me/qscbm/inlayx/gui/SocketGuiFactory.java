package me.qscbm.inlayx.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.qscbm.inlayx.InlayX;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 镶嵌 GUI 的构建与装饰物品.
 */
public class SocketGuiFactory {
    private final InlayX plugin;

    private ItemStack equipSlotItem;
    private ItemStack gemSlotItem;
    private ItemStack confirmItem;
    private ItemStack cancelItem;
    private final ItemStack backgroundItem;

    public SocketGuiFactory(InlayX plugin) {
        this.plugin = plugin;
        this.backgroundItem = guiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        rebuildItems();
    }

    public void rebuildItems() {
        var i18n = plugin.getLanguageService();
        this.equipSlotItem = guiItem(
                Material.RED_STAINED_GLASS_PANE,
                i18n.get("gui.socket.place_equipment"),
                i18n.get("gui.socket.place_equipment_lore"));
        this.gemSlotItem = guiItem(
                Material.BLUE_STAINED_GLASS_PANE,
                i18n.get("gui.socket.place_gem"),
                i18n.get("gui.socket.place_gem_lore"));
        this.confirmItem = guiItem(
                Material.LIME_WOOL,
                i18n.get("gui.socket.confirm"),
                i18n.get("gui.socket.confirm_lore"),
                i18n.get("gui.socket.confirm_warning"));
        this.cancelItem = guiItem(Material.RED_WOOL, i18n.get("gui.socket.cancel"), i18n.get("gui.socket.cancel_lore"));
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
