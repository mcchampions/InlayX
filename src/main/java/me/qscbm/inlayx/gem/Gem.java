package me.qscbm.inlayx.gem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

/**
 * 宝石
 */
@Getter
@Setter
public class Gem {
    private final String id;
    private final String name;
    private final GemType type;
    private final int level;
    private final Material material;

    /*
     * 属性 Lore 行
     */
    private final List<String> attributeLore;

    /*
     * 掉落配置
     */
    private double dropChance = 0;

    @Getter(AccessLevel.NONE)
    private Set<String> dropSources = new HashSet<>();

    private int minMobLevel = 1;
    private double levelBonus = 0;

    /*
     * 镶嵌配置
     */
    private double socketSuccessRate = 0.9;
    private boolean destroyOnFailure = false;

    /*
     * 装备材质过滤
     */
    private Gem.MaterialFilterMode materialFilterMode = Gem.MaterialFilterMode.NONE;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Set<Material> filterMaterials = new HashSet<>();

    /*
     * 显示样式
     */
    private String displayName;
    private List<String> lore;

    /*
     * 物品修饰
     */
    @Getter(AccessLevel.NONE)
    private final Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Set<ItemFlag> itemFlags = EnumSet.noneOf(ItemFlag.class);

    private Color leatherColor;

    @Getter(AccessLevel.NONE)
    private final List<AttributeEntry> attributes = new ArrayList<>();

    private Integer customModelData;

    @Getter(AccessLevel.NONE)
    private final List<PotionEntry> potionEffects = new ArrayList<>();

    /*
     * 耐久配置
     */
    private DurabilityEntry durability;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private ItemMeta itemMetaTemplate;

    public Gem(String id, String name, GemType type, int level, Material material) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.level = level;
        this.material = material;
        this.attributeLore = new ArrayList<>();
    }

    // ==================== 属性 Lore ====================

    public List<String> getAttributeLore() {
        return Collections.unmodifiableList(attributeLore);
    }

    public Set<String> getDropSources() {
        return Collections.unmodifiableSet(dropSources);
    }

    public void addAttributeLore(String lore) {
        attributeLore.add(lore);
    }

    public int getAttributeCount() {
        return attributeLore.size();
    }

    // ==================== 掉落配置 ====================

    public boolean canDropFrom(String source, int mobLevel) {
        return dropSources.contains(source) && mobLevel >= minMobLevel;
    }

    // ==================== 镶嵌限制 ====================

    /**
     * 装备材质过滤模式
     */
    public enum MaterialFilterMode {
        NONE,
        WHITELIST,
        BLACKLIST
    }

    public Set<Material> getFilterMaterials() {
        return Collections.unmodifiableSet(filterMaterials);
    }

    public void setFilterMaterials(Set<Material> filterMaterials) {
        this.filterMaterials.clear();
        if (filterMaterials != null) {
            this.filterMaterials.addAll(filterMaterials);
        }
    }

    /**
     * 判断宝石能否镶嵌到指定材质的装备上
     */
    public boolean canSocketTo(Material material) {
        return switch (materialFilterMode) {
            case NONE -> true;
            case WHITELIST -> filterMaterials.contains(material);
            case BLACKLIST -> !filterMaterials.contains(material);
        };
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

    public void addAttribute(
            Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {
        attributes.add(new AttributeEntry(attribute, amount, operation, slot));
    }

    public List<PotionEntry> getPotionEffects() {
        return Collections.unmodifiableList(potionEffects);
    }

    public void addPotionEffect(
            PotionEffectType effect, int duration, int amplifier, boolean ambient, boolean particles, boolean icon) {
        potionEffects.add(new PotionEntry(effect, duration, amplifier, ambient, particles, icon));
    }

    /**
     * 一个原版属性修饰符的配置值
     */
    public record AttributeEntry(
            Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {}

    /**
     * 药水效果的配置值
     */
    public record PotionEntry(
            PotionEffectType effect, int duration, int amplifier, boolean ambient, boolean particles, boolean icon) {}

    public record DurabilityEntry(DurabilityMode mode, double value) {}

    public enum DurabilityMode {
        /**
         * 原版损耗值
         */
        DAMAGE,
        /**
         * 剩余耐久
         */
        REMAINING,
        /**
         * 剩余耐久百分比
         */
        PERCENT
    }

    ItemMeta getItemMetaTemplate() {
        return itemMetaTemplate;
    }

    void setItemMetaTemplate(ItemMeta itemMetaTemplate) {
        this.itemMetaTemplate = itemMetaTemplate;
    }
}
