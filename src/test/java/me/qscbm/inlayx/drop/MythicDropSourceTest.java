package me.qscbm.inlayx.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.UUID;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.api.DropCandidate;
import me.qscbm.inlayx.api.DropSourceContext;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.integration.MythicMobsBridge;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.entity.ZombieMock;

class MythicDropSourceTest extends InlayXTestBase {
    private PlayerMock player;
    private ZombieMock zombie;

    @BeforeEach
    void init() {
        player = server.addPlayer("Steve");
        zombie = new ZombieMock(server, UUID.randomUUID());
    }

    @Test
    void selectsWhenLevelAllows() {
        MythicDropSource source = new MythicDropSource(new TestBridge(plugin, true, 5));
        Gem gem = new Gem("test", "测试", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("chance", 1.0);
        settings.set("min_mob_level", 1);
        settings.set("per_level_rate", 0.0);
        DropSourceContext context = new DropSourceContext(zombie, player, List.of(new DropCandidate(gem, settings)));
        source.handleEntityDeath(context);
        assertSame(gem, context.getSelected().gem());
    }

    @Test
    void rejectsLevelBelowMinimum() {
        MythicDropSource source = new MythicDropSource(new TestBridge(plugin, true, 4));
        Gem gem = new Gem("test", "测试", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("chance", 1.0);
        settings.set("min_mob_level", 5);
        settings.set("per_level_rate", 0.0);
        DropSourceContext context = new DropSourceContext(zombie, player, List.of(new DropCandidate(gem, settings)));
        source.handleEntityDeath(context);
        assertNull(context.getSelected());
    }

    @Test
    void usesPerLevelRate() {
        MythicDropSource source = new MythicDropSource(new TestBridge(plugin, true, 2));
        Gem gem = new Gem("test", "测试", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("chance", 0.0);
        settings.set("min_mob_level", 1);
        settings.set("per_level_rate", 1.0);
        DropSourceContext context = new DropSourceContext(zombie, player, List.of(new DropCandidate(gem, settings)));
        source.handleEntityDeath(context);
        assertEquals(gem, context.getSelected().gem());
    }

    @Test
    void ignoresNonMythicMob() {
        MythicDropSource source = new MythicDropSource(new TestBridge(plugin, false, 5));
        Gem gem = new Gem("test", "测试", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("chance", 1.0);
        settings.set("min_mob_level", 1);
        settings.set("per_level_rate", 0.0);
        DropSourceContext context = new DropSourceContext(zombie, player, List.of(new DropCandidate(gem, settings)));
        source.handleEntityDeath(context);
        assertNull(context.getSelected());
    }

    private static final class TestBridge extends MythicMobsBridge {
        private final boolean mythic;
        private final int level;

        private TestBridge(InlayX plugin, boolean mythic, int level) {
            super(plugin);
            this.mythic = mythic;
            this.level = level;
        }

        @Override
        public boolean isMythicMob(LivingEntity entity) {
            return mythic;
        }

        @Override
        public int getMobLevel(LivingEntity entity) {
            return level;
        }
    }
}
