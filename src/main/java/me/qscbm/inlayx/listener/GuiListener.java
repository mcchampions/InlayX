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
        SocketResult result = gm.socketGem(equipment, gemItem);
        switch (result.getStatus()) {
            case SUCCESS -> {
                inv.setItem(EQUIP_SLOT, result.getItem());
                consumeOneGem(gemItem, inv);
                player.sendMessage(ChatColor.GREEN + "宝石镶嵌成功!");
                playSound(player, true);
            }
            case NOT_A_GEM -> player.sendMessage(ChatColor.RED + "这不是一个有效的宝石!");
            case UNKNOWN_GEM -> player.sendMessage(ChatColor.RED + "无法识别该宝石, 可能已被删除或配置已变更!");
            case NO_SOCKET ->
                player.sendMessage(ChatColor.RED + (gm.hasSocketLore(equipment) ? "该装备的宝石槽位已满!" : "该装备没有宝石槽位!"));
            case TYPE_MISMATCH -> {
                String typeName = gem == null ? "对应" : gem.getType().getName();
                player.sendMessage(ChatColor.RED + "该装备没有「" + typeName + "」类型的空槽位!");
            }
            case OVER_CAP_LIMIT -> player.sendMessage(ChatColor.RED + "该装备的宝石槽位数量异常, 无法镶嵌!");
            case FAILED -> handleSocketFailure(gm, gemItem, inv, player);
            default -> player.sendMessage(ChatColor.RED + "无法镶嵌, 请检查装备与宝石!");
        }
    }

    private void handleSocketFailure(GemManager gm, ItemStack gemItem, Inventory inv, Player player) {
        Gem gem = gm.getGem(gm.getGemId(gemItem));
        if (gem != null && gem.isDestroyOnFailure()) {
            consumeOneGem(gemItem, inv);
            player.sendMessage(ChatColor.RED + "镶嵌失败!宝石已碎裂.");
        } else {
            player.sendMessage(ChatColor.RED + "镶嵌失败!宝石完好无损, 可再次尝试.");
        }
        playSound(player, false);
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

    private void playSound(Player player, boolean success) {
        if (success) {
            plugin.getConfigManager().getSocketSuccessSound().play(player);
        } else {
            plugin.getConfigManager().getSocketFailureSound().play(player);
        }
    }

    private GemManager gm() {
        return plugin.getGemManager();
    }
}
