package me.qscbm.inlayx.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import lombok.NonNull;
import me.qscbm.inlayx.InlayXTestBase;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.api.DropSourceContext;
import org.junit.jupiter.api.Test;

class DropSourceRegistryTest extends InlayXTestBase {

    @Test
    void builtInSourcesAreRegisteredInOrder() {
        assertEquals(
                List.of(DropSource.MYTHIC, DropSource.NORMAL),
                plugin.getDropSourceRegistry().getSources().stream()
                        .map(DropSource::id)
                        .toList());
    }

    @Test
    void registersSourceWithDefaultsAndRejectsDuplicateId() {
        DropSource source = new TestDropSource("test_source", Map.of("chance", 0.25));
        assertTrue(plugin.registerDropSource(source));
        assertEquals(source, plugin.getDropSourceRegistry().get("test_source"));
        assertEquals(
                0.25,
                plugin.getDropSourceRegistry().getDefaultSettings("test_source").get("chance"));
        assertEquals(
                true,
                plugin.getDropSourceRegistry().getDefaultSettings("test_source").get("enable"));
        assertEquals(
                0,
                plugin.getDropSourceRegistry().getDefaultSettings("test_source").get("priority"));
        assertFalse(plugin.registerDropSource(source));
    }

    @Test
    void customSourcesKeepRegistrationOrderAfterBuiltIns() {
        plugin.registerDropSource(new TestDropSource("first", Map.of()));
        plugin.registerDropSource(new TestDropSource("second", Map.of()));
        assertEquals(
                List.of(DropSource.MYTHIC, DropSource.NORMAL, "first", "second"),
                plugin.getDropSourceRegistry().getSources().stream()
                        .map(DropSource::id)
                        .toList());
    }

    @Test
    void nullDefaultSettingsIsRejected() {
        DropSource source = new TestDropSource("bad_source", null);
        assertThrows(NullPointerException.class, () -> plugin.registerDropSource(source));
    }

    private record TestDropSource(String id, Map<String, Object> defaults) implements DropSource {

        @Override
        public @NonNull Map<String, Object> defaultSettings() {
            return defaults;
        }

        @Override
        public void handleEntityDeath(@NonNull DropSourceContext context) {}
    }
}
