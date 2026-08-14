package me.qscbm.inlayx.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
