package me.qscbm.inlayx.listener;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.gui.GemSocketHolder;
import me.qscbm.inlayx.gui.TalismanApplyHolder;
import me.qscbm.inlayx.socket.SocketResult;
import me.qscbm.inlayx.talisman.TalismanManager;
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

    private static boolean isTalismanGUI(Inventory inv) {
        return inv.getHolder() instanceof TalismanApplyHolder;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory inv = event.getInventory();
        boolean socketGui = isSocketGUI(inv);
        boolean talismanGui = isTalismanGUI(inv);
        if (!socketGui && !talismanGui) {
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
            if (socketGui) {
                handleConfirm(event, player);
            } else {
                handleTalismanConfirm(event, player);
            }
        } else if (slot == CANCEL_SLOT) {
            event.setCancelled(true);
            player.closeInventory();
        }
    }

    private void handleTalismanConfirm(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        Inventory inv = event.getInventory();
        ItemStack talismanItem = inv.getItem(EQUIP_SLOT);
        ItemStack gemItem = inv.getItem(GEM_SLOT);

        if (talismanItem == null
                || gemItem == null
                || talismanItem.getType().isAir()
                || gemItem.getType().isAir()) {
            player.sendMessage(plugin.getLanguageService().get("gui.talisman.need_both"));
            return;
        }
        if (!plugin.getTalismanManager().isTalisman(talismanItem)
                || !plugin.getGemManager().isGem(gemItem)) {
            player.sendMessage(plugin.getLanguageService().get("gui.talisman.need_both"));
            return;
        }

        String talismanId = plugin.getTalismanManager().getTalismanId(talismanItem);
        TalismanManager.ApplyStatus status = plugin.getTalismanManager().applyToGem(gemItem, talismanId);
        if (plugin.getInteractionFeedback().sendTalismanApplyFeedback(player, status)) {
            consumeOneTalisman(talismanItem, inv);
        }
        inv.setItem(GEM_SLOT, gemItem);
    }

    private static void consumeOneTalisman(ItemStack talismanItem, Inventory inv) {
        if (talismanItem.getAmount() > 1) {
            talismanItem.setAmount(talismanItem.getAmount() - 1);
            inv.setItem(EQUIP_SLOT, talismanItem);
        } else {
            inv.setItem(EQUIP_SLOT, null);
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
            player.sendMessage(plugin.getLanguageService().get("gui.socket.need_both"));
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
        } else {
            inv.setItem(GEM_SLOT, gemItem);
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
        Inventory inv = event.getInventory();
        if (!isSocketGUI(inv) && !isTalismanGUI(inv)) {
            return;
        }
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
