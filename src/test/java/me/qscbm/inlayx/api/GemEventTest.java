package me.qscbm.inlayx.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.api.event.GemExtractEvent;
import me.qscbm.inlayx.api.event.GemRegisterEvent;
import me.qscbm.inlayx.api.event.GemSocketEvent;
import me.qscbm.inlayx.api.event.GemSocketedEvent;
import me.qscbm.inlayx.api.event.GemUnregisterEvent;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemType;
import me.qscbm.inlayx.socket.ExtractResult;
import me.qscbm.inlayx.socket.SocketResult;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class GemEventTest extends InlayXTestBase {
    private PlayerMock player;

    @BeforeEach
    void init() {
        player = server.addPlayer("Steve");
    }

    private static final class CancelSocketListener implements Listener {
        @EventHandler
        public void onSocket(GemSocketEvent event) {
            event.setCancelled(true);
        }
    }

    private static final class CancelExtractListener implements Listener {
        @EventHandler
        public void onExtract(GemExtractEvent event) {
            event.setCancelled(true);
        }
    }

    private static final class CancelRegisterListener implements Listener {
        @EventHandler
        public void onRegister(GemRegisterEvent event) {
            event.setCancelled(true);
        }
    }

    private static final class CancelUnregisterListener implements Listener {
        @EventHandler
        public void onUnregister(GemUnregisterEvent event) {
            event.setCancelled(true);
        }
    }

    @Test
    void cancellingGemSocketEventStopsSocketing() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(plugin.getConfigManager().getGemType("ATTACK"), 1);
        ItemStack gemItem = plugin.getGemManager().createGemItem("t1");
        server.getPluginManager().registerEvents(new CancelSocketListener(), plugin);

        SocketResult result = plugin.getGemManager().socketGem(player, sword, gemItem);

        assertEquals(SocketResult.Status.CANCELLED, result.getStatus());
        assertFalse(plugin.getGemManager().getSocketedGems(sword).contains("t1"));
    }

    @Test
    void gemSocketedEventFiresAfterSuccessfulSocket() {
        registerGem("t1", "ATTACK", 1.0);
        ItemStack sword = socketableSword(plugin.getConfigManager().getGemType("ATTACK"), 1);
        ItemStack gemItem = plugin.getGemManager().createGemItem("t1");
        AtomicBoolean fired = new AtomicBoolean();
        server.getPluginManager()
                .registerEvents(
                        new Listener() {
                            @EventHandler
                            public void onSocketed(GemSocketedEvent event) {
                                fired.set(true);
                            }
                        },
                        plugin);

        SocketResult result = plugin.getGemManager().socketGem(player, sword, gemItem);

        assertEquals(SocketResult.Status.SUCCESS, result.getStatus());
        assertTrue(fired.get());
    }

    @Test
    void cancellingGemExtractEventKeepsGemOnEquipment() {
        registerGem("t1", "ATTACK", 1.0);
        setExtractRate(1.0);
        GemType attack = plugin.getConfigManager().getGemType("ATTACK");
        ItemStack sword = socketableSword(attack, 1);
        plugin.getGemManager().socketGem(sword, plugin.getGemManager().createGemItem("t1"));
        server.getPluginManager().registerEvents(new CancelExtractListener(), plugin);

        ExtractResult result = plugin.getGemManager().extractGem(player, sword, "t1");

        assertEquals(ExtractResult.Status.CANCELLED, result.getStatus());
        assertTrue(plugin.getGemManager().getSocketedGems(sword).contains("t1"));
    }

    @Test
    void cancellingGemRegisterEventStopsRegistration() {
        server.getPluginManager().registerEvents(new CancelRegisterListener(), plugin);
        GemType attack = plugin.getConfigManager().getGemType("ATTACK");
        Gem gem = new Gem("t1", "测试宝石", attack, 1, Material.EMERALD);

        plugin.getGemManager().registerGem(gem);

        assertNull(plugin.getGemManager().getGem("t1"));
    }

    @Test
    void cancellingGemUnregisterEventKeepsGemRegistered() {
        registerGem("t1", "ATTACK", 1.0);
        server.getPluginManager().registerEvents(new CancelUnregisterListener(), plugin);

        Gem removed = plugin.getGemManager().unregisterGem("t1");

        assertNull(removed);
        assertNotNull(plugin.getGemManager().getGem("t1"));
    }
}
