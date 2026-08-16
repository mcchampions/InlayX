package me.qscbm.inlayx.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import me.qscbm.inlayx.InlayXTestBase;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.junit.jupiter.api.Test;

class GemLoaderTest extends InlayXTestBase {

    private void registerFromText(String id, String text) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(text));
        ConfigurationSection section = yaml.getConfigurationSection(id);
        plugin.getGemManager().getLoader().parseAndRegister(id, section);
    }

    @Test
    void parsesGemFromConfigText() {
        registerFromText("attack_test", """
                        attack_test:
                          name: "血锋尖晶"
                          type: ATTACK
                          level: 4
                          material: REDSTONE
                          attributes:
                            - "物理伤害 +8"
                            - "暴击几率 +2.5%"
                          socket:
                            success_rate: 0.95
                            destroy_on_failure: false
                          drop:
                              normal:
                                chance: 0.02
                              mythic:
                                chance: 0.02
                                min_mob_level: 3
                                per_level_rate: 0.001
                        """);
        Gem gem = plugin.getGemManager().getGem("attack_test");
        assertNotNull(gem);
        assertEquals("血锋尖晶", gem.getName());
        assertEquals("ATTACK", gem.getType().id());
        assertEquals(4, gem.getLevel());
        assertEquals(Material.REDSTONE, gem.getMaterial());
        assertEquals(List.of("物理伤害 +8", "暴击几率 +2.5%"), gem.getAttributeLore());
        assertEquals(0.95, gem.getSocketSuccessRate());
        assertFalse(gem.isDestroyOnFailure());
        assertEquals(Set.of("normal", "mythic"), gem.getDropSources());
        assertEquals(0.02, ((Number) gem.getDropSourceSettings().get("normal").get("chance")).doubleValue());
        assertEquals(0.02, ((Number) gem.getDropSourceSettings().get("mythic").get("chance")).doubleValue());
        assertEquals(3, ((Number) gem.getDropSourceSettings().get("mythic").get("min_mob_level")).intValue());
        assertEquals(0.001, ((Number) gem.getDropSourceSettings().get("mythic").get("per_level_rate")).doubleValue());
        assertEquals(ChatColor.RED + "血锋尖晶 ★★★★", gem.getDisplayName());
        assertTrue(gem.getLore().stream().anyMatch(l -> l.contains("物理伤害 +8")));
        assertTrue(gem.getLore().stream().anyMatch(l -> l.contains("成功率")));
    }

    @Test
    void appliesDisplayOverrides() {
        registerFromText("over_gem", """
                        over_gem:
                          name: "名称"
                          type: UTILITY
                          level: 1
                          material: BONE
                          attributes:
                            - "经验加成 +4%"
                          overrides:
                            display_pattern:
                              display_name: "测试宝石3"
                              lore:
                                - "&7不过1个普普通通的小宝石罢了"
                                - "{attributeLores}"
                              per_line_attribute_lore: "&7  - &b{attributeLore}"
                        """);
        Gem gem = plugin.getGemManager().getGem("over_gem");
        assertNotNull(gem);
        assertEquals("测试宝石3", gem.getDisplayName());
        assertEquals(2, gem.getLore().size());
        assertTrue(gem.getLore().get(0).contains("普普通通"));
        assertTrue(gem.getLore().get(1).contains("经验加成 +4%"));
    }

    @Test
    void duplicateGemIdKeepsFirstDefinition() {
        registerFromText("dup_gem", """
                        dup_gem:
                          name: "第一个"
                          type: UTILITY
                          level: 1
                          material: BONE
                        """);
        registerFromText("dup_gem", """
                        dup_gem:
                          name: "第二个"
                          type: UTILITY
                          level: 2
                          material: BONE
                        """);
        Gem gem = plugin.getGemManager().getGem("dup_gem");
        assertNotNull(gem);
        assertEquals("第一个", gem.getName());
        assertEquals(1, gem.getLevel());
    }

    @Test
    void invalidMaterialFallsBackToEmerald() {
        registerFromText("bad_material", """
                        bad_material:
                          name: "坏材质"
                          type: UTILITY
                          level: 1
                          material: RUBY
                        """);
        Gem gem = plugin.getGemManager().getGem("bad_material");
        assertNotNull(gem);
        assertEquals(Material.EMERALD, gem.getMaterial());
    }

    @Test
    void parsesWhitelistMaterialFilter() {
        registerFromText("whitelist_gem", """
                        whitelist_gem:
                          name: "白名单"
                          type: UTILITY
                          level: 1
                          material: BONE
                          socket:
                            equipment_materials:
                              mode: WHITELIST
                              list: [DIAMOND_SWORD]
                        """);
        Gem gem = plugin.getGemManager().getGem("whitelist_gem");
        assertNotNull(gem);
        assertEquals(Gem.MaterialFilterMode.WHITELIST, gem.getMaterialFilterMode());
        assertEquals(Set.of(Material.DIAMOND_SWORD), gem.getFilterMaterials());
        assertTrue(gem.canSocketTo(Material.DIAMOND_SWORD));
        assertFalse(gem.canSocketTo(Material.STONE));
    }

    @Test
    void parsesBlacklistMaterialFilter() {
        registerFromText("blacklist_gem", """
                        blacklist_gem:
                          name: "黑名单"
                          type: UTILITY
                          level: 1
                          material: BONE
                          socket:
                            equipment_materials:
                              mode: BLACKLIST
                              list: [DIAMOND_SWORD]
                        """);
        Gem gem = plugin.getGemManager().getGem("blacklist_gem");
        assertNotNull(gem);
        assertEquals(Gem.MaterialFilterMode.BLACKLIST, gem.getMaterialFilterMode());
        assertFalse(gem.canSocketTo(Material.DIAMOND_SWORD));
        assertTrue(gem.canSocketTo(Material.STONE));
    }

    @Test
    void parsesItemOverrides() {
        registerFromText("item_gem", """
                        item_gem:
                          name: "带物品属性"
                          type: UTILITY
                          level: 1
                          material: LEATHER_HELMET
                          overrides:
                            item:
                              Durability: "20"
                              EnchantList:
                                - "SHARPNESS:5"
                              ItemFlagList:
                                - "HIDE_ENCHANTS"
                              Color: "FF0000"
                              Attributes:
                                - "GENERIC_ATTACK_DAMAGE:10:0:MAINHAND"
                              CustomModelData: 42
                              Potion:
                                SPEED:
                                  duration: 200
                                  amplifier: 1
                                  ambient: true
                                  particles: true
                                  icon: true
                        """);
        Gem gem = plugin.getGemManager().getGem("item_gem");
        assertNotNull(gem);
        assertEquals(Gem.DurabilityMode.DAMAGE, gem.getDurability().mode());
        assertEquals(20, gem.getDurability().value());
        Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
        assertEquals(5, gem.getEnchantments().get(sharpness));
        assertTrue(gem.getItemFlags().contains(ItemFlag.HIDE_ENCHANTS));
        assertEquals(Color.fromRGB(255, 0, 0), gem.getLeatherColor());
        assertEquals(1, gem.getAttributes().size());
        Gem.AttributeEntry attribute = gem.getAttributes().getFirst();
        assertEquals(Attribute.ATTACK_DAMAGE, attribute.attribute());
        assertEquals(10, attribute.amount());
        assertEquals(AttributeModifier.Operation.ADD_NUMBER, attribute.operation());
        assertEquals(EquipmentSlot.HAND, attribute.slot());
        assertEquals(42, gem.getCustomModelData());
        assertEquals(1, gem.getPotionEffects().size());
        Gem.PotionEntry potion = gem.getPotionEffects().getFirst();
        assertEquals(200, potion.duration());
        assertEquals(1, potion.amplifier());
        assertTrue(potion.ambient());
        assertTrue(potion.particles());
        assertTrue(potion.icon());
    }
}
