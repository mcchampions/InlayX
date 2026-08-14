package me.qscbm.inlayx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.qscbm.inlayx.InlayXTestBase;
import org.bukkit.Sound;
import org.junit.jupiter.api.Test;

class ConfigManagerTest extends InlayXTestBase {

    @Test
    void loadsDefaultsFromBundledConfig() {
        ConfigManager cm = plugin.getConfigManager();
        assertEquals(8, cm.getMaxSockets());
        assertTrue(cm.isRightClickSocketEnabled());
        assertTrue(cm.isDragSocketEnabled());
        plugin.getConfig().set("settings.gem.extract.success_rate", 0.9);
        plugin.getConfigManager().loadSettings();
        assertEquals(0.9, cm.getExtractSuccessRate());
        plugin.getConfig().set("settings.gem.extract.success_rate", 5.0);
        plugin.getConfigManager().loadSettings();
        assertEquals(1.0, cm.getExtractSuccessRate());
        assertTrue(cm.isDropGemOnFullInventory());
        assertEquals("攻击", cm.getGemType("ATTACK").getName());
        assertEquals("ATTACK", cm.getDefaultGemType().getId());
        assertTrue(cm.getSocketSuccessSound().isEnable());
        assertEquals(Sound.ENTITY_PLAYER_LEVELUP, cm.getSocketSuccessSound().getSound());
        assertEquals(Sound.BLOCK_ANVIL_BREAK, cm.getSocketFailureSound().getSound());
        assertEquals(Sound.ENTITY_PLAYER_LEVELUP, cm.getExtractSuccessSound().getSound());
        assertEquals(Sound.BLOCK_ANVIL_BREAK, cm.getExtractFailureSound().getSound());
    }

    @Test
    void reloadAppliesChangesAndFallsBackOnBadSound() {
        plugin.getConfig().set("settings.socket.quick_socket.right_click", false);
        plugin.getConfig().set("settings.sounds.socket.success.enable", false);
        plugin.getConfig().set("settings.sounds.socket.success.sound", "NOT_A_REAL_SOUND");
        plugin.getConfigManager().loadSettings();
        assertFalse(plugin.getConfigManager().isRightClickSocketEnabled());
        assertFalse(plugin.getConfigManager().getSocketSuccessSound().isEnable());
        assertEquals(
                Sound.ENTITY_PLAYER_LEVELUP,
                plugin.getConfigManager().getSocketSuccessSound().getSound());
    }
}
