package me.qscbm.inlayx.listener;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.gui.GemSocketHolder;
import me.qscbm.inlayx.socket.SocketResult;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {
    private static final int EQUIP_SLOT = 13;
    private static final int GEM_SLOT = 31;
    private static final int CONFIRM_SLOT = 49;
    private static final int CANCEL_SLOT = 51;

    private final InlayX plugin;

    public GuiListener(InlayX plugin) {
        this.plugin = plugin;
    }

    private static boolean isSocketGUI(Inventory inv) {
        return inv.getHolder() instanceof GemSocketHolder;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isSocketGUI(event.getInventory())) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot >= 54) {
            return;
        }

        boolean isGuiSlot = slot == EQUIP_SLOT || slot == GEM_SLOT || slot == CONFIRM_SLOT || slot == CANCEL_SLOT;
        if (!isGuiSlot) {
            event.setCancelled(true);
            return;
        }

        if (slot == CONFIRM_SLOT) {
            handleConfirm(event, player);
        } else if (slot == CANCEL_SLOT) {
            event.setCancelled(true);
            player.closeInventory();
        }
    }

    private void handleConfirm(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        Inventory inv = event.getInventory();
        ItemStack equipment = inv.getItem(EQUIP_SLOT);
        ItemStack gemItem = inv.getItem(GEM_SLOT);

        if (equipment == null
                || gemItem == null
                || equipment.getType().isAir()
                || gemItem.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "请放入装备和宝石!");
            return;
        }

        GemManager gm = gm();
        Gem gem = gm.getGem(gm.getGemId(gemItem));
        SocketResult result = gm.socketGem(player, equipment, gemItem);
        if (result.isSuccess()) {
            inv.setItem(EQUIP_SLOT, result.getItem());
            consumeOneGem(gemItem, inv);
            plugin.getInteractionFeedback().sendSocketSuccess(player);
            return;
        }
        if (plugin.getInteractionFeedback().sendSocketFailure(player, result, gem, gm.hasSocketLore(equipment))) {
            consumeOneGem(gemItem, inv);
        }
    }

    private static void consumeOneGem(ItemStack gemItem, Inventory inv) {
        if (gemItem.getAmount() > 1) {
            gemItem.setAmount(gemItem.getAmount() - 1);
            inv.setItem(GEM_SLOT, gemItem);
        } else {
            inv.setItem(GEM_SLOT, null);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!isSocketGUI(event.getInventory())) {
            return;
        }

        Inventory inv = event.getInventory();
        returnItem(player, inv.getItem(EQUIP_SLOT));
        returnItem(player, inv.getItem(GEM_SLOT));
    }

    private static void returnItem(Player player, ItemStack item) {
        if (item == null) {
            return;
        }
        player.getInventory()
                .addItem(item)
                .forEach((idx, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private GemManager gm() {
        return plugin.getGemManager();
    }
}
