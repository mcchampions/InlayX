package me.qscbm.inlayx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.config.AttachmentHandlerConfigManager.EnchantmentConfig;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

class AttachmentHandlerConfigManagerTest extends InlayXTestBase {

    private Gem testGem() {
        return new Gem("attach_test", "测试", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
    }

    private Enchantment sharpness() {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
    }

    private ItemMeta freshSwordMeta() {
        return new ItemStack(Material.DIAMOND_SWORD).getItemMeta();
    }

    private Map<Enchantment, Integer> enchantment(int level) {
        Map<Enchantment, Integer> map = new HashMap<>();
        map.put(sharpness(), level);
        return map;
    }

    @Test
    void loadsDefaultBundledConfig() {
        AttachmentHandlerConfigManager mgr = plugin.getAttachmentHandlerConfigManager();
        EnchantmentConfig cfg = mgr.getEnchantmentConfig();
        assertNotNull(cfg);
        assertFalse(cfg.isIgnoreLevelRestriction());
    }

    @Test
    void socketAppliesLevelWhenEnchantmentAbsent() {
        AttachmentHandlerConfigManager mgr = plugin.getAttachmentHandlerConfigManager();
        ItemMeta result = mgr.applyEnchantmentConfigWhenSocket(freshSwordMeta(), enchantment(3), testGem());
        // non_exist -> {level} = 3
        assertEquals(3, result.getEnchantLevel(sharpness()));
    }

    @Test
    void socketKeepsOldLevelWhenGemLevelLower() {
        AttachmentHandlerConfigManager mgr = plugin.getAttachmentHandlerConfigManager();
        ItemMeta meta = freshSwordMeta();
        meta.addEnchant(sharpness(), 5, true);
        ItemMeta result = mgr.applyEnchantmentConfigWhenSocket(meta, enchantment(3), testGem());
        // lower -> {old_level} = 5
        assertEquals(5, result.getEnchantLevel(sharpness()));
    }

    @Test
    void socketUsesGemLevelWhenHigher() {
        AttachmentHandlerConfigManager mgr = plugin.getAttachmentHandlerConfigManager();
        ItemMeta meta = freshSwordMeta();
        meta.addEnchant(sharpness(), 2, true);
        ItemMeta result = mgr.applyEnchantmentConfigWhenSocket(meta, enchantment(4), testGem());
        // higher -> {level} = 4
        assertEquals(4, result.getEnchantLevel(sharpness()));
    }

    @Test
    void socketIncrementsWhenSameLevel() {
        AttachmentHandlerConfigManager mgr = plugin.getAttachmentHandlerConfigManager();
        ItemMeta meta = freshSwordMeta();
        meta.addEnchant(sharpness(), 3, true);
        ItemMeta result = mgr.applyEnchantmentConfigWhenSocket(meta, enchantment(3), testGem());
        // same -> {level} + 1 = 4
        assertEquals(4, result.getEnchantLevel(sharpness()));
    }

    @Test
    void emptyEnchantmentsLeavesMetaUnchanged() {
        AttachmentHandlerConfigManager mgr = plugin.getAttachmentHandlerConfigManager();
        ItemMeta meta = freshSwordMeta();
        ItemMeta result = mgr.applyEnchantmentConfigWhenSocket(meta, new HashMap<>(), testGem());
        assertSame(meta, result);
    }

    @Test
    void extractRemovesEnchantmentAfterSocket() {
        AttachmentHandlerConfigManager mgr = plugin.getAttachmentHandlerConfigManager();
        Gem gem = testGem();
        Map<Enchantment, Integer> enchantments = enchantment(3);
        ItemMeta meta = freshSwordMeta();
        meta = mgr.applyEnchantmentConfigWhenSocket(meta, enchantments, gem);
        assertEquals(3, meta.getEnchantLevel(sharpness()));
        // 默认 before_higher.after_same -> {old_level} = 0 -> 移除附魔
        meta = mgr.applyEnchantmentConfigWhenExtract(meta, enchantments, gem);
        assertEquals(0, meta.getEnchantLevel(sharpness()));
    }

    @Test
    void negativeFormulaResultClampedToZero() throws Exception {
        File file = new File(plugin.getDataFolder(), "attachment_handler.yml");
        Files.writeString(file.toPath(), """
                enchantments:
                  ignore_level_restriction: false
                  handler:
                    socket:
                      non_exist: "{level} - 100"
                """);
        plugin.getAttachmentHandlerConfigManager().load();

        AttachmentHandlerConfigManager mgr = plugin.getAttachmentHandlerConfigManager();
        ItemMeta result = mgr.applyEnchantmentConfigWhenSocket(freshSwordMeta(), enchantment(5), testGem());
        assertEquals(0, result.getEnchantLevel(sharpness()));
    }
}
