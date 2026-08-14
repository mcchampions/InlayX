package me.qscbm.inlayx.listener;

import com.tcoded.folialib.FoliaLib;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.gui.ExtractGuiFactory;
import me.qscbm.inlayx.gui.GemExtractHolder;
import me.qscbm.inlayx.socket.ExtractResult;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ExtractGuiListener implements Listener {
    private final InlayX plugin;
    private final FoliaLib foliaLib;

    public ExtractGuiListener(InlayX plugin) {
        this.plugin = plugin;
        this.foliaLib = new FoliaLib(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof GemExtractHolder holder)) {
            return;
        }

        int raw = event.getRawSlot();
        if (raw >= 54) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }
        if (raw == ExtractGuiFactory.EQUIP_SLOT) {
            Inventory inv = event.getInventory();
            // 延迟刷新
            foliaLib.getScheduler().runAtEntity(player, task -> refresh(inv, holder));
            return;
        }
        if (isResultSlot(raw)) {
            return;
        }

        event.setCancelled(true);
        if (event.getAction() != InventoryAction.PICKUP_ALL) {
            return;
        }
        if (raw == ExtractGuiFactory.PREV_BUTTON_SLOT && holder.getPage() > 0) {
            holder.setPage(holder.getPage() - 1);
            refresh(event.getInventory(), holder);
        } else if (raw == ExtractGuiFactory.NEXT_BUTTON_SLOT && holder.getPage() < maxPage(event.getInventory()) - 1) {
            holder.setPage(holder.getPage() + 1);
            refresh(event.getInventory(), holder);
        } else if (isSlotArea(raw)) {
            handleExtract(player, event.getInventory(), holder, raw);
        }
    }

    private void handleExtract(Player player, Inventory inv, GemExtractHolder holder, int raw) {
        ItemStack clicked = inv.getItem(raw);
        GemManager gm = plugin.getGemManager();
        if (clicked == null || !gm.isGem(clicked)) {
            return;
        }
        String gemId = gm.getGemId(clicked);
        ItemStack equipment = inv.getItem(ExtractGuiFactory.EQUIP_SLOT);
        if (equipment == null || gemId == null) {
            return;
        }

        ExtractResult result = gm.extractGem(equipment, gemId);
        switch (result.getStatus()) {
            case SUCCESS -> {
                inv.setItem(ExtractGuiFactory.EQUIP_SLOT, equipment);
                giveToResultArea(player, inv, gm.createGemItem(gemId));
                player.sendMessage(ChatColor.GREEN + "宝石提取成功!");
                playSound(player, true);
            }
            case FAILED -> {
                inv.setItem(ExtractGuiFactory.EQUIP_SLOT, equipment);
                player.sendMessage(ChatColor.RED + "提取失败!宝石已碎裂.");
                playSound(player, false);
            }
            case NOT_FOUND -> player.sendMessage(ChatColor.RED + "该装备上没有镶嵌「" + gemId + "」宝石!");
        }
        refresh(inv, holder);
    }

    private void giveToResultArea(Player player, Inventory inv, ItemStack gemItem) {
        if (gemItem == null) {
            return;
        }
        for (int slot : ExtractGuiFactory.RESULT_SLOTS) {
            ItemStack current = inv.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                inv.setItem(slot, gemItem);
                return;
            }
        }
        player.getInventory()
                .addItem(gemItem)
                .forEach((idx, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private int maxPage(Inventory inv) {
        ItemStack equipment = inv.getItem(ExtractGuiFactory.EQUIP_SLOT);
        int size = equipment == null || equipment.getType() == Material.AIR
                ? 0
                : plugin.getGemManager().getSocketSlots(equipment).size();
        return Math.max(1, (size + ExtractGuiFactory.PAGE_SIZE - 1) / ExtractGuiFactory.PAGE_SIZE);
    }

    private void refresh(Inventory inv, GemExtractHolder holder) {
        plugin.getGemManager().getExtractGuiFactory().refresh(inv, holder);
    }

    private static boolean isResultSlot(int raw) {
        for (int slot : ExtractGuiFactory.RESULT_SLOTS) {
            if (slot == raw) {
                return true;
            }
        }
        return false;
    }

    private static int slotGridIndex(int raw) {
        for (int i = 0; i < ExtractGuiFactory.SLOT_SLOTS.length; i++) {
            if (ExtractGuiFactory.SLOT_SLOTS[i] == raw) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isSlotArea(int raw) {
        return slotGridIndex(raw) >= 0;
    }

    private void playSound(Player player, boolean success) {
        if (success) {
            plugin.getConfigManager().getExtractSuccessSound().play(player);
        } else {
            plugin.getConfigManager().getExtractFailureSound().play(player);
        }
    }

    public void cancelTasks() {
        foliaLib.getScheduler().cancelAllTasks();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GemExtractHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory inv = event.getInventory();
        returnItem(player, inv.getItem(ExtractGuiFactory.EQUIP_SLOT));
        for (int slot : ExtractGuiFactory.RESULT_SLOTS) {
            returnItem(player, inv.getItem(slot));
        }
    }

    private static void returnItem(Player player, ItemStack item) {
        if (item == null) {
            return;
        }
        player.getInventory()
                .addItem(item)
                .forEach((idx, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
