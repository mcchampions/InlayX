package me.qscbm.inlayx.gem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import me.qscbm.inlayx.InlayX;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

/**
 * 宝石物品工厂
 * <p>
 * 宝石物品识别与生成.
 */
public class GemItemFactory {
    static final String GEM_ID_KEY = "gem_id";

    private final InlayX plugin;
    private final AtomicReference<GemManager.GemRegistry> registry;

    public GemItemFactory(InlayX plugin, AtomicReference<GemManager.GemRegistry> registry) {
        this.plugin = plugin;
        this.registry = registry;
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
        Gem gem = registry.get().gems().get(gemId);
        if (gem == null || gem.getItemMetaTemplate() == null) {
            return null;
        }
        ItemStack item = new ItemStack(gem.getMaterial());
        item.setItemMeta(gem.getItemMetaTemplate().clone());
        return item;
    }

    boolean initializeItemMetaTemplate(Gem gem) {
        ItemStack item = new ItemStack(gem.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            plugin.getLogger().severe("宝石 " + gem.getId() + " 的材质无法创建 ItemMeta: " + gem.getMaterial());
            return false;
        }
        meta.setDisplayName(gem.getDisplayName());
        meta.setLore(gem.getLore());
        applyItemOverrides(gem, meta);
        meta.getPersistentDataContainer().set(gemIdKey(), PersistentDataType.STRING, gem.getId());
        gem.setItemMetaTemplate(meta);
        return true;
    }

    private void applyItemOverrides(Gem gem, ItemMeta meta) {
        for (ItemFlag itemFlag : gem.getItemFlags()) {
            meta.addItemFlags(itemFlag);
        }
        for (Map.Entry<Enchantment, Integer> entry : gem.getEnchantments().entrySet()) {
            if (!meta.addEnchant(entry.getKey(), entry.getValue(), true)) {
                plugin.getLogger()
                        .warning("宝石 " + gem.getId() + " 的附魔 "
                                + entry.getKey().getKey().getKey() + " 无法应用到材质 " + gem.getMaterial() + " 上");
            }
        }
        if (gem.getLeatherColor() != null) {
            if (meta instanceof LeatherArmorMeta leatherMeta) {
                leatherMeta.setColor(gem.getLeatherColor());
            } else {
                plugin.getLogger()
                        .warning("宝石 " + gem.getId() + " 配置了 Color, 但材质 " + gem.getMaterial() + " 不是皮革装备, 颜色不会生效");
            }
        }
        if (gem.getDurability() != null) {
            if (!(meta instanceof Damageable damageable)) {
                plugin.getLogger()
                        .warning("宝石 " + gem.getId() + " 配置了 Durability, 但材质 " + gem.getMaterial() + " 没有耐久条");
            } else {
                int maxDamage = damageable.hasMaxDamage()
                        ? damageable.getMaxDamage()
                        : gem.getMaterial().getMaxDurability();
                if (maxDamage <= 0) {
                    plugin.getLogger()
                            .warning("宝石 " + gem.getId() + " 配置了 Durability, 但材质 " + gem.getMaterial() + " 没有耐久条");
                } else {
                    Gem.DurabilityEntry durability = gem.getDurability();
                    int damage =
                            switch (durability.mode()) {
                                case DAMAGE ->
                                    clamp((int) Math.round(durability.value()), 0, Math.max(0, maxDamage - 1));
                                case REMAINING -> maxDamage - clamp((int) Math.round(durability.value()), 1, maxDamage);
                                case PERCENT ->
                                    maxDamage
                                            - clamp(
                                                    (int) Math.round(maxDamage * durability.value() / 100.0),
                                                    1,
                                                    maxDamage);
                            };
                    damageable.setDamage(damage);
                }
            }
        }
        int attributeIndex = 0;
        for (Gem.AttributeEntry attribute : gem.getAttributes()) {
            NamespacedKey modifierKey = new NamespacedKey(plugin, "attribute_" + attributeIndex++);
            AttributeModifier modifier = attribute.slot() == null
                    ? new AttributeModifier(modifierKey, attribute.amount(), attribute.operation())
                    : new AttributeModifier(
                            modifierKey,
                            attribute.amount(),
                            attribute.operation(),
                            attribute.slot().getGroup());
            meta.addAttributeModifier(attribute.attribute(), modifier);
        }
        if (gem.getCustomModelData() != null) {
            meta.setCustomModelData(gem.getCustomModelData());
        }
        if (!gem.getPotionEffects().isEmpty()) {
            if (meta instanceof PotionMeta potionMeta) {
                for (Gem.PotionEntry potion : gem.getPotionEffects()) {
                    potionMeta.addCustomEffect(
                            new PotionEffect(
                                    potion.effect(),
                                    potion.duration(),
                                    potion.amplifier(),
                                    potion.ambient(),
                                    potion.particles(),
                                    potion.icon()),
                            true);
                }
            } else {
                plugin.getLogger()
                        .warning("宝石 " + gem.getId() + " 配置了 Potion, 但材质 " + gem.getMaterial() + " 不是药水类物品, 效果不会生效");
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 生成配置已不存在的宝石的占位物品(仅用于 GUI 展示, 携带原 gem_id 以供点击移除).
     */
    public ItemStack createUnknownGemItem(String gemId) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "未知宝石");
        meta.setLore(List.of(ChatColor.GRAY + "宝石ID: " + gemId, ChatColor.RED + "该宝石的配置已不存在, 点击将直接移除(不会返还)"));
        meta.getPersistentDataContainer().set(gemIdKey(), PersistentDataType.STRING, gemId);
        item.setItemMeta(meta);
        return item;
    }
}
