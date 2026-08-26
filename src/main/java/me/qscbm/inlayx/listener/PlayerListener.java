package me.qscbm.inlayx.listener;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.socket.SocketResult;
import me.qscbm.inlayx.talisman.TalismanManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    private final InlayX plugin;

    public PlayerListener(InlayX plugin) {
        this.plugin = plugin;
    }

    // 右键快捷镶嵌: 主手持宝石, 副手持装备
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }
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
            } else {
                // 宝石保留(防碎裂保护或本就不碎裂): 写回宝石, 保护次数扣减才会生效
                player.getInventory().setItemInMainHand(gemItem);
            }
            event.setCancelled(true);
            return;
        }
        player.getInventory().setItemInOffHand(result.getItem());
        consumeMainHandGem(player, gemItem);
        plugin.getInteractionFeedback().sendSocketSuccess(player);
        event.setCancelled(true);
    }

    // 右键应用保护符: 主手持保护符, 副手持宝石
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTalismanInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().name().contains("RIGHT")) {
            return;
        }
        ItemStack talismanItem = event.getItem();
        if (talismanItem == null || !tm().isTalisman(talismanItem)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("inlayx.talisman.use")) {
            player.sendMessage(plugin.getLanguageService().get("common.no_permission"));
            event.setCancelled(true);
            return;
        }
        ItemStack gemItem = player.getInventory().getItemInOffHand();
        if (gemItem == null || !plugin.getGemManager().isGem(gemItem)) {
            return;
        }
        if (handleTalismanApply(player, talismanItem, gemItem)) {
            consumeMainHandTalisman(player, talismanItem);
        }
        player.getInventory().setItemInOffHand(gemItem);
        event.setCancelled(true);
    }

    // 背包拖拽应用保护符: 光标持保护符, 点击宝石
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClickApplyTalisman(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;
        if (!isPlayerInventoryClick(event)) return;

        ItemStack talismanItem = event.getCursor();
        if (talismanItem == null || !tm().isTalisman(talismanItem)) {
            return;
        }
        if (!player.hasPermission("inlayx.talisman.use")) {
            return;
        }
        ItemStack gemItem = event.getCurrentItem();
        if (gemItem == null || gemItem.getType() == Material.AIR) {
            return;
        }
        if (!plugin.getGemManager().isGem(gemItem)) {
            return;
        }
        if (handleTalismanApply(player, talismanItem, gemItem)) {
            consumeCursorTalisman(event, talismanItem);
        }
        event.setCurrentItem(gemItem);
        event.setCancelled(true);
    }

    /**
     * 把保护符应用到宝石上并反馈结果.
     *
     * @return true 表示保护符已消耗
     */
    private boolean handleTalismanApply(Player player, ItemStack talismanItem, ItemStack gemItem) {
        String talismanId = tm().getTalismanId(talismanItem);
        TalismanManager.ApplyStatus status = tm().applyToGem(gemItem, talismanId);
        return plugin.getInteractionFeedback().sendTalismanApplyFeedback(player, status);
    }

    private void consumeMainHandTalisman(Player player, ItemStack talismanItem) {
        if (talismanItem.getAmount() > 1) {
            talismanItem.setAmount(talismanItem.getAmount() - 1);
            player.getInventory().setItemInMainHand(talismanItem);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private void consumeCursorTalisman(InventoryClickEvent event, ItemStack talismanItem) {
        if (talismanItem.getAmount() > 1) {
            talismanItem.setAmount(talismanItem.getAmount() - 1);
            event.setCursor(talismanItem);
        } else {
            event.setCursor(null);
        }
    }

    // 背包拖拽快捷镶嵌
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfigManager().isDragSocketEnabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;
        if (!isPlayerInventoryClick(event)) return;

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
            } else {
                event.setCursor(gemItem);
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

    private static boolean isPlayerInventoryClick(InventoryClickEvent event) {
        Inventory clicked = event.getClickedInventory();
        return clicked != null
                && event.getRawSlot() >= event.getView().getTopInventory().getSize()
                && clicked.equals(event.getView().getBottomInventory());
    }

    private GemManager gm() {
        return this.plugin.getGemManager();
    }

    private TalismanManager tm() {
        return this.plugin.getTalismanManager();
    }
}
