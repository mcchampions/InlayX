package me.qscbm.inlayx.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.UUID;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.api.DropCandidate;
import me.qscbm.inlayx.api.DropSourceContext;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.entity.SkeletonMock;
import org.mockbukkit.mockbukkit.entity.ZombieMock;

class NormalDropSourceTest extends InlayXTestBase {

    private PlayerMock player;
    private NormalDropSource source;

    @BeforeEach
    void init() {
        player = server.addPlayer("Steve");
        source = new NormalDropSource();
    }

    @Test
    void selectsAllowedMobWithChanceOne() {
        ZombieMock zombie = new ZombieMock(server, UUID.randomUUID());
        Gem gem = new Gem("test", "测试", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("chance", 1.0);
        settings.set("allow_entities", List.of("ZOMBIE"));
        DropSourceContext context = new DropSourceContext(zombie, player, List.of(new DropCandidate(gem, settings)));
        source.handleEntityDeath(context);
        assertSame(gem, context.getSelected().gem());
    }

    @Test
    void ignoresNotAllowedEntities() {
        SkeletonMock skeleton = new SkeletonMock(server, UUID.randomUUID());
        Gem gem = new Gem("test", "测试", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("chance", 1.0);
        settings.set("allow_entities", List.of("ZOMBIE"));
        DropSourceContext context = new DropSourceContext(skeleton, player, List.of(new DropCandidate(gem, settings)));
        source.handleEntityDeath(context);
        assertNull(context.getSelected());
    }

    @Test
    void zeroChanceDoesNotSelect() {
        ZombieMock zombie = new ZombieMock(server, UUID.randomUUID());
        Gem gem = new Gem("test", "测试", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("chance", 0.0);
        settings.set("allow_entities", List.of("ZOMBIE"));
        DropSourceContext context = new DropSourceContext(zombie, player, List.of(new DropCandidate(gem, settings)));
        source.handleEntityDeath(context);
        assertNull(context.getSelected());
    }

    @Test
    void weightedSelectNeverPicksZeroChanceGem() {
        ZombieMock zombie = new ZombieMock(server, UUID.randomUUID());
        Gem zeroGem = new Gem("zero", "零", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.EMERALD);
        Gem certainGem = new Gem("certain", "必掉", plugin.getConfigManager().getGemType("ATTACK"), 1, Material.DIAMOND);
        YamlConfiguration zeroSettings = new YamlConfiguration();
        zeroSettings.set("chance", 0.0);
        zeroSettings.set("allow_entities", List.of("ZOMBIE"));
        YamlConfiguration certainSettings = new YamlConfiguration();
        certainSettings.set("chance", 1.0);
        certainSettings.set("allow_entities", List.of("ZOMBIE"));
        for (int i = 0; i < 20; i++) {
            DropSourceContext context = new DropSourceContext(
                    zombie,
                    player,
                    List.of(new DropCandidate(zeroGem, zeroSettings), new DropCandidate(certainGem, certainSettings)));
            source.handleEntityDeath(context);
            assertEquals(certainGem, context.getSelected().gem());
        }
    }
}
