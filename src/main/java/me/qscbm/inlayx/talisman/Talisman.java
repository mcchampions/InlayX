package me.qscbm.inlayx.talisman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.qscbm.inlayx.gem.AttributeEntry;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 保护符
 */
@Getter
@Setter
public class Talisman {
    private final String id;

    private final String name;

    private final Material material;

    private String displayName;

    private List<String> lore;

    @Getter(AccessLevel.NONE)
    private final Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Set<ItemFlag> itemFlags = EnumSet.noneOf(ItemFlag.class);

    @Getter(AccessLevel.NONE)
    private final List<AttributeEntry> attributes = new ArrayList<>();

    private Color leatherColor;

    private Integer customModelData;

    private Gem.DurabilityEntry durability;

    @Getter(AccessLevel.NONE)
    private final List<Gem.PotionEntry> potionEffects = new ArrayList<>();

    /**
     * 应用到宝石后, 效果的可用次数.
     */
    private int maxUses = 1;

    private TalismanFunction function = TalismanFunction.empty();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private ItemMeta itemMetaTemplate;

    public Talisman(String id, String name, Material material) {
        this.id = id;
        this.name = name;
        this.material = material;
    }

    // ==================== 物品修饰 ====================

    public Map<Enchantment, Integer> getEnchantments() {
        return Collections.unmodifiableMap(enchantments);
    }

    public void addEnchantment(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
    }

    public Set<ItemFlag> getItemFlags() {
        return Collections.unmodifiableSet(itemFlags);
    }

    public void addItemFlag(ItemFlag itemFlag) {
        itemFlags.add(itemFlag);
    }

    public List<AttributeEntry> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public void addAttribute(AttributeEntry attribute) {
        attributes.add(attribute);
    }

    public List<Gem.PotionEntry> getPotionEffects() {
        return Collections.unmodifiableList(potionEffects);
    }

    public void addPotionEffect(Gem.PotionEntry potion) {
        potionEffects.add(potion);
    }

    ItemMeta getItemMetaTemplate() {
        return itemMetaTemplate;
    }

    void setItemMetaTemplate(ItemMeta itemMetaTemplate) {
        this.itemMetaTemplate = itemMetaTemplate;
    }
}
