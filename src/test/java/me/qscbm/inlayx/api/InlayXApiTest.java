package me.qscbm.inlayx.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.InlayXTestBase;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;

class InlayXApiTest extends InlayXTestBase {

    @Test
    void apiIsAvailableFromServicesManagerAndStaticAccessor() {
        InlayXApi fromService = Bukkit.getServicesManager().load(InlayXApi.class);
        InlayXApi fromStatic = InlayX.getApi();

        assertNotNull(fromService);
        assertSame(plugin, fromService);
        assertSame(fromService, fromStatic);
        assertEquals(plugin.getGemManager(), fromService.getGemManager());
    }

    @Test
    void registersDropSourceThroughApi() {
        DropSource source = new DropSource() {
            @Override
            public String id() {
                return "api_source";
            }

            @Override
            public Map<String, Object> defaultSettings() {
                return Map.of("chance", 0.5);
            }

            @Override
            public void handleEntityDeath(DropSourceContext context) {}
        };
        assertTrue(InlayX.getApi().registerDropSource(source));
        assertFalse(InlayX.getApi().registerDropSource(source));
    }
}
