package me.qscbm.inlayx.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.gem.GemType;
import me.qscbm.inlayx.gui.ExtractGuiFactory;
import me.qscbm.inlayx.gui.GemExtractHolder;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class ExtractGuiListenerTest extends InlayXTestBase {

    private PlayerMock player;
    private ExtractGuiListener listener;

    @BeforeEach
    void init() {
        player = server.addPlayer("Steve");
        listener = new ExtractGuiListener(plugin);
    }

    @Test
    void clickingUnknownGemRemovesItWithoutRoll() {
        setExtractRate(0.0);
        registerGem("t1", "ATTACK", 1.0);
        GemType attack = plugin.getConfigManager().getGemType("ATTACK");
        ItemStack sword = socketableSword(attack, 1);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        plugin.getGemManager().unregisterGem("t1");

        Inventory inv = plugin.getGemManager().getExtractGuiFactory().createGUI();
        inv.setItem(ExtractGuiFactory.EQUIP_SLOT, sword);
        plugin.getGemManager().getExtractGuiFactory().refresh(inv, (GemExtractHolder) inv.getHolder());

        ItemStack marker = inv.getItem(ExtractGuiFactory.SLOT_SLOTS[0]);
        assertTrue(plugin.getGemManager().isGem(marker));

        InventoryView view = player.openInventory(inv);
        InventoryClickEvent event = new InventoryClickEvent(
                view,
                InventoryType.SlotType.CONTAINER,
                ExtractGuiFactory.SLOT_SLOTS[0],
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL);
        listener.onClick(event);

        assertFalse(plugin.getGemManager()
                .getSocketedGems(inv.getItem(ExtractGuiFactory.EQUIP_SLOT))
                .contains("t1"));
    }
}
