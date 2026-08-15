package me.qscbm.inlayx.listener;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.socket.SocketResult;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    private final InlayX plugin;

    public PlayerListener(InlayX plugin) {
        this.plugin = plugin;
    }

    // 右键快捷镶嵌: 主手持宝石, 副手持装备
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isRightClickSocketEnabled()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().name().contains("RIGHT")) {
            return;
        }
        ItemStack gemItem = event.getItem();
        if (gemItem == null || !gm().isGem(gemItem)) {
            return;
        }

        GemManager gm = gm();
        Player player = event.getPlayer();
        ItemStack equipment = player.getInventory().getItemInOffHand();
        if (equipment == null || equipment.getType() == Material.AIR) {
            return;
        }
        if (!gm.hasSocketLore(equipment)) {
            return;
        }

        Gem gem = gm.getGem(gm.getGemId(gemItem));
        SocketResult result = gm.socketGem(player, equipment, gemItem);
        if (!result.isSuccess()) {
            if (plugin.getInteractionFeedback().sendSocketFailure(player, result, gem, gm.hasSocketLore(equipment))) {
                consumeMainHandGem(player, gemItem);
            }
            event.setCancelled(true);
            return;
        }
        player.getInventory().setItemInOffHand(result.getItem());
        consumeMainHandGem(player, gemItem);
        plugin.getInteractionFeedback().sendSocketSuccess(player);
        event.setCancelled(true);
    }

    // 背包拖拽快捷镶嵌
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfigManager().isDragSocketEnabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;

        ItemStack gemItem = event.getCursor();
        if (gemItem == null || !gm().isGem(gemItem)) {
            return;
        }

        GemManager gm = gm();
        ItemStack equipment = event.getCurrentItem();
        if (equipment == null || equipment.getType() == Material.AIR) {
            return;
        }
        if (!gm.hasSocketLore(equipment)) {
            return;
        }

        Gem gem = gm.getGem(gm.getGemId(gemItem));
        SocketResult result = gm.socketGem(player, equipment, gemItem);
        if (!result.isSuccess()) {
            if (plugin.getInteractionFeedback().sendSocketFailure(player, result, gem, gm.hasSocketLore(equipment))) {
                consumeCursorGem(event, gemItem);
            }
            event.setCancelled(true);
            return;
        }
        event.setCurrentItem(result.getItem());
        consumeCursorGem(event, gemItem);
        plugin.getInteractionFeedback().sendSocketSuccess(player);
        event.setCancelled(true);
    }

    private void consumeMainHandGem(Player player, ItemStack gemItem) {
        if (gemItem.getAmount() > 1) {
            gemItem.setAmount(gemItem.getAmount() - 1);
            player.getInventory().setItemInMainHand(gemItem);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private void consumeCursorGem(InventoryClickEvent event, ItemStack gemItem) {
        if (gemItem.getAmount() > 1) {
            gemItem.setAmount(gemItem.getAmount() - 1);
            event.setCursor(gemItem);
        } else {
            event.setCursor(null);
        }
    }

    private GemManager gm() {
        return this.plugin.getGemManager();
    }
}
