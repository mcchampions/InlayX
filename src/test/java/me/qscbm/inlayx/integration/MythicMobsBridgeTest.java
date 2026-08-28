package me.qscbm.inlayx.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import me.qscbm.inlayx.InlayXTestBase;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class MythicMobsBridgeTest extends InlayXTestBase {
    @Test
    void fallsBackToVanillaWithoutMythicMobs() {
        MythicMobsBridge bridge = new MythicMobsBridge(plugin);
        assertFalse(bridge.isAvailable());
        PlayerMock player = server.addPlayer("Steve");
        assertFalse(bridge.isMythicMob(player));
        assertEquals(1, bridge.getMobLevel(player));
    }
}
