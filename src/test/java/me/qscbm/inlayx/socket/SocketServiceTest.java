package me.qscbm.inlayx.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.gem.GemType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

class SocketServiceTest extends InlayXTestBase {

    private GemType attackType() {
        return plugin.getConfigManager().getGemType("ATTACK");
    }

    private void stripHeader(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String header = plugin.getConfigManager().getSocketHeader();
        List<String> lore =
                meta.getLore().stream().filter(l -> !l.equals(header)).toList();
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    @Test
    void addSlotCreatesEmptySlotsInLore() {
        ItemStack sword = socketableSword(attackType(), 2);
        assertEquals(2, plugin.getGemManager().getSocketCount(sword));
        assertTrue(plugin.getGemManager().hasSocketLore(sword));
        assertFalse(plugin.getGemManager().hasSocketedGems(sword));
        assertTrue(
                sword.getItemMeta().getLore().contains(plugin.getConfigManager().getSocketHeader()));
    }

    @Test
    void addSlotClampsToMaxSockets() {
        ItemStack sword = socketableSword(attackType(), 100);
        assertEquals(
                plugin.getConfigManager().getMaxSockets(),
                plugin.getGemManager().getSocketCount(sword));
    }

    @Test
    void socketGemStoresGemAndSlotType() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(attackType(), 1);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        assertEquals(SocketResult.Status.SUCCESS, result.getStatus());
        assertTrue(plugin.getGemManager().getSocketedGems(sword).contains("t1"));
        assertEquals(
                "ATTACK", plugin.getGemManager().getSocketSlots(sword).get(0).getType());
        assertTrue(sword.getItemMeta().getLore().stream().anyMatch(l -> l.contains("测试宝石")));
    }

    @Test
    void socketGemRejectsTypeMismatch() {
        registerGem("t2", "DEFENSE", 1.0);
        ItemStack sword = socketableSword(attackType(), 1);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t2"));
        assertEquals(SocketResult.Status.TYPE_MISMATCH, result.getStatus());
        assertTrue(plugin.getGemManager().getSocketedGems(sword).isEmpty());
    }

    @Test
    void socketGemRejectsEquipmentWithoutSlots() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        assertEquals(SocketResult.Status.NO_SOCKET, result.getStatus());
    }

    @Test
    void socketGemRejectsUnknownGem() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(attackType(), 1);
        ItemStack fake = new ItemStack(Material.EMERALD);
        ItemMeta meta = fake.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "gem_id"), PersistentDataType.STRING, "ghost");
        fake.setItemMeta(meta);
        assertEquals(
                SocketResult.Status.UNKNOWN_GEM,
                plugin.getGemManager().socketGem(sword, fake).getStatus());
    }

    @Test
    void socketGemRejectsForgedLoreOverMaxSockets() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        String emptyLine = plugin.getConfigManager()
                .getSocketEmptyPattern()
                .replace("{gemTypeColor}", ChatColor.RED.toString())
                .replace("{gemTypeName}", "攻击")
                .replace("{gemTypeId}", "ATTACK");
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getConfigManager().getSocketHeader());
        for (int i = 0; i <= plugin.getConfigManager().getMaxSockets(); i++) {
            lore.add(emptyLine);
        }
        ItemMeta meta = sword.getItemMeta();
        meta.setLore(lore);
        sword.setItemMeta(meta);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        assertEquals(SocketResult.Status.OVER_CAP_LIMIT, result.getStatus());
    }

    @Test
    void socketGemRollFailureKeepsGemAndSlot() {
        registerGem("t3", "ATTACK", 0.0);
        ItemStack sword = socketableSword(attackType(), 1);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t3"));
        assertEquals(SocketResult.Status.FAILED, result.getStatus());
        assertTrue(plugin.getGemManager().getSocketedGems(sword).isEmpty());
        assertEquals(1, plugin.getGemManager().getSocketCount(sword));
    }

    @Test
    void extractSuccessRestoresSlotTypeAndAllowsReSocket() {
        setExtractRate(1.0);
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(attackType(), 1);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        ExtractResult result = plugin.getGemManager().extractGem(sword, "t1");
        assertEquals(ExtractResult.Status.SUCCESS, result.getStatus());
        List<SocketSlot> slots = plugin.getGemManager().getSocketSlots(sword);
        assertEquals(1, slots.size());
        assertNull(slots.get(0).getGemId());
        assertEquals("ATTACK", slots.get(0).getType());
        assertEquals(
                SocketResult.Status.SUCCESS,
                plugin.getGemManager()
                        .socketGem(sword, plugin.getGemManager().createGemItem("t1"))
                        .getStatus());
    }

    @Test
    void extractFailureDestroysGem() {
        setExtractRate(0.0);
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(attackType(), 1);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        ExtractResult result = plugin.getGemManager().extractGem(sword, "t1");
        assertEquals(ExtractResult.Status.FAILED, result.getStatus());
        assertTrue(plugin.getGemManager().getSocketedGems(sword).isEmpty());
    }

    @Test
    void removeSlotOnlyRemovesEmptySlots() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(attackType(), 2);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        plugin.getGemManager().removeSlotFromItem(sword, 2, attackType());
        assertEquals(1, plugin.getGemManager().getSocketCount(sword));
        assertTrue(plugin.getGemManager().getSocketedGems(sword).contains("t1"));
    }

    @Test
    void removeAllSlotsAlsoRemovesHeader() {
        ItemStack sword = socketableSword(attackType(), 1);
        plugin.getGemManager().removeSlotFromItem(sword, 1, attackType());
        assertEquals(0, plugin.getGemManager().getSocketCount(sword));
        List<String> lore = sword.getItemMeta().getLore();
        assertTrue(lore == null || !lore.contains(plugin.getConfigManager().getSocketHeader()));
    }

    @Test
    void headerMissingKeepsGemsVisibleAndRestoresOnWrite() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(attackType(), 2);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        stripHeader(sword);
        assertEquals(List.of("t1"), plugin.getGemManager().getSocketedGems(sword));
        plugin.getGemManager().addSlotToItem(sword, 1, attackType());
        assertEquals(2, plugin.getGemManager().getSocketCount(sword));
        assertTrue(
                sword.getItemMeta().getLore().contains(plugin.getConfigManager().getSocketHeader()));
    }

    @Test
    void headerMissingRestoresFreedSlotOnExtract() {
        setExtractRate(1.0);
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(attackType(), 2);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        stripHeader(sword);
        assertEquals(
                ExtractResult.Status.SUCCESS,
                plugin.getGemManager().extractGem(sword, "t1").getStatus());
        assertEquals(1, plugin.getGemManager().getSocketCount(sword));
        assertTrue(plugin.getGemManager().getSocketedGems(sword).isEmpty());
        assertEquals(
                "ATTACK", plugin.getGemManager().getSocketSlots(sword).get(0).getType());
        assertTrue(
                sword.getItemMeta().getLore().contains(plugin.getConfigManager().getSocketHeader()));
    }
}
