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
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.config.ItemGroupOrItem;
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
    @Getter(AccessLevel.NONE)
    private final Map<String, Map<String, Object>> dropSourceSettings = new LinkedHashMap<>();

    /*
     * 镶嵌配置
     */
    private double socketSuccessRate = 1;
    private boolean destroyOnFailure = false;

    /*
     * 装备材质过滤
     */
    private Gem.MaterialFilterMode materialFilterMode = Gem.MaterialFilterMode.NONE;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Set<ItemGroupOrItem> filterMaterials = new HashSet<>();

    /*
     * 显示样式
     */
    private String displayName;
    private List<String> lore;

    private List<String> noneFilterPattern = InlayX.INSTANCE.getConfigManager().getNoneFilterPattern();
    private List<String> whiteListFilterPattern =
            InlayX.INSTANCE.getConfigManager().getWhiteListFilterPattern();
    private List<String> blackListFilterPattern =
            InlayX.INSTANCE.getConfigManager().getBlackListFilterPattern();
    private String perLineEquipmentDisplayLore =
            InlayX.INSTANCE.getConfigManager().getPerLineEquipmentDisplayLore();

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

    private GemAttachment gemAttachment;

    /**
     * 宝石注册来源
     * <p>
     * 对于外部插件来说, 应该自定义一个标识,
     * 以用于辨别宝石注册来源, 并且防止插件重载时失效
     */
    private final String identifier;

    public Gem(String id, String name, GemType type, int level, Material material) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.level = level;
        this.material = material;
        this.attributeLore = new ArrayList<>();
        this.identifier = "inlayx";
    }

    public Gem(String id, String name, GemType type, int level, Material material, String identifier) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.level = level;
        this.material = material;
        this.attributeLore = new ArrayList<>();
        this.identifier = identifier;
    }

    // ==================== 属性 Lore ====================

    public List<String> getAttributeLore() {
        return Collections.unmodifiableList(attributeLore);
    }

    public void addAttributeLore(String lore) {
        attributeLore.add(lore);
    }

    public int getAttributeCount() {
        return attributeLore.size();
    }

    // ==================== 掉落配置 ====================

    public Set<String> getDropSources() {
        return Collections.unmodifiableSet(dropSourceSettings.keySet());
    }

    public boolean hasDropSource(String sourceId) {
        return dropSourceSettings.containsKey(sourceId);
    }

    public Map<String, Map<String, Object>> getDropSourceSettings() {
        Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : dropSourceSettings.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    public void setDropSourceSettings(Map<String, Map<String, Object>> settings) {
        dropSourceSettings.clear();
        if (settings != null) {
            for (Map.Entry<String, Map<String, Object>> entry : settings.entrySet()) {
                putDropSourceSettings(entry.getKey(), entry.getValue());
            }
        }
    }

    public void putDropSourceSettings(String sourceId, Map<String, Object> settings) {
        if (sourceId == null || sourceId.isBlank() || settings == null) {
            throw new IllegalArgumentException("掉落来源配置不能为空");
        }
        dropSourceSettings.put(sourceId, new LinkedHashMap<>(settings));
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

    public Set<ItemGroupOrItem> getFilterMaterials() {
        return Collections.unmodifiableSet(filterMaterials);
    }

    public void setFilterMaterials(Set<ItemGroupOrItem> filterMaterials) {
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
            case WHITELIST -> {
                for (ItemGroupOrItem item : filterMaterials) {
                    if (item.containsItem(material)) {
                        yield true;
                    }
                }
                yield false;
            }
            case BLACKLIST -> {
                for (ItemGroupOrItem item : filterMaterials) {
                    if (item.containsItem(material)) {
                        yield false;
                    }
                }
                yield true;
            }
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
