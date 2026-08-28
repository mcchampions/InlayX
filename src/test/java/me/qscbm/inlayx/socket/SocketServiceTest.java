package me.qscbm.inlayx.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.gem.Gem;
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

    @Test
    void addSlotCreatesEmptySlotsInLore() {
        ItemStack sword = socketableSword(attackType(), 2);
        assertEquals(2, plugin.getGemManager().getSocketCount(sword));
        assertTrue(plugin.getGemManager().hasSocketLore(sword));
        assertFalse(plugin.getGemManager().hasSocketedGems(sword));
        assertTrue(
                sword.getItemMeta().getLore().contains(plugin.getConfigManager().getSocketHeader()));
        assertTrue(
                sword.getItemMeta().getLore().contains(plugin.getConfigManager().getSocketFooter()));
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
                "ATTACK",
                plugin.getGemManager().getSocketSlots(sword).getFirst().getType());
        assertTrue(sword.getItemMeta().getLore().stream().anyMatch(l -> l.contains("测试宝石")));
    }

    @Test
    void socketGemTranslatesColorCodesInAttributeLore() {
        registerGem("t_color", "ATTACK", 1.0);
        plugin.getGemManager().getGems().get("t_color").addAttributeLore("&e测试属性");
        ItemStack sword = socketableSword(attackType(), 1);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t_color"));
        assertEquals(SocketResult.Status.SUCCESS, result.getStatus());
        List<String> lore = sword.getItemMeta().getLore();
        assertTrue(lore.stream().anyMatch(l -> l.contains("§e测试属性")));
        assertFalse(lore.stream().anyMatch(l -> l.contains("&e测试属性")));
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
    void socketGemRejectsWhitelistedEquipmentMaterial() {
        registerGem("t_material", "ATTACK", 1.0);
        Gem filtered = plugin.getGemManager().getGems().get("t_material");
        filtered.setMaterialFilterMode(Gem.MaterialFilterMode.WHITELIST);
        filtered.setFilterMaterials(
                Set.of(plugin.getItemGroupConfigManager().getItemGroupOrItem(Material.NETHERITE_SWORD.name())));
        ItemStack sword = socketableSword(attackType(), 1);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t_material"));
        assertEquals(SocketResult.Status.MATERIAL_MISMATCH, result.getStatus());
        assertTrue(plugin.getGemManager().getSocketedGems(sword).isEmpty());
    }

    @Test
    void socketGemAllowsWhitelistedEquipmentMaterial() {
        registerGem("t_material", "ATTACK", 1.0);
        Gem filtered = plugin.getGemManager().getGems().get("t_material");
        filtered.setMaterialFilterMode(Gem.MaterialFilterMode.WHITELIST);
        filtered.setFilterMaterials(
                Set.of(plugin.getItemGroupConfigManager().getItemGroupOrItem(Material.DIAMOND_SWORD.name())));
        ItemStack sword = socketableSword(attackType(), 1);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t_material"));
        assertEquals(SocketResult.Status.SUCCESS, result.getStatus());
        assertTrue(plugin.getGemManager().getSocketedGems(sword).contains("t_material"));
    }

    @Test
    void socketGemRejectsBlacklistedEquipmentMaterial() {
        registerGem("t_material", "ATTACK", 1.0);
        Gem filtered = plugin.getGemManager().getGems().get("t_material");
        filtered.setMaterialFilterMode(Gem.MaterialFilterMode.BLACKLIST);
        filtered.setFilterMaterials(
                Set.of(plugin.getItemGroupConfigManager().getItemGroupOrItem(Material.DIAMOND_SWORD.name())));
        ItemStack sword = socketableSword(attackType(), 1);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t_material"));
        assertEquals(SocketResult.Status.MATERIAL_MISMATCH, result.getStatus());
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
        lore.add(plugin.getConfigManager().getSocketFooter());
        ItemMeta meta = sword.getItemMeta();
        meta.setLore(lore);
        sword.setItemMeta(meta);
        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        assertEquals(SocketResult.Status.OVER_CAP_LIMIT, result.getStatus());
    }

    @Test
    void trailingLoreAfterFooterIsPreservedOnSocket() {
        registerGem("t_tail", "ATTACK", 1.0);
        ItemStack sword = socketableSword(attackType(), 1);
        String externalLore = "&7外部插件写入的文本";
        ItemMeta meta = sword.getItemMeta();
        List<String> lore = new ArrayList<>(meta.getLore());
        lore.add(externalLore);
        meta.setLore(lore);
        sword.setItemMeta(meta);

        SocketResult result =
                plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t_tail"));

        assertEquals(SocketResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, plugin.getGemManager().getSocketCount(sword));
        List<String> updatedLore = sword.getItemMeta().getLore();
        int footerIdx = updatedLore.indexOf(plugin.getConfigManager().getSocketFooter());
        assertTrue(footerIdx >= 0);
        assertTrue(updatedLore.subList(footerIdx + 1, updatedLore.size()).contains(externalLore));
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
        assertNull(slots.getFirst().getGemId());
        assertEquals("ATTACK", slots.getFirst().getType());
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
    void removeAllSlotsAlsoRemovesHeaderAndFooter() {
        ItemStack sword = socketableSword(attackType(), 1);
        plugin.getGemManager().removeSlotFromItem(sword, 1, attackType());
        assertEquals(0, plugin.getGemManager().getSocketCount(sword));
        List<String> lore = sword.getItemMeta().getLore();
        assertTrue(lore == null
                || (!lore.contains(plugin.getConfigManager().getSocketHeader())
                        && !lore.contains(plugin.getConfigManager().getSocketFooter())));
    }
}
