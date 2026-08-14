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
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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
                            chance: 0.02
                            sources: [normal, mythic]
                            min_mob_level: 3
                            per_level_rate: 0.001
                        """);
        Gem gem = plugin.getGemManager().getGem("attack_test");
        assertNotNull(gem);
        assertEquals("血锋尖晶", gem.getName());
        assertEquals("ATTACK", gem.getType().getId());
        assertEquals(4, gem.getLevel());
        assertEquals(Material.REDSTONE, gem.getMaterial());
        assertEquals(List.of("物理伤害 +8", "暴击几率 +2.5%"), gem.getAttributeLore());
        assertEquals(0.95, gem.getSocketSuccessRate());
        assertFalse(gem.isDestroyOnFailure());
        assertEquals(0.02, gem.getDropChance());
        assertEquals(Set.of("normal", "mythic"), gem.getDropSources());
        assertEquals(3, gem.getMinMobLevel());
        assertEquals(0.001, gem.getLevelBonus());
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
}
