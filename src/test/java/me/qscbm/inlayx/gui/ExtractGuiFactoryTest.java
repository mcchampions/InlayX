package me.qscbm.inlayx.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.gem.GemType;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class ExtractGuiFactoryTest extends InlayXTestBase {

    private Inventory newGui() {
        return plugin.getGemManager().getExtractGuiFactory().createGUI();
    }

    @Test
    void rendersEmptySlotsGreenAndMissingSlotsRed() {
        ExtractGuiFactory factory = plugin.getGemManager().getExtractGuiFactory();
        Inventory inv = newGui();
        GemExtractHolder holder = (GemExtractHolder) inv.getHolder();
        ItemStack sword = socketableSword(plugin.getConfigManager().getGemType("ATTACK"), 2);
        inv.setItem(ExtractGuiFactory.EQUIP_SLOT, sword);
        factory.refresh(inv, holder);
        assertEquals(
                Material.GREEN_STAINED_GLASS_PANE,
                inv.getItem(ExtractGuiFactory.SLOT_SLOTS[0]).getType());
        assertEquals(
                Material.GREEN_STAINED_GLASS_PANE,
                inv.getItem(ExtractGuiFactory.SLOT_SLOTS[1]).getType());
        assertEquals(
                Material.RED_STAINED_GLASS_PANE,
                inv.getItem(ExtractGuiFactory.SLOT_SLOTS[2]).getType());
        assertTrue(inv.getItem(ExtractGuiFactory.SLOT_SLOTS[2])
                .getItemMeta()
                .getLore()
                .getFirst()
                .contains("暂无此槽位"));
    }

    @Test
    void rendersPlaceholderWithoutEquipment() {
        Inventory inv = newGui();
        assertEquals(
                Material.GRAY_STAINED_GLASS_PANE,
                inv.getItem(ExtractGuiFactory.SLOT_SLOTS[0]).getType());
        assertTrue(inv.getItem(ExtractGuiFactory.SLOT_SLOTS[0])
                .getItemMeta()
                .getLore()
                .getFirst()
                .contains("请先将装备放入左侧格子"));
    }

    @Test
    void rendersSocketedGemAsGemItem() {
        registerGem("t1", "ATTACK", 1.0);
        ExtractGuiFactory factory = plugin.getGemManager().getExtractGuiFactory();
        Inventory inv = newGui();
        GemExtractHolder holder = (GemExtractHolder) inv.getHolder();
        GemType attack = plugin.getConfigManager().getGemType("ATTACK");
        ItemStack sword = socketableSword(attack, 2);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        inv.setItem(ExtractGuiFactory.EQUIP_SLOT, sword);
        factory.refresh(inv, holder);
        assertTrue(plugin.getGemManager().isGem(inv.getItem(ExtractGuiFactory.SLOT_SLOTS[0])));
        assertEquals(
                Material.GREEN_STAINED_GLASS_PANE,
                inv.getItem(ExtractGuiFactory.SLOT_SLOTS[1]).getType());
    }

    @Test
    void rendersUnknownSocketedGemAsUnknownMarker() {
        registerGem("t1", "ATTACK", 1.0);
        ExtractGuiFactory factory = plugin.getGemManager().getExtractGuiFactory();
        Inventory inv = newGui();
        GemExtractHolder holder = (GemExtractHolder) inv.getHolder();
        GemType attack = plugin.getConfigManager().getGemType("ATTACK");
        ItemStack sword = socketableSword(attack, 1);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        plugin.getGemManager().unregisterGem("t1");
        inv.setItem(ExtractGuiFactory.EQUIP_SLOT, sword);
        factory.refresh(inv, holder);

        ItemStack marker = inv.getItem(ExtractGuiFactory.SLOT_SLOTS[0]);
        assertNotNull(marker);
        assertTrue(plugin.getGemManager().isGem(marker));
        assertEquals("t1", plugin.getGemManager().getGemId(marker));
        assertNull(plugin.getGemManager().getGem("t1"));
    }

    @Test
    void reloadClearsGemItemCacheAndRendersNewDefinition() {
        plugin.getConfig().set("gems.cache_gem.name", "旧宝石");
        plugin.getConfig().set("gems.cache_gem.type", "ATTACK");
        plugin.getConfig().set("gems.cache_gem.level", 1);
        plugin.getConfig().set("gems.cache_gem.material", "EMERALD");
        plugin.getConfig().set("gems.cache_gem.socket.success_rate", 1.0);
        plugin.getConfig().set("gems.cache_gem.attributes", List.of("旧属性"));
        plugin.getGemManager().loadGems();

        ExtractGuiFactory factory = plugin.getGemManager().getExtractGuiFactory();
        Inventory inv = newGui();
        GemExtractHolder holder = (GemExtractHolder) inv.getHolder();
        GemType attack = plugin.getConfigManager().getGemType("ATTACK");
        ItemStack sword = socketableSword(attack, 1);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("cache_gem"));
        inv.setItem(ExtractGuiFactory.EQUIP_SLOT, sword);
        factory.refresh(inv, holder);
        String cachedName =
                inv.getItem(ExtractGuiFactory.SLOT_SLOTS[0]).getItemMeta().getDisplayName();
        assertTrue(cachedName.contains("旧宝石"));

        plugin.getConfig().set("gems.cache_gem.name", "新宝石");
        plugin.getGemManager().loadGems();
        factory.refresh(inv, holder);

        String refreshedName =
                inv.getItem(ExtractGuiFactory.SLOT_SLOTS[0]).getItemMeta().getDisplayName();
        assertTrue(refreshedName.contains("新宝石"));
        assertFalse(refreshedName.contains("旧宝石"));
    }
}
