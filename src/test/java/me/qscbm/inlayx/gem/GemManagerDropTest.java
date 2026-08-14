package me.qscbm.inlayx.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import me.qscbm.inlayx.InlayXTestBase;
import org.junit.jupiter.api.Test;

class GemManagerDropTest extends InlayXTestBase {

    private void writeGemFile(String name, String content) throws IOException {
        File dir = new File(plugin.getDataFolder(), "gems");
        dir.mkdirs();
        Files.writeString(new File(dir, name).toPath(), content, StandardCharsets.UTF_8);
    }

    @Test
    void dropsBySourceAndMobLevel() throws IOException {
        writeGemFile("test_drop.yml", """
                        drop_normal:
                          name: "普通掉落"
                          type: ATTACK
                          level: 1
                          material: EMERALD
                          drop:
                            chance: 1.0
                            sources: [normal]
                            min_mob_level: 1
                        drop_mythic:
                          name: "神话掉落"
                          type: ATTACK
                          level: 5
                          material: DIAMOND
                          drop:
                            chance: 1.0
                            sources: [mythic]
                            min_mob_level: 5
                        """);
        plugin.getGemManager().loadGems();
        Gem normal = plugin.getGemManager().getDropGem("normal", 1);
        assertNotNull(normal);
        assertEquals("drop_normal", normal.getId());
        Gem mythic = plugin.getGemManager().getDropGem("mythic", 8);
        assertNotNull(mythic);
        assertEquals("drop_mythic", mythic.getId());
        assertNull(plugin.getGemManager().getDropGem("mythic", 4));
        assertNull(plugin.getGemManager().getDropGem("unknown", 10));
    }

    @Test
    void noDropWhenChanceIsZero() throws IOException {
        writeGemFile("test_no_drop.yml", """
                        drop_none:
                          name: "不掉落"
                          type: ATTACK
                          level: 1
                          material: EMERALD
                          drop:
                            chance: 0.0
                            sources: [normal]
                            min_mob_level: 1
                        """);
        plugin.getGemManager().loadGems();
        assertNull(plugin.getGemManager().getDropGem("normal", 1));
    }
}
