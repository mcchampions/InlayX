package me.qscbm.inlayx.talisman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class TalismanManagerTest extends InlayXTestBase {
    private void writeTalismansConfig(String content) {
        try {
            Files.writeString(plugin.getDataFolder().toPath().resolve("talismans.yml"), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        plugin.getTalismanManager().load();
    }

    private ItemStack createGemItem(String gemId) {
        return plugin.getGemManager().createGemItem(gemId);
    }

    private Gem registerGem(String id, String typeId) {
        GemType type = plugin.getConfigManager().getGemType(typeId);
        Gem gem = new Gem(id, "测试宝石", type, 1, Material.EMERALD);
        gem.setSocketSuccessRate(1.0);
        gem.setDisplayName("测试宝石");
        gem.setLore(List.of());
        gem.addAttributeLore("物理伤害 +1");
        plugin.getGemManager().registerGem(gem);
        return gem;
    }

    @Test
    void bundledTalismansAreLoaded() {
        assertTrue(plugin.getTalismanManager().getTalisman("socket_guard") != null);
        assertTrue(plugin.getTalismanManager().getTalisman("lucky_charm") != null);
        assertTrue(plugin.getTalismanManager().getTalisman("all_in_one") != null);
    }

    @Test
    void createTalismanItemCarriesPdcId() {
        ItemStack item = plugin.getTalismanManager().createTalismanItem("lucky_charm");
        assertNotNull(item);
        assertTrue(plugin.getTalismanManager().isTalisman(item));
        assertEquals("lucky_charm", plugin.getTalismanManager().getTalismanId(item));
    }

    @Test
    void createUnknownTalismanReturnsNull() {
        assertNull(plugin.getTalismanManager().createTalismanItem("ghost"));
    }

    @Test
    void parseAppliesItemAttributes() {
        writeTalismansConfig("""
                settings:
                  allow_different_effects: true
                  allow_same_restack: false
                test_charm:
                  name: "测试符"
                  material: DIAMOND
                  display_name: "&b测试符"
                  lore:
                    - "&7测试lore"
                  custom_model_data: 999
                  enchantments:
                    - "UNBREAKING:1"
                  item_flags:
                    - "HIDE_ENCHANTS"
                  attributes:
                    - "LUCK:5:0"
                  max_uses: 4
                  function:
                    success_rate_bonus: 0.2
                    prevent_destroy: true
                """);
        Talisman talisman = plugin.getTalismanManager().getTalisman("test_charm");
        assertNotNull(talisman);
        assertEquals(4, talisman.getMaxUses());
        assertEquals(0.2, talisman.getFunction().successRateBonus());
        assertTrue(talisman.getFunction().preventDestroy());

        ItemStack item = plugin.getTalismanManager().createTalismanItem("test_charm");
        assertNotNull(item);
        var meta = item.getItemMeta();
        assertEquals("§b测试符", meta.getDisplayName());
        assertTrue(meta.getLore().contains("§7测试lore"));
        assertEquals(999, meta.getCustomModelData());
        assertTrue(meta.hasEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING));
        assertTrue(meta.hasItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS));
    }

    @Test
    void applyWritesBonusAndPreventEntries() {
        registerGem("g1", "ATTACK");
        ItemStack gemItem = createGemItem("g1");
        TalismanManager.ApplyStatus status = plugin.getTalismanManager().applyToGem(gemItem, "all_in_one");
        assertEquals(TalismanManager.ApplyStatus.SUCCESS, status);

        TalismanEffect.State state = plugin.getTalismanManager().readEffects(gemItem);
        assertEquals("all_in_one", state.bonus().id());
        assertEquals(0.15, state.bonus().bonus());
        assertEquals(2, state.bonus().uses());
        assertEquals("all_in_one", state.prevent().id());
        assertEquals(2, state.prevent().uses());
    }

    @Test
    void duplicateApplicationIsRejectedByDefault() {
        registerGem("g1", "ATTACK");
        ItemStack gemItem = createGemItem("g1");
        assertEquals(
                TalismanManager.ApplyStatus.SUCCESS, plugin.getTalismanManager().applyToGem(gemItem, "lucky_charm"));
        assertEquals(
                TalismanManager.ApplyStatus.DUPLICATE,
                plugin.getTalismanManager().applyToGem(gemItem, "lucky_charm"));
        assertEquals(5, plugin.getTalismanManager().readEffects(gemItem).bonus().uses());
    }

    @Test
    void restackRefreshesUsesWhenAllowed() {
        writeTalismansConfig("""
                settings:
                  allow_different_effects: true
                  allow_same_restack: true
                test_luck:
                  name: "测试幸运符"
                  material: GOLD_NUGGET
                  max_uses: 3
                  function:
                    success_rate_bonus: 0.1
                    prevent_destroy: false
                """);
        registerGem("g1", "ATTACK");
        ItemStack gemItem = createGemItem("g1");
        assertEquals(
                TalismanManager.ApplyStatus.SUCCESS, plugin.getTalismanManager().applyToGem(gemItem, "test_luck"));
        assertEquals(
                TalismanManager.ApplyStatus.REFRESHED,
                plugin.getTalismanManager().applyToGem(gemItem, "test_luck"));
        assertEquals(3, plugin.getTalismanManager().readEffects(gemItem).bonus().uses());
    }

    @Test
    void differentEffectsCoexistAndSameFunctionSlotIsReplaced() {
        registerGem("g1", "ATTACK");
        ItemStack gemItem = createGemItem("g1");
        assertEquals(
                TalismanManager.ApplyStatus.SUCCESS, plugin.getTalismanManager().applyToGem(gemItem, "lucky_charm"));
        assertEquals(
                TalismanManager.ApplyStatus.SUCCESS, plugin.getTalismanManager().applyToGem(gemItem, "socket_guard"));
        TalismanEffect.State state = plugin.getTalismanManager().readEffects(gemItem);
        assertEquals("lucky_charm", state.bonus().id());
        assertEquals("socket_guard", state.prevent().id());
        assertEquals(
                TalismanManager.ApplyStatus.REPLACED,
                plugin.getTalismanManager().applyToGem(gemItem, "all_in_one"));
        state = plugin.getTalismanManager().readEffects(gemItem);
        assertEquals("all_in_one", state.bonus().id());
        assertEquals("all_in_one", state.prevent().id());
    }

    @Test
    void singleEffectModeReplacesEverything() {
        writeTalismansConfig("""
                settings:
                  allow_different_effects: false
                  allow_same_restack: false
                test_luck:
                  name: "测试幸运符"
                  material: GOLD_NUGGET
                  max_uses: 3
                  function:
                    success_rate_bonus: 0.1
                    prevent_destroy: false
                test_guard:
                  name: "测试守护符"
                  material: PAPER
                  max_uses: 2
                  function:
                    success_rate_bonus: 0.0
                    prevent_destroy: true
                """);
        registerGem("g1", "ATTACK");
        ItemStack gemItem = createGemItem("g1");
        assertEquals(
                TalismanManager.ApplyStatus.SUCCESS, plugin.getTalismanManager().applyToGem(gemItem, "test_luck"));
        assertEquals(
                TalismanManager.ApplyStatus.REPLACED,
                plugin.getTalismanManager().applyToGem(gemItem, "test_guard"));
        TalismanEffect.State state = plugin.getTalismanManager().readEffects(gemItem);
        assertNull(state.bonus());
        assertEquals("test_guard", state.prevent().id());
    }

    @Test
    void applyRejectsNonGemAndUnknownTalisman() {
        registerGem("g1", "ATTACK");
        assertEquals(
                TalismanManager.ApplyStatus.UNKNOWN_TALISMAN,
                plugin.getTalismanManager().applyToGem(createGemItem("g1"), "ghost"));
        assertEquals(
                TalismanManager.ApplyStatus.NOT_A_GEM,
                plugin.getTalismanManager().applyToGem(new ItemStack(Material.DIAMOND), "lucky_charm"));
    }

    @Test
    void talismanWithoutFunctionIsSkippedOnLoad() {
        writeTalismansConfig("""
                settings:
                  allow_different_effects: true
                  allow_same_restack: false
                empty_charm:
                  name: "空符"
                  material: PAPER
                  max_uses: 1
                  function:
                    success_rate_bonus: 0.0
                    prevent_destroy: false
                """);
        assertNull(plugin.getTalismanManager().getTalisman("empty_charm"));
    }

    @Test
    void createApplyGuiIsAvailable() {
        assertNotNull(plugin.getTalismanManager().createApplyGUI());
        assertFalse(plugin.getTalismanManager().getAllTalismans().isEmpty());
    }
}
