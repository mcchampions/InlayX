package me.qscbm.inlayx.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.qscbm.inlayx.InlayXTestBase;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class GemItemFactoryTest extends InlayXTestBase {

    @Test
    void identifiesAndCreatesGemItems() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack item = plugin.getGemManager().createGemItem("t1");
        assertNotNull(item);
        assertTrue(plugin.getGemManager().isGem(item));
        assertEquals("t1", plugin.getGemManager().getGemId(item));
        assertEquals("测试宝石", item.getItemMeta().getDisplayName());
        assertFalse(plugin.getGemManager().isGem(new ItemStack(Material.EMERALD)));
        assertNull(plugin.getGemManager().createGemItem("missing"));
    }
}
