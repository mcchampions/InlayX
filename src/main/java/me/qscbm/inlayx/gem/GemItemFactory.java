package me.qscbm.inlayx.gem;

import java.util.Map;
import me.qscbm.inlayx.InlayX;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * 宝石物品工厂
 * <p>
 * 宝石物品识别与生成.
 */
public class GemItemFactory {
    static final String GEM_ID_KEY = "gem_id";

    private final InlayX plugin;
    private final Map<String, Gem> gems;

    public GemItemFactory(InlayX plugin, Map<String, Gem> gems) {
        this.plugin = plugin;
        this.gems = gems;
    }

    NamespacedKey gemIdKey() {
        return new NamespacedKey(this.plugin, GEM_ID_KEY);
    }

    public boolean isGem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return item.getPersistentDataContainer().has(gemIdKey(), PersistentDataType.STRING);
    }

    public String getGemId(ItemStack item) {
        if (!isGem(item)) {
            return null;
        }
        return item.getPersistentDataContainer().get(gemIdKey(), PersistentDataType.STRING);
    }

    public String getGemId(ItemMeta meta) {
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(gemIdKey(), PersistentDataType.STRING);
    }

    public ItemStack createGemItem(String gemId) {
        Gem gem = gems.get(gemId);
        if (gem == null) {
            return null;
        }
        ItemStack item = new ItemStack(gem.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(gem.getDisplayName());
        meta.setLore(gem.getLore());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(gemIdKey(), PersistentDataType.STRING, gemId);
        item.setItemMeta(meta);
        return item;
    }
}
