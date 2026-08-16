package me.qscbm.inlayx.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.api.DropSourceContext;
import me.qscbm.inlayx.api.event.GemDropEvent;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.entity.ZombieMock;

class DropCoordinatorTest extends InlayXTestBase {

    private PlayerMock player;
    private ZombieMock zombie;

    @BeforeEach
    void init() {
        player = server.addPlayer("Steve");
        zombie = new ZombieMock(server, UUID.randomUUID());
        zombie.setKiller(player);
        plugin.getConfig().set("settings.gem.drop.enable", true);
        plugin.getConfigManager().loadSettings();
    }

    @Test
    void higherPrioritySourceRunsFirstInFirstMode() {
        registerGem("high_gem", "high_source", Map.of("chance", 1.0, "priority", 9));
        registerGem("low_gem", "low_source", Map.of("chance", 1.0, "priority", 1));
        registerSelectingSource("low_source", 1);
        registerSelectingSource("high_source", 9);

        EntityDeathEvent event = new EntityDeathEvent(zombie, null, new ArrayList<>());
        plugin.getDropCoordinator().onEntityDeath(event);

        assertEquals(1, event.getDrops().size());
        assertEquals(
                "high_gem", plugin.getGemManager().getGemId(event.getDrops().get(0)));
    }

    @Test
    void allModeContinuesAfterDrop() {
        plugin.getConfig().set("settings.gem.drop.mode", "ALL");
        plugin.getConfigManager().loadSettings();
        registerGem("high_gem", "high_source", Map.of("chance", 1.0, "priority", 9));
        registerGem("low_gem", "low_source", Map.of("chance", 1.0, "priority", 1));
        registerSelectingSource("low_source", 1);
        registerSelectingSource("high_source", 9);

        EntityDeathEvent event = new EntityDeathEvent(zombie, null, new ArrayList<>());
        plugin.getDropCoordinator().onEntityDeath(event);

        assertEquals(2, event.getDrops().size());
        assertTrue(event.getDrops().stream()
                .anyMatch(item -> plugin.getGemManager().getGemId(item).equals("high_gem")));
        assertTrue(event.getDrops().stream()
                .anyMatch(item -> plugin.getGemManager().getGemId(item).equals("low_gem")));
    }

    @Test
    void cancelledEventDoesNotStopLaterSources() {
        AtomicInteger count = new AtomicInteger();
        server.getPluginManager()
                .registerEvents(
                        new Listener() {
                            @EventHandler
                            public void onDrop(GemDropEvent event) {
                                if (count.incrementAndGet() == 1) {
                                    event.setCancelled(true);
                                }
                            }
                        },
                        plugin);
        registerGem("high_gem", "high_source", Map.of("chance", 1.0, "priority", 9));
        registerGem("low_gem", "low_source", Map.of("chance", 1.0, "priority", 1));
        registerSelectingSource("low_source", 1);
        registerSelectingSource("high_source", 9);

        EntityDeathEvent event = new EntityDeathEvent(zombie, null, new ArrayList<>());
        plugin.getDropCoordinator().onEntityDeath(event);

        assertEquals(1, event.getDrops().size());
        assertEquals("low_gem", plugin.getGemManager().getGemId(event.getDrops().get(0)));
    }

    @Test
    void disabledCandidateIsSkipped() {
        registerGem("disabled_gem", "disabled_source", Map.of("chance", 1.0, "enable", false));
        registerSelectingSource("disabled_source", 0);

        EntityDeathEvent event = new EntityDeathEvent(zombie, null, new ArrayList<>());
        plugin.getDropCoordinator().onEntityDeath(event);

        assertTrue(event.getDrops().isEmpty());
    }

    @Test
    void candidateSettingsMergeDefaultsWithGemOverrides() {
        AtomicReference<ConfigurationSection> seenSettings = new AtomicReference<>();
        plugin.registerDropSource(new TestDropSource("merge_source", Map.of("chance", 0.5, "extra", "bar")) {
            @Override
            public void handleEntityDeath(DropSourceContext context) {
                seenSettings.set(context.getCandidates().get(0).settings());
                context.select(context.getCandidates().get(0));
            }
        });
        registerGem("merge_gem", "merge_source", Map.of("chance", 0.1, "priority", 7));

        EntityDeathEvent event = new EntityDeathEvent(zombie, null, new ArrayList<>());
        plugin.getDropCoordinator().onEntityDeath(event);

        assertNotNull(seenSettings.get());
        assertEquals(0.1, seenSettings.get().getDouble("chance"));
        assertEquals("bar", seenSettings.get().getString("extra"));
        assertEquals(7, seenSettings.get().getInt("priority"));
        assertEquals(1, event.getDrops().size());
    }

    @Test
    void gemDropEventCarriesSourceAndSettings() {
        AtomicReference<ConfigurationSection> eventSettings = new AtomicReference<>();
        AtomicReference<String> eventSource = new AtomicReference<>();
        server.getPluginManager()
                .registerEvents(
                        new Listener() {
                            @EventHandler
                            public void onDrop(GemDropEvent event) {
                                eventSettings.set(event.getSettings());
                                eventSource.set(event.getSourceId());
                            }
                        },
                        plugin);
        registerGem("event_gem", "event_source", Map.of("chance", 0.25, "priority", 3));
        registerSelectingSource("event_source", 3);

        EntityDeathEvent event = new EntityDeathEvent(zombie, null, new ArrayList<>());
        plugin.getDropCoordinator().onEntityDeath(event);

        assertNotNull(eventSettings.get());
        assertEquals("event_source", eventSource.get());
        assertEquals(0.25, eventSettings.get().getDouble("chance"));
        assertEquals(3, eventSettings.get().getInt("priority"));
    }

    private void registerSelectingSource(String id, int priority) {
        plugin.registerDropSource(new TestDropSource(id, Map.of("chance", 1.0, "priority", priority)) {
            @Override
            public void handleEntityDeath(DropSourceContext context) {
                context.select(context.getCandidates().get(0));
            }
        });
    }

    private void registerGem(String gemId, String sourceId, Map<String, Object> settings) {
        Gem gem = new Gem(gemId, "测试宝石", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
        gem.putDropSourceSettings(sourceId, settings);
        plugin.getGemManager().registerGem(gem);
    }

    private static class TestDropSource implements DropSource {
        private final String id;
        private final Map<String, Object> defaults;

        private TestDropSource(String id, Map<String, Object> defaults) {
            this.id = id;
            this.defaults = defaults;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Map<String, Object> defaultSettings() {
            return defaults;
        }

        @Override
        public void handleEntityDeath(DropSourceContext context) {}
    }
}
