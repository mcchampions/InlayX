package me.qscbm.inlayx;

import java.util.List;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

public abstract class InlayXTestBase {

    protected ServerMock server;
    protected InlayX plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(InlayX.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    protected Gem registerGem(String id, String typeId, double successRate) {
        GemType type = plugin.getConfigManager().getGemType(typeId);
        Gem gem = new Gem(id, "测试宝石", type, 1, Material.EMERALD);
        gem.setSocketSuccessRate(successRate);
        gem.setDisplayName("测试宝石");
        gem.setLore(List.of());
        gem.addAttributeLore("物理伤害 +1");
        plugin.getGemManager().registerGem(gem);
        return gem;
    }

    protected ItemStack socketableSword(GemType type, int sockets) {
        return plugin.getGemManager().addSlotToItem(new ItemStack(Material.DIAMOND_SWORD), sockets, type);
    }

    protected void setExtractRate(double rate) {
        plugin.getConfig().set("settings.gem.extract.success_rate", rate);
        plugin.getConfigManager().loadSettings();
    }
}
