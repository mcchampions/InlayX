package me.qscbm.inlayx.listener;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.socket.SocketResult;
import org.bukkit.ChatColor;
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
        SocketResult result = gm.socketGem(equipment, gemItem);
        if (!result.isSuccess()) {
            handleSocketFailure(player, gemItem, true, gem, result);
            event.setCancelled(true);
            return;
        }
        player.getInventory().setItemInOffHand(result.getItem());
        consumeMainHandGem(player, gemItem);
        player.sendMessage(ChatColor.GREEN + "宝石镶嵌成功!");
        playSound(player, true);
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
        SocketResult result = gm.socketGem(equipment, gemItem);
        if (!result.isSuccess()) {
            handleSocketFailure(player, gemItem, false, gem, result);
            event.setCancelled(true);
            return;
        }
        event.setCurrentItem(result.getItem());
        consumeCursorGem(player, gemItem);
        player.sendMessage(ChatColor.GREEN + "宝石镶嵌成功!");
        playSound(player, true);
        event.setCancelled(true);
    }

    private void handleSocketFailure(Player player, ItemStack gemItem, boolean mainHand, Gem gem, SocketResult result) {
        switch (result.getStatus()) {
            case FAILED -> {
                if (gem != null && gem.isDestroyOnFailure()) {
                    if (mainHand) {
                        consumeMainHandGem(player, gemItem);
                    } else {
                        consumeCursorGem(player, gemItem);
                    }
                    player.sendMessage(ChatColor.RED + "镶嵌失败!宝石已碎裂.");
                } else {
                    player.sendMessage(ChatColor.RED + "镶嵌失败!宝石完好无损, 可再次尝试.");
                }
                playSound(player, false);
            }
            case NO_SOCKET -> player.sendMessage(ChatColor.RED + "该装备的宝石槽位已满!");
            case TYPE_MISMATCH -> {
                String typeName = gem == null ? "对应" : gem.getType().getName();
                player.sendMessage(ChatColor.RED + "该装备没有「" + typeName + "」类型的空槽位!");
            }
            case OVER_CAP_LIMIT -> player.sendMessage(ChatColor.RED + "该装备的宝石槽位数量异常, 无法镶嵌!");
            case UNKNOWN_GEM -> player.sendMessage(ChatColor.RED + "无法识别该宝石, 可能已被删除或配置已变更!");
            default -> player.sendMessage(ChatColor.RED + "无法镶嵌, 请检查装备与宝石!");
        }
    }

    private void consumeMainHandGem(Player player, ItemStack gemItem) {
        if (gemItem.getAmount() > 1) {
            gemItem.setAmount(gemItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private void consumeCursorGem(Player player, ItemStack gemItem) {
        if (gemItem.getAmount() > 1) {
            gemItem.setAmount(gemItem.getAmount() - 1);
        } else {
            player.setItemOnCursor(null);
        }
    }

    private void playSound(Player player, boolean success) {
        if (success) {
            plugin.getConfigManager().getSocketSuccessSound().play(player);
        } else {
            plugin.getConfigManager().getSocketFailureSound().play(player);
        }
    }

    private GemManager gm() {
        return this.plugin.getGemManager();
    }
}
