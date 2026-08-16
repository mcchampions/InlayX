package me.qscbm.inlayx.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.gem.GemType;
import me.qscbm.inlayx.socket.SocketSlot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 提取宝石 GUI 的构建与刷新.
 */
public class ExtractGuiFactory {
    public static final int EQUIP_SLOT = 19;
    public static final int PAGE_SIZE = 10;
    public static final int[] SLOT_SLOTS = {4, 5, 6, 7, 8, 13, 14, 15, 16, 17};
    public static final int PREV_BUTTON_SLOT = 22;
    public static final int NEXT_BUTTON_SLOT = 26;
    public static final int[] RESULT_SLOTS = {40, 41, 42, 43, 44, 49, 50, 51, 52, 53};

    private static final int[] BACKGROUND_SLOTS = {0, 1, 2, 9, 10, 11, 18, 20, 27, 28, 29, 36, 37, 38, 45, 46, 47};
    private static final int[] DIVIDER_COL_SLOTS = {3, 12, 21, 30, 39, 48};
    private static final int[] DIVIDER_ROW_SLOTS = {22, 23, 24, 25, 26, 31, 32, 33, 34, 35};

    private final InlayX plugin;
    private final GemManager gemManager;

    private final ItemStack backgroundItem;
    private final ItemStack dividerColItem;
    private final ItemStack dividerRowItem;
    private final ItemStack placeholderItem;
    private final ItemStack emptySlotBase;
    private final ItemStack noSlotItem;
    private final ItemMeta emptySlotBaseMeta;
    private final ItemStack prevButton;
    private final ItemStack nextButton;

    private final Map<String, ItemStack> gemItemCache = new ConcurrentHashMap<>();

    public ExtractGuiFactory(InlayX plugin, GemManager gemManager) {
        this.plugin = plugin;
        this.gemManager = gemManager;
        this.backgroundItem = guiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        this.dividerColItem = guiItem(Material.BLUE_STAINED_GLASS_PANE, " ");
        this.dividerRowItem = guiItem(Material.BROWN_STAINED_GLASS_PANE, " ");
        this.placeholderItem =
                guiItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "宝石槽位", ChatColor.GRAY + "请先将装备放入左侧格子");
        this.emptySlotBase =
                guiItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GRAY + "宝石槽位", ChatColor.GRAY + "（空）");
        this.noSlotItem = guiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.GRAY + "宝石槽位", ChatColor.RED + "暂无此槽位");
        this.emptySlotBaseMeta = emptySlotBase.getItemMeta();
        this.prevButton = guiItem(Material.ARROW, ChatColor.YELLOW + "上一页", ChatColor.GRAY + "点击查看上一页槽位");
        this.nextButton = guiItem(Material.ARROW, ChatColor.YELLOW + "下一页", ChatColor.GRAY + "点击查看下一页槽位");
    }

    public Inventory createGUI() {
        GemExtractHolder holder = new GemExtractHolder(0);
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_PURPLE + "宝石提取");
        fillDecoration(inv);
        inv.setItem(EQUIP_SLOT, null);
        refresh(inv, holder);
        return inv;
    }

    /**
     * 清空宝石物品缓存, 重载宝石配置后调用, 使 GUI 展示新的物品定义.
     */
    public void clearGemItemCache() {
        gemItemCache.clear();
    }

    /**
     * 按当前装备与页码刷新槽位区与翻页按钮.
     */
    public void refresh(Inventory inv, GemExtractHolder holder) {
        ItemStack equipment = inv.getItem(EQUIP_SLOT);
        List<SocketSlot> slots = equipment == null || equipment.getType() == Material.AIR
                ? List.of()
                : gemManager.getSocketSlots(equipment);
        int maxPage = Math.max(1, (slots.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (holder.getPage() >= maxPage) {
            holder.setPage(maxPage - 1);
        }
        int page = holder.getPage();
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            if (slots.isEmpty()) {
                inv.setItem(SLOT_SLOTS[i], placeholderItem);
            } else if (index < slots.size()) {
                SocketSlot slot = slots.get(index);
                if (slot.getGemId() == null) {
                    inv.setItem(SLOT_SLOTS[i], emptySlotItem(slot.getIndex() + 1, slot.getType()));
                } else {
                    Gem gem = gemManager.getGem(slot.getGemId());
                    if (gem == null) {
                        inv.setItem(SLOT_SLOTS[i], gemManager.getItemFactory().createUnknownGemItem(slot.getGemId()));
                    } else {
                        inv.setItem(
                                SLOT_SLOTS[i],
                                gemItemCache.computeIfAbsent(slot.getGemId(), gemManager::createGemItem));
                    }
                }
            } else {
                inv.setItem(SLOT_SLOTS[i], noSlotItem);
            }
        }
        inv.setItem(PREV_BUTTON_SLOT, page > 0 ? prevButton : dividerRowItem);
        inv.setItem(NEXT_BUTTON_SLOT, page < maxPage - 1 ? nextButton : dividerRowItem);
    }

    private ItemStack emptySlotItem(int slotNo, String typeId) {
        ItemMeta meta = emptySlotBaseMeta.clone();
        List<String> lore = new ArrayList<>(2);
        lore.add(ChatColor.GRAY + "第 " + slotNo + " 槽位");
        GemType type = plugin.getConfigManager().getGemType(typeId);
        lore.add(type == null ? ChatColor.GRAY + "未知类型（空）" : type.color() + type.name() + ChatColor.GRAY + "类型（空）");
        meta.setLore(lore);
        ItemStack item = emptySlotBase.clone();
        item.setItemMeta(meta);
        return item;
    }

    private void fillDecoration(Inventory inv) {
        for (int slot : BACKGROUND_SLOTS) {
            inv.setItem(slot, backgroundItem);
        }
        for (int slot : DIVIDER_COL_SLOTS) {
            inv.setItem(slot, dividerColItem);
        }
        for (int slot : DIVIDER_ROW_SLOTS) {
            inv.setItem(slot, dividerRowItem);
        }
    }

    private static ItemStack guiItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreLines.length > 0) {
            meta.setLore(new ArrayList<>(List.of(loreLines)));
        }
        item.setItemMeta(meta);
        return item;
    }
}
