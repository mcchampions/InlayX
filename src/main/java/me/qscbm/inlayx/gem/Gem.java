package me.qscbm.inlayx.gem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

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
     * 装备材质过滤模式. NONE 不限制, WHITELIST 只允许列表中的材质, BLACKLIST 禁止列表中的材质.
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
     * 判断宝石能否镶嵌到指定材质的装备上. 过滤模式为 NONE 时任何装备都可以.
     */
    public boolean canSocketTo(Material material) {
        return switch (materialFilterMode) {
            case NONE -> true;
            case WHITELIST -> filterMaterials.contains(material);
            case BLACKLIST -> !filterMaterials.contains(material);
        };
    }
}
