package me.qscbm.inlayx.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import me.qscbm.inlayx.InlayXTestBase;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class GemManagerDropTest extends InlayXTestBase {

    private void writeGemFile(String name, String content) throws IOException {
        File dir = new File(plugin.getDataFolder(), "gems");
        dir.mkdirs();
        Files.writeString(new File(dir, name).toPath(), content, StandardCharsets.UTF_8);
    }

    @Test
    void dropIndexContainsConfiguredSources() throws IOException {
        writeGemFile("test_drop.yml", """
                        drop_normal:
                          name: "普通掉落"
                          type: ATTACK
                          level: 1
                          material: EMERALD
                          drop:
                            normal:
                              chance: 1.0
                            mythic:
                              chance: 1.0
                              min_mob_level: 5
                        """);
        plugin.getGemManager().loadGems();
        assertTrue(plugin.getGemManager().getDropCandidates("normal").stream()
                .anyMatch(gem -> gem.getId().equals("drop_normal")));
        assertTrue(plugin.getGemManager().getDropCandidates("mythic").stream()
                .anyMatch(gem -> gem.getId().equals("drop_normal")));
        assertTrue(plugin.getGemManager().getDropCandidates("unknown").isEmpty());
    }

    @Test
    void registerAndUnregisterRebuildsDropIndex() {
        GemType type = plugin.getConfigManager().getGemType("ATTACK");
        Gem gem = new Gem("api_gem", "API宝石", type, 1, Material.EMERALD);
        gem.putDropSourceSettings("normal", Map.of("chance", 1.0));

        plugin.getGemManager().registerGem(gem);
        assertTrue(plugin.getGemManager().getDropCandidates("normal").stream()
                .anyMatch(candidate -> candidate.getId().equals("api_gem")));

        plugin.getGemManager().unregisterGem("api_gem");
        assertTrue(plugin.getGemManager().getDropCandidates("normal").stream()
                .noneMatch(candidate -> candidate.getId().equals("api_gem")));
    }

    @Test
    void gemCollectionsAreUnmodifiable() {
        GemType type = plugin.getConfigManager().getGemType("ATTACK");
        Gem gem = new Gem("api_gem", "API宝石", type, 1, Material.EMERALD);
        assertThrows(
                UnsupportedOperationException.class,
                () -> plugin.getGemManager().getGems().put("other", gem));
        assertThrows(
                UnsupportedOperationException.class, () -> gem.getDropSources().add("normal"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> gem.getDropSourceSettings().put("other", Map.of()));
    }

    @Test
    void gemDropSourceSettingsAreDetached() {
        GemType type = plugin.getConfigManager().getGemType("ATTACK");
        Gem gem = new Gem("api_gem", "API宝石", type, 1, Material.EMERALD);
        gem.putDropSourceSettings("normal", Map.of("chance", 1.0));
        assertEquals(1.0, ((Number) gem.getDropSourceSettings().get("normal").get("chance")).doubleValue());
    }
}
