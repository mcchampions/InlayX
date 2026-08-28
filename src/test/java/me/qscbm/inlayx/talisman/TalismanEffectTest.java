package me.qscbm.inlayx.talisman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.qscbm.inlayx.InlayXTestBase;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

class TalismanEffectTest extends InlayXTestBase {
    private TalismanEffect effect() {
        return plugin.getTalismanManager().getEffect();
    }

    private ItemStack gemItem() {
        return new ItemStack(Material.EMERALD);
    }

    @Test
    void emptyStateReadsFromItemWithoutTag() {
        assertTrue(effect().read(gemItem()).isEmpty());
    }

    @Test
    void writeAndReadRoundTrip() {
        ItemStack item = gemItem();
        TalismanEffect.State state = new TalismanEffect.State(
                new TalismanEffect.BonusEntry("lucky_charm", 0.1, 5),
                new TalismanEffect.PreventEntry("socket_guard", 3));
        effect().write(item, state);

        TalismanEffect.State read = effect().read(item);
        assertEquals("lucky_charm", read.bonus().id());
        assertEquals(0.1, read.bonus().bonus());
        assertEquals(5, read.bonus().uses());
        assertEquals("socket_guard", read.prevent().id());
        assertEquals(3, read.prevent().uses());
    }

    @Test
    void writeEmptyRemovesTag() {
        ItemStack item = gemItem();
        TalismanEffect.State state =
                new TalismanEffect.State(new TalismanEffect.BonusEntry("lucky_charm", 0.1, 1), null);
        effect().write(item, state);
        assertTrue(effect().read(item).bonus() != null);

        effect().write(item, TalismanEffect.State.empty());
        assertTrue(effect().read(item).isEmpty());
    }

    @Test
    void consumeBonusUseDecrementsAndRemovesAtZero() {
        TalismanEffect.State state = new TalismanEffect.State(
                new TalismanEffect.BonusEntry("lucky_charm", 0.1, 2),
                new TalismanEffect.PreventEntry("socket_guard", 3));
        TalismanEffect.State afterOne = TalismanEffect.consumeBonusUse(state);
        assertEquals(1, afterOne.bonus().uses());
        assertEquals(3, afterOne.prevent().uses());
        TalismanEffect.State afterTwo = TalismanEffect.consumeBonusUse(afterOne);
        assertNull(afterTwo.bonus());
        assertEquals(3, afterTwo.prevent().uses());
    }

    @Test
    void consumePreventUseDecrementsAndRemovesAtZero() {
        TalismanEffect.State state = new TalismanEffect.State(
                new TalismanEffect.BonusEntry("lucky_charm", 0.1, 5),
                new TalismanEffect.PreventEntry("socket_guard", 1));
        TalismanEffect.State after = TalismanEffect.consumePreventUse(state);
        assertEquals(5, after.bonus().uses());
        assertNull(after.prevent());
    }

    @Test
    void consumeOnEmptyStateIsNoOp() {
        TalismanEffect.State empty = TalismanEffect.State.empty();
        assertEquals(empty, TalismanEffect.consumeBonusUse(empty));
        assertEquals(empty, TalismanEffect.consumePreventUse(empty));
    }

    @Test
    void corruptedPdcReadsAsEmpty() {
        ItemStack item = gemItem();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer()
                .set(effect().key(), org.bukkit.persistence.PersistentDataType.STRING, "{not json");
        item.setItemMeta(meta);
        assertTrue(effect().read(item).isEmpty());
    }
}
