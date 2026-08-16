package me.qscbm.inlayx.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.api.DropSourceContext;
import me.qscbm.inlayx.config.CommentConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

class DropSourceConfigManagerTest extends InlayXTestBase {

    @Test
    void createsFileWithBuiltInDefaults() throws Exception {
        File file = new File(plugin.getDataFolder(), "drop_source.yml");
        assertTrue(file.exists());
        CommentConfiguration config = new CommentConfiguration();
        config.load(file);
        assertTrue(config.isConfigurationSection("normal"));
        assertTrue(config.isConfigurationSection("mythic"));
        List<String> allowEntities = config.getStringList("normal.allow_entities");
        assertTrue(allowEntities.contains("ZOMBIE"));
        assertTrue(allowEntities.stream().allMatch(name -> name.equals(name.toUpperCase())));
        assertEquals(
                0.1,
                plugin.getDropSourceConfigManager().getSourceDefaults("normal").getDouble("chance"));
        assertEquals(
                1,
                plugin.getDropSourceConfigManager().getSourceDefaults("mythic").getInt("priority"));
    }

    @Test
    void registeringSourceAddsMissingKeyToFile() throws Exception {
        File file = new File(plugin.getDataFolder(), "drop_source.yml");
        assertTrue(plugin.registerDropSource(new TestDropSource("custom_source", Map.of("chance", 0.3))));
        CommentConfiguration config = new CommentConfiguration();
        config.load(file);
        assertTrue(config.isConfigurationSection("custom_source"));
        assertEquals(0.3, config.getDouble("custom_source.chance"));
    }

    @Test
    void reloadDoesNotOverwriteExistingValues() throws Exception {
        File file = new File(plugin.getDataFolder(), "drop_source.yml");
        CommentConfiguration config = new CommentConfiguration();
        config.load(file);
        config.set("normal.chance", 0.77);
        config.save(file);

        plugin.getDropSourceConfigManager().load();

        CommentConfiguration reloaded = new CommentConfiguration();
        reloaded.load(file);
        assertEquals(0.77, reloaded.getDouble("normal.chance"));
        assertEquals(
                0.77,
                plugin.getDropSourceConfigManager().getSourceDefaults("normal").getDouble("chance"));
    }

    @Test
    void missingFileKeysFallBackToDefaultSettings() throws Exception {
        File file = new File(plugin.getDataFolder(), "drop_source.yml");
        CommentConfiguration config = new CommentConfiguration();
        config.load(file);
        config.set("normal.chance", null);
        config.save(file);

        plugin.getDropSourceConfigManager().load();

        assertEquals(
                0.1,
                plugin.getDropSourceConfigManager().getSourceDefaults("normal").getDouble("chance"));
    }

    @Test
    void newSourceReceivesLoadedSettingsCallback() {
        AtomicReference<ConfigurationSection> loaded = new AtomicReference<>();
        assertTrue(plugin.registerDropSource(new TestDropSource("callback_source", Map.of("chance", 0.4)) {
            @Override
            public void onSettingsLoaded(ConfigurationSection settings) {
                loaded.set(settings);
            }
        }));
        assertNotNull(loaded.get());
        assertEquals(0.4, loaded.get().getDouble("chance"));
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
