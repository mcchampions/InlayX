package me.qscbm.inlayx.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class GemTemplateTest {

    @Test
    void parsesAllVariables() {
        Gem gem = new Gem("g1", "红宝石", new GemType("ATTACK", "攻击", ChatColor.RED), 3, Material.EMERALD);
        gem.setSocketSuccessRate(0.8);
        gem.setDestroyOnFailure(true);
        String out = GemTemplate.parse(
                "{gemTypeColor}{gemName} {gemLevelStars} {gemLevel} {successRate} {destroyOnFailure} {gemTypeName} {gemId}",
                gem);
        assertEquals(ChatColor.RED + "红宝石 ★★★ 3 80.0% 是 攻击 g1", out);
    }

    @Test
    void replacesExtraVariables() {
        Gem gem = new Gem("g1", "红宝石", new GemType("ATTACK", "攻击", ChatColor.RED), 1, Material.EMERALD);
        assertEquals("A替换B", GemTemplate.parse("A{extra}B", gem, "{extra}", "替换"));
    }
}
