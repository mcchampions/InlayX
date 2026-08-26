package me.qscbm.inlayx.talisman;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.AttributeEntry;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gui.TalismanApplyGuiFactory;
import me.qscbm.inlayx.util.TextUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 保护符管理.
 */
public class TalismanManager {
    private static final String CONFIG_FILE = "talismans.yml";

    static final String TALISMAN_ID_KEY = "talisman_id";

    /**
     * 应用保护符到宝石的结果.
     */
    public enum ApplyStatus {
        /** 成功应用 */
        SUCCESS,
        /** 重新应用, 效果次数已刷新 */
        REFRESHED,
        /** 覆盖了已有的同类效果 */
        REPLACED,
        /** 宝石已拥有同种保护符效果 */
        DUPLICATE,
        /** 该保护符未配置任何功能 */
        NO_EFFECT,
        /** 目标物品不是宝石 */
        NOT_A_GEM,
        /** 保护符不存在 */
        UNKNOWN_TALISMAN
    }

    private final InlayX plugin;

    private final TalismanEffect effect;

    private final TalismanApplyGuiFactory guiFactory;

    private final Map<String, Talisman> talismans = new LinkedHashMap<>();

    private boolean allowDifferentEffects = true;

    private boolean allowSameRestack = false;

    public TalismanManager(InlayX plugin) {
        this.plugin = plugin;
        this.effect = new TalismanEffect(new NamespacedKey(plugin, "talismans"));
        this.guiFactory = new TalismanApplyGuiFactory(plugin);
    }

    // ==================== 加载 ====================

    public synchronized void load() {
        talismans.clear();
        allowDifferentEffects = true;
        allowSameRestack = false;
        File file = new File(plugin.getDataFolder(), CONFIG_FILE);
        if (!file.exists()) {
            plugin.saveResource(CONFIG_FILE, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings != null) {
            allowDifferentEffects = settings.getBoolean("allow_different_effects", true);
            allowSameRestack = settings.getBoolean("allow_same_restack", false);
        }

        for (String id : config.getKeys(false)) {
            if ("settings".equals(id)) {
                continue;
            }
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Talisman talisman = parse(id, section);
            if (talisman != null) {
                talismans.put(id, talisman);
            }
        }
        plugin.getLogger().info("已加载 " + talismans.size() + " 个保护符");
    }

    private Talisman parse(String id, ConfigurationSection section) {
        try {
            String name = section.getString("name", "保护符");
            String materialName = section.getString("material", "PAPER");
            Material material = Material.getMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("保护符 " + id + " 使用了无效的材质: " + materialName + ", 已回退为 PAPER");
                material = Material.PAPER;
            }
            Talisman talisman = new Talisman(id, name, material);

            talisman.setDisplayName(TextUtils.translateAlternateColorCodes(section.getString("display_name", name)));
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(TextUtils.translateAlternateColorCodes(line));
            }
            talisman.setLore(lore);

            if (section.contains("custom_model_data")) {
                talisman.setCustomModelData(section.getInt("custom_model_data"));
            }

            for (String entry : section.getStringList("enchantments")) {
                int colon = entry.lastIndexOf(':');
                if (colon <= 0 || colon == entry.length() - 1) {
                    plugin.getLogger().warning("保护符 " + id + " 的 enchantments 格式错误: " + entry + ", 应使用 附魔名:等级");
                    continue;
                }
                Enchantment enchantment = parseEnchantment(entry.substring(0, colon));
                if (enchantment == null) {
                    plugin.getLogger().warning("保护符 " + id + " 的 enchantments 含有无效附魔: " + entry);
                    continue;
                }
                int level;
                try {
                    level = Integer.parseInt(entry.substring(colon + 1));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("保护符 " + id + " 的 enchantments 等级无效: " + entry);
                    continue;
                }
                talisman.addEnchantment(enchantment, level);
            }

            for (String flagName : section.getStringList("item_flags")) {
                try {
                    talisman.addItemFlag(ItemFlag.valueOf(flagName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("保护符 " + id + " 的 item_flags 含有无效选项: " + flagName);
                }
            }

            for (String entry : section.getStringList("attributes")) {
                String[] parts = entry.split(":");
                if (parts.length < 3 || parts.length > 4) {
                    plugin.getLogger().warning("保护符 " + id + " 的 attributes 格式错误: " + entry + ", 应使用 属性名:数字:模式[:生效位置]");
                    continue;
                }
                Attribute attribute = parseAttribute(parts[0]);
                if (attribute == null) {
                    plugin.getLogger().warning("保护符 " + id + " 的 attributes 含有无效属性: " + parts[0]);
                    continue;
                }
                double amount;
                try {
                    amount = Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("保护符 " + id + " 的 attributes 数值无效: " + parts[1]);
                    continue;
                }
                AttributeModifier.Operation operation = parseOperation(parts[2]);
                EquipmentSlot slot = parts.length == 4 ? parseEquipmentSlot(parts[3]) : null;
                talisman.addAttribute(new AttributeEntry(attribute, amount, operation, slot));
            }

            String colorHex = section.getString("color");
            if (colorHex != null) {
                Color color = parseHexColor(colorHex);
                if (color == null) {
                    plugin.getLogger().warning("保护符 " + id + " 的 color 无效: " + colorHex + ", 应使用 RRGGBB 或 #RRGGBB");
                } else {
                    talisman.setLeatherColor(color);
                }
            }

            String durability = section.getString("durability");
            if (durability != null) {
                Gem.DurabilityEntry durabilityEntry = parseDurability(id, durability);
                if (durabilityEntry != null) {
                    talisman.setDurability(durabilityEntry);
                }
            }

            ConfigurationSection potionSec = section.getConfigurationSection("potion");
            if (potionSec != null) {
                for (String effectName : potionSec.getKeys(false)) {
                    ConfigurationSection effectSec = potionSec.getConfigurationSection(effectName);
                    if (effectSec == null) {
                        continue;
                    }
                    PotionEffectType effectType = parsePotionEffect(effectName);
                    if (effectType == null) {
                        plugin.getLogger().warning("保护符 " + id + " 的 potion 含有无效效果: " + effectName);
                        continue;
                    }
                    talisman.addPotionEffect(new Gem.PotionEntry(
                            effectType,
                            effectSec.getInt("duration", 200),
                            effectSec.getInt("amplifier", 0),
                            effectSec.getBoolean("ambient", true),
                            effectSec.getBoolean("particles", true),
                            effectSec.getBoolean("icon", true)));
                }
            } else {
                String potionType = section.getString("potion");
                if (potionType != null) {
                    PotionEffectType effectType = parsePotionEffect(potionType);
                    if (effectType == null) {
                        plugin.getLogger().warning("保护符 " + id + " 的 potion 含有无效效果: " + potionType);
                    } else {
                        talisman.addPotionEffect(new Gem.PotionEntry(effectType, 200, 0, true, true, true));
                    }
                }
            }

            talisman.setMaxUses(Math.max(1, section.getInt("max_uses", 1)));

            ConfigurationSection functionSec = section.getConfigurationSection("function");
            double successRateBonus = 0;
            boolean preventDestroy = false;
            if (functionSec != null) {
                successRateBonus = functionSec.getDouble("success_rate_bonus", 0);
                preventDestroy = functionSec.getBoolean("prevent_destroy", false);
            }
            TalismanFunction function = new TalismanFunction(successRateBonus, preventDestroy);
            if (!function.hasAnyEffect()) {
                plugin.getLogger().warning("保护符 " + id + " 未配置任何功能(function), 已跳过");
                return null;
            }
            talisman.setFunction(function);

            if (!buildItemMetaTemplate(talisman)) {
                return null;
            }
            return talisman;
        } catch (Exception e) {
            plugin.getLogger().warning("解析保护符 " + id + " 时出错: " + e.getMessage());
            return null;
        }
    }

    // ==================== 物品模板 ====================

    private boolean buildItemMetaTemplate(Talisman talisman) {
        ItemStack item = new ItemStack(talisman.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            plugin.getLogger().severe("保护符 " + talisman.getId() + " 的材质无法创建 ItemMeta: " + talisman.getMaterial());
            return false;
        }
        meta.setDisplayName(talisman.getDisplayName());
        meta.setLore(talisman.getLore());
        applyItemOverrides(talisman, meta);
        meta.getPersistentDataContainer().set(talismanIdKey(), PersistentDataType.STRING, talisman.getId());
        talisman.setItemMetaTemplate(meta);
        return true;
    }

    private void applyItemOverrides(Talisman talisman, ItemMeta meta) {
        for (ItemFlag itemFlag : talisman.getItemFlags()) {
            meta.addItemFlags(itemFlag);
        }
        for (Map.Entry<Enchantment, Integer> entry : talisman.getEnchantments().entrySet()) {
            if (!meta.addEnchant(entry.getKey(), entry.getValue(), true)) {
                plugin.getLogger()
                        .warning("保护符 " + talisman.getId() + " 的附魔 "
                                + entry.getKey().getKey().getKey() + " 无法应用到材质 " + talisman.getMaterial() + " 上");
            }
        }
        if (talisman.getLeatherColor() != null) {
            if (meta instanceof LeatherArmorMeta leatherMeta) {
                leatherMeta.setColor(talisman.getLeatherColor());
            } else {
                plugin.getLogger()
                        .warning("保护符 " + talisman.getId() + " 配置了 color, 但材质 " + talisman.getMaterial()
                                + " 不是皮革装备, 颜色不会生效");
            }
        }
        if (talisman.getDurability() != null) {
            if (!(meta instanceof Damageable damageable)) {
                plugin.getLogger()
                        .warning("保护符 " + talisman.getId() + " 配置了 durability, 但材质 " + talisman.getMaterial()
                                + " 没有耐久条");
            } else {
                int maxDamage = damageable.hasMaxDamage()
                        ? damageable.getMaxDamage()
                        : talisman.getMaterial().getMaxDurability();
                if (maxDamage <= 0) {
                    plugin.getLogger()
                            .warning("保护符 " + talisman.getId() + " 配置了 durability, 但材质 " + talisman.getMaterial()
                                    + " 没有耐久条");
                } else {
                    Gem.DurabilityEntry durability = talisman.getDurability();
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
        for (AttributeEntry attribute : talisman.getAttributes()) {
            NamespacedKey modifierKey = new NamespacedKey(plugin, "talisman_attribute_" + attributeIndex++);
            AttributeModifier modifier = attribute.slot() == null
                    ? new AttributeModifier(modifierKey, attribute.amount(), attribute.operation())
                    : new AttributeModifier(
                            modifierKey,
                            attribute.amount(),
                            attribute.operation(),
                            attribute.slot().getGroup());
            meta.addAttributeModifier(attribute.attribute(), modifier);
        }
        if (talisman.getCustomModelData() != null) {
            meta.setCustomModelData(talisman.getCustomModelData());
        }
        if (!talisman.getPotionEffects().isEmpty()) {
            if (meta instanceof PotionMeta potionMeta) {
                for (Gem.PotionEntry potion : talisman.getPotionEffects()) {
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
                        .warning("保护符 " + talisman.getId() + " 配置了 potion, 但材质 " + talisman.getMaterial()
                                + " 不是药水类物品, 效果不会生效");
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== 物品识别 ====================

    public boolean isTalisman(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return item.getPersistentDataContainer().has(talismanIdKey(), PersistentDataType.STRING);
    }

    public String getTalismanId(ItemStack item) {
        if (!isTalisman(item)) {
            return null;
        }
        return item.getPersistentDataContainer().get(talismanIdKey(), PersistentDataType.STRING);
    }

    public Talisman getTalisman(String id) {
        return talismans.get(id);
    }

    public List<Talisman> getAllTalismans() {
        return List.copyOf(talismans.values());
    }

    public ItemStack createTalismanItem(String id) {
        Talisman talisman = talismans.get(id);
        if (talisman == null || talisman.getItemMetaTemplate() == null) {
            return null;
        }
        ItemStack item = new ItemStack(talisman.getMaterial());
        item.setItemMeta(talisman.getItemMetaTemplate().clone());
        return item;
    }

    public boolean isAllowDifferentEffects() {
        return allowDifferentEffects;
    }

    public boolean isAllowSameRestack() {
        return allowSameRestack;
    }

    // ==================== 应用到宝石 ====================

    /**
     * 向宝石写入保护符效果
     */
    public ApplyStatus applyToGem(ItemStack gemItem, String talismanId) {
        Talisman talisman = talismans.get(talismanId);
        if (talisman == null) {
            return ApplyStatus.UNKNOWN_TALISMAN;
        }
        if (gemItem == null || !plugin.getGemManager().isGem(gemItem)) {
            return ApplyStatus.NOT_A_GEM;
        }
        TalismanFunction function = talisman.getFunction();
        if (!function.hasAnyEffect()) {
            return ApplyStatus.NO_EFFECT;
        }

        TalismanEffect.State state = effect.read(gemItem);
        String id = talisman.getId();
        int uses = Math.max(1, talisman.getMaxUses());

        TalismanEffect.BonusEntry newBonus = function.successRateBonus() > 0
                ? new TalismanEffect.BonusEntry(id, function.successRateBonus(), uses)
                : null;
        TalismanEffect.PreventEntry newPrevent =
                function.preventDestroy() ? new TalismanEffect.PreventEntry(id, uses) : null;

        boolean sameIdBonus = state.bonus() != null && id.equals(state.bonus().id());
        boolean sameIdPrevent =
                state.prevent() != null && id.equals(state.prevent().id());
        if (sameIdBonus || sameIdPrevent) {
            if (!allowSameRestack) {
                return ApplyStatus.DUPLICATE;
            }
            TalismanEffect.BonusEntry bonus = sameIdBonus ? newBonus : state.bonus();
            TalismanEffect.PreventEntry prevent = sameIdPrevent ? newPrevent : state.prevent();
            effect.write(gemItem, new TalismanEffect.State(bonus, prevent));
            return ApplyStatus.REFRESHED;
        }

        if (!allowDifferentEffects) {
            // 只允许一个效果: 新保护符覆盖宝石上已有的全部效果
            boolean replaced = !state.isEmpty();
            effect.write(gemItem, new TalismanEffect.State(newBonus, newPrevent));
            return replaced ? ApplyStatus.REPLACED : ApplyStatus.SUCCESS;
        }

        // 不同效果可共存: 同类效果被覆盖, 其他功能保留
        boolean replaced =
                (newBonus != null && state.bonus() != null) || (newPrevent != null && state.prevent() != null);
        TalismanEffect.BonusEntry bonus = newBonus != null ? newBonus : state.bonus();
        TalismanEffect.PreventEntry prevent = newPrevent != null ? newPrevent : state.prevent();
        effect.write(gemItem, new TalismanEffect.State(bonus, prevent));
        return replaced ? ApplyStatus.REPLACED : ApplyStatus.SUCCESS;
    }

    // ==================== 效果读写(供镶嵌流程使用) ====================

    public TalismanEffect.State readEffects(ItemStack gemItem) {
        return effect.read(gemItem);
    }

    public void writeEffects(ItemStack gemItem, TalismanEffect.State state) {
        effect.write(gemItem, state);
    }

    public TalismanEffect getEffect() {
        return effect;
    }

    // ==================== GUI ====================

    public org.bukkit.inventory.Inventory createApplyGUI() {
        return guiFactory.createApplyGUI();
    }

    /**
     * 重建应用 GUI 的装饰物品(语言文件重载后调用).
     */
    public void rebuildGuiItems() {
        guiFactory.rebuildItems();
    }

    // ==================== 解析工具 ====================

    private NamespacedKey talismanIdKey() {
        return new NamespacedKey(plugin, TALISMAN_ID_KEY);
    }

    private Enchantment parseEnchantment(String name) {
        NamespacedKey key = parseNamespacedKey(name);
        return key == null ? null : Registry.ENCHANTMENT.get(key);
    }

    private Attribute parseAttribute(String name) {
        NamespacedKey key = parseNamespacedKey(name);
        if (key == null) {
            return null;
        }
        String value = key.getKey().toLowerCase();
        if (value.startsWith("generic_")) {
            value = value.substring("generic_".length());
        }
        if ("horse_jump_strength".equals(value)) {
            value = "jump_strength";
        }
        if ("zombie_spawn_reinforcements".equals(value)) {
            value = "spawn_reinforcements";
        }
        return Registry.ATTRIBUTE.get(NamespacedKey.minecraft(value));
    }

    private PotionEffectType parsePotionEffect(String name) {
        NamespacedKey key = parseNamespacedKey(name);
        return key == null ? null : Registry.EFFECT.get(key);
    }

    private NamespacedKey parseNamespacedKey(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            String value = name.trim().toLowerCase();
            if (value.contains(":")) {
                return NamespacedKey.fromString(value);
            }
            return NamespacedKey.minecraft(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private AttributeModifier.Operation parseOperation(String name) {
        if (name != null) {
            switch (name.trim()) {
                case "0" -> {
                    return AttributeModifier.Operation.ADD_NUMBER;
                }
                case "1" -> {
                    return AttributeModifier.Operation.ADD_SCALAR;
                }
                case "2" -> {
                    return AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                }
                default -> {}
            }
        }
        try {
            return AttributeModifier.Operation.valueOf(
                    name == null ? "ADD_NUMBER" : name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AttributeModifier.Operation.ADD_NUMBER;
        }
    }

    private EquipmentSlot parseEquipmentSlot(String name) {
        if (name == null || name.isBlank() || "null".equalsIgnoreCase(name)) {
            return null;
        }
        String normalized = name.trim().toUpperCase();
        if ("MAINHAND".equals(normalized)) {
            return EquipmentSlot.HAND;
        }
        if ("OFFHAND".equals(normalized)) {
            return EquipmentSlot.OFF_HAND;
        }
        try {
            return EquipmentSlot.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Color parseHexColor(String hex) {
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        if (value.length() != 6) {
            return null;
        }
        try {
            return Color.fromRGB(Integer.parseInt(value, 16));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Gem.DurabilityEntry parseDurability(String id, String raw) {
        String value = raw.trim();
        try {
            if (value.endsWith("%")) {
                double percent = Double.parseDouble(value.substring(0, value.length() - 1));
                return new Gem.DurabilityEntry(Gem.DurabilityMode.PERCENT, percent);
            }
            if (value.startsWith("<")) {
                double remaining = Double.parseDouble(value.substring(1));
                return new Gem.DurabilityEntry(Gem.DurabilityMode.REMAINING, remaining);
            }
            double damage = Double.parseDouble(value);
            return new Gem.DurabilityEntry(Gem.DurabilityMode.DAMAGE, damage);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("保护符 " + id + " 的 durability 格式错误: " + raw);
            return null;
        }
    }
}
