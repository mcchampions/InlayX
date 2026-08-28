package me.qscbm.inlayx.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemType;
import me.qscbm.inlayx.talisman.TalismanEffect;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class SocketTalismanTest extends InlayXTestBase {
    private GemType attackType() {
        return plugin.getConfigManager().getGemType("ATTACK");
    }

    private void writeTalismansConfig(String content) {
        try {
            Files.writeString(plugin.getDataFolder().toPath().resolve("talismans.yml"), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        plugin.getTalismanManager().load();
    }

    private void installTestTalismans() {
        writeTalismansConfig("""
                settings:
                  allow_different_effects: true
                  allow_same_restack: false
                test_luck:
                  name: "测试幸运符"
                  material: GOLD_NUGGET
                  max_uses: 3
                  function:
                    success_rate_bonus: 1.0
                    prevent_destroy: false
                test_guard:
                  name: "测试守护符"
                  material: PAPER
                  max_uses: 2
                  function:
                    success_rate_bonus: 0.0
                    prevent_destroy: true
                test_guard1:
                  name: "测试守护符一号"
                  material: PAPER
                  max_uses: 1
                  function:
                    success_rate_bonus: 0.0
                    prevent_destroy: true
                """);
    }

    private Gem registerGem(String id, String typeId, double successRate, boolean destroyOnFailure) {
        GemType type = plugin.getConfigManager().getGemType(typeId);
        Gem gem = new Gem(id, "测试宝石", type, 1, Material.EMERALD);
        gem.setSocketSuccessRate(successRate);
        gem.setDestroyOnFailure(destroyOnFailure);
        gem.setDisplayName("测试宝石");
        gem.setLore(List.of());
        gem.addAttributeLore("物理伤害 +1");
        plugin.getGemManager().registerGem(gem);
        return gem;
    }

    private ItemStack gemWithTalisman(String gemId, String talismanId) {
        ItemStack gemItem = plugin.getGemManager().createGemItem(gemId);
        plugin.getTalismanManager().applyToGem(gemItem, talismanId);
        return gemItem;
    }

    @Test
    void bonusBoostsSuccessRateAndConsumesOneUse() {
        installTestTalismans();
        registerGem("g1", "ATTACK", 0.0, false);
        ItemStack sword = socketableSword(attackType(), 1);
        ItemStack gemItem = gemWithTalisman("g1", "test_luck");
        assertEquals(3, plugin.getTalismanManager().readEffects(gemItem).bonus().uses());

        SocketResult result = plugin.getGemManager().socketGem(sword, gemItem);
        assertEquals(SocketResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, plugin.getTalismanManager().readEffects(gemItem).bonus().uses());
    }

    @Test
    void bonusNotConsumedWhenBaseRateAlreadyHundred() {
        installTestTalismans();
        registerGem("g1", "ATTACK", 1.0, false);
        ItemStack sword = socketableSword(attackType(), 1);
        ItemStack gemItem = gemWithTalisman("g1", "test_luck");

        SocketResult result = plugin.getGemManager().socketGem(sword, gemItem);
        assertEquals(SocketResult.Status.SUCCESS, result.getStatus());
        assertEquals(3, plugin.getTalismanManager().readEffects(gemItem).bonus().uses());
    }

    @Test
    void preventProtectsGemAndConsumesOneUse() {
        installTestTalismans();
        registerGem("g1", "ATTACK", 0.0, true);
        ItemStack sword = socketableSword(attackType(), 1);
        ItemStack gemItem = gemWithTalisman("g1", "test_guard");
        assertEquals(
                2, plugin.getTalismanManager().readEffects(gemItem).prevent().uses());

        SocketResult result = plugin.getGemManager().socketGem(sword, gemItem);
        assertEquals(SocketResult.Status.FAILED, result.getStatus());
        assertTrue(result.isTalismanProtected());
        assertEquals(1, result.getTalismanPreventUsesRemaining());
        assertEquals(
                1, plugin.getTalismanManager().readEffects(gemItem).prevent().uses());
    }

    @Test
    void preventNotConsumedWhenGemWouldNotBreakAnyway() {
        installTestTalismans();
        registerGem("g1", "ATTACK", 0.0, false);
        ItemStack sword = socketableSword(attackType(), 1);
        ItemStack gemItem = gemWithTalisman("g1", "test_guard");
        assertEquals(
                2, plugin.getTalismanManager().readEffects(gemItem).prevent().uses());

        SocketResult result = plugin.getGemManager().socketGem(sword, gemItem);
        assertEquals(SocketResult.Status.FAILED, result.getStatus());
        assertFalse(result.isTalismanProtected());
        assertEquals(
                2, plugin.getTalismanManager().readEffects(gemItem).prevent().uses());
    }

    @Test
    void preventIsRemovedWhenUsesExhausted() {
        installTestTalismans();
        registerGem("g1", "ATTACK", 0.0, true);
        ItemStack sword = socketableSword(attackType(), 1);
        ItemStack gemItem = gemWithTalisman("g1", "test_guard1");
        assertEquals(
                1, plugin.getTalismanManager().readEffects(gemItem).prevent().uses());

        SocketResult first = plugin.getGemManager().socketGem(sword, gemItem);
        assertEquals(SocketResult.Status.FAILED, first.getStatus());
        assertTrue(first.isTalismanProtected());
        assertEquals(0, first.getTalismanPreventUsesRemaining());
        assertNull(plugin.getTalismanManager().readEffects(gemItem).prevent());

        SocketResult second = plugin.getGemManager().socketGem(sword, gemItem);
        assertEquals(SocketResult.Status.FAILED, second.getStatus());
        assertFalse(second.isTalismanProtected());
    }

    @Test
    void failureWithoutTalismanIsNotProtected() {
        installTestTalismans();
        registerGem("g1", "ATTACK", 0.0, true);
        ItemStack sword = socketableSword(attackType(), 1);
        ItemStack gemItem = plugin.getGemManager().createGemItem("g1");

        SocketResult result = plugin.getGemManager().socketGem(sword, gemItem);
        assertEquals(SocketResult.Status.FAILED, result.getStatus());
        assertFalse(result.isTalismanProtected());
    }

    @Test
    void bonusAndPreventConsumeIndependently() {
        writeTalismansConfig("""
                settings:
                  allow_different_effects: true
                  allow_same_restack: false
                test_combo:
                  name: "测试全能符"
                  material: NETHER_STAR
                  max_uses: 3
                  function:
                    success_rate_bonus: 1.0
                    prevent_destroy: true
                """);
        registerGem("g1", "ATTACK", 0.0, true);
        ItemStack sword = socketableSword(attackType(), 1);
        ItemStack gemItem = gemWithTalisman("g1", "test_combo");
        assertEquals(3, plugin.getTalismanManager().readEffects(gemItem).bonus().uses());
        assertEquals(
                3, plugin.getTalismanManager().readEffects(gemItem).prevent().uses());

        SocketResult result = plugin.getGemManager().socketGem(sword, gemItem);
        assertEquals(SocketResult.Status.SUCCESS, result.getStatus());
        TalismanEffect.State state = plugin.getTalismanManager().readEffects(gemItem);
        assertEquals(2, state.bonus().uses());
        assertEquals(3, state.prevent().uses());
    }
}
