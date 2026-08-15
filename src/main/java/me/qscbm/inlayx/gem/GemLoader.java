package me.qscbm.inlayx.gem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.Getter;
import me.qscbm.inlayx.InlayX;
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
import org.bukkit.potion.PotionEffectType;

/**
 * 宝石加载器
 */
class GemLoader {
    private final InlayX plugin;
    private final GemItemFactory itemFactory;
    private final Runnable onGemsChanged;

    private boolean loading;

    @Getter
    private final Map<String, Gem> gems;

    GemLoader(InlayX plugin, Map<String, Gem> gems, GemItemFactory itemFactory, Runnable onGemsChanged) {
        this.plugin = plugin;
        this.gems = gems;
        this.itemFactory = itemFactory;
        this.onGemsChanged = onGemsChanged;
    }

    void loadAll() {
        loading = true;
        try {
            gems.clear();
            int fromConfig = loadFromConfigSection();
            if (fromConfig > 0) {
                plugin.getLogger().info("从 config.yml 加载了 " + fromConfig + " 个宝石");
            }
            int fromDir = loadFromDirectory(gemsDir());
            if (fromDir > 0) {
                plugin.getLogger().info("从 gems/ 目录加载了 " + fromDir + " 个宝石");
            }
            plugin.getLogger().info("共加载 " + gems.size() + " 个宝石");
        } finally {
            loading = false;
        }
    }

    // -- config.yml --

    private int loadFromConfigSection() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gems");
        if (section == null) {
            return 0;
        }
        int count = 0;
        for (String id : section.getKeys(false)) {
            ConfigurationSection gemSec = section.getConfigurationSection(id);
            if (gemSec != null && parseAndRegister(id, gemSec)) {
                count++;
            }
        }
        return count;
    }

    // -- gems/ 目录 --

    private int loadFromDirectory(File dir) {
        if (!dir.exists()) {
            extractBundled(dir);
        }
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        return scanDir(dir);
    }

    private int scanDir(File dir) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        Arrays.sort(files, Comparator.comparing(File::getAbsolutePath));
        for (File f : files) {
            if (f.isDirectory()) {
                count += scanDir(f);
            } else if (f.getName().endsWith(".yml") || f.getName().endsWith(".yaml")) {
                count += loadFromFile(f);
            }
        }
        return count;
    }

    private int loadFromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = 0;
        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section != null && parseAndRegister(id, section)) {
                count++;
            }
        }
        if (count > 0) {
            plugin.getLogger().info("从 " + file.getName() + " 加载了 " + count + " 个宝石");
        }
        return count;
    }

    // -- JAR 提取 --

    private void extractBundled(File targetDir) {
        File jar = pluginJar();
        if (jar == null) {
            return;
        }
        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            int n = 0;
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String name = e.getName();
                if (!name.startsWith("gems/")
                        || e.isDirectory()
                        || !(name.endsWith(".yml") || name.endsWith(".yaml"))) {
                    continue;
                }
                File target = new File(targetDir.getParentFile(), name);
                if (target.exists()) {
                    continue;
                }
                target.getParentFile().mkdirs();
                try (InputStream in = plugin.getResource(name)) {
                    if (in != null) {
                        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        n++;
                    }
                }
            }
            if (n > 0) {
                plugin.getLogger().info("已从插件包提取 " + n + " 个宝石文件到 gems/ 目录");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("提取宝石文件失败: " + e.getMessage());
        }
    }

    private File pluginJar() {
        try {
            return new File(plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (Exception e) {
            return null;
        }
    }

    private File gemsDir() {
        return new File(plugin.getDataFolder(), "gems");
    }

    // -- 解析 --

    boolean parseAndRegister(String gemId, ConfigurationSection section) {
        if (gems.containsKey(gemId)) {
            plugin.getLogger().severe("检测到重复的宝石ID: " + gemId + ", 本次定义已被忽略, 请检查 gems/ 目录与 config.yml 的 gems 配置");
            return false;
        }
        try {
            String name = section.getString("name", "未命名宝石");
            int level = section.getInt("level", 1);
            GemType type = parseType(section.getString("type", "ATTACK"));
            String materialName = section.getString("material", "EMERALD");
            Material material = Material.getMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("宝石 " + gemId + " 使用了无效的材质: " + materialName + ", 已回退为 EMERALD");
                material = Material.EMERALD;
            }

            Gem gem = new Gem(gemId, name, type, level, material);

            List<String> attrs = section.getStringList("attributes");
            for (String line : attrs) {
                gem.addAttributeLore(line);
            }

            ConfigurationSection socketSec = section.getConfigurationSection("socket");
            if (socketSec != null) {
                gem.setSocketSuccessRate(socketSec.getDouble("success_rate", 1));
                gem.setDestroyOnFailure(socketSec.getBoolean("destroy_on_failure", false));
                ConfigurationSection filterSec = socketSec.getConfigurationSection("equipment_materials");
                if (filterSec != null) {
                    Gem.MaterialFilterMode mode = Gem.MaterialFilterMode.NONE;
                    String modeName = filterSec.getString("mode", "NONE");
                    if (modeName != null) {
                        try {
                            mode = Gem.MaterialFilterMode.valueOf(modeName.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger()
                                    .warning("宝石 " + gemId + " 的 socket.equipment_materials.mode 无效: " + modeName
                                            + ", 已按 NONE 处理");
                        }
                    }
                    Set<Material> filterMaterials = new HashSet<>();
                    for (String materialEntry : filterSec.getStringList("list")) {
                        Material filtered = Material.getMaterial(materialEntry);
                        if (filtered == null) {
                            plugin.getLogger()
                                    .warning("宝石 " + gemId + " 的 socket.equipment_materials.list 含有无效材质: "
                                            + materialEntry + ", 已忽略该项");
                        } else {
                            filterMaterials.add(filtered);
                        }
                    }
                    gem.setMaterialFilterMode(mode);
                    gem.setFilterMaterials(filterMaterials);
                }
            }

            ConfigurationSection dropSec = section.getConfigurationSection("drop");
            if (dropSec != null) {
                gem.setDropChance(dropSec.getDouble("chance", 0));
                gem.setLevelBonus(dropSec.getDouble("per_level_rate", 0));
                List<String> sources = dropSec.getStringList("sources");
                if (!sources.isEmpty()) {
                    gem.setDropSources(new HashSet<>(sources));
                }
                gem.setMinMobLevel(dropSec.getInt("min_mob_level", 1));
            }

            gem.setDisplayName(parseVariables(plugin.getConfigManager().getGemDisplayNamePattern(), gem));
            gem.setLore(parseVariables(
                    plugin.getConfigManager().getGemLorePattern(),
                    gem,
                    plugin.getConfigManager().getAttributeLorePattern()));

            ConfigurationSection overrideSec = section.getConfigurationSection("overrides");
            if (overrideSec != null) {
                loadOverride(gem, overrideSec);
            }
            if (!itemFactory.initializeItemMetaTemplate(gem)) {
                return false;
            }
            gems.put(gemId, gem);
            if (!loading) {
                onGemsChanged.run();
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("解析宝石 " + gemId + " 时出错: " + e.getMessage());
            return false;
        }
    }

    private void loadOverride(Gem gem, ConfigurationSection overrideSec) {
        ConfigurationSection displayPatternSec = overrideSec.getConfigurationSection("display_pattern");
        if (displayPatternSec != null) {
            String displayNamePattern = displayPatternSec.getString("display_name");
            if (displayNamePattern != null) {
                gem.setDisplayName(parseVariables(displayNamePattern, gem));
            }
            Set<String> keys = displayPatternSec.getKeys(false);
            List<String> lorePatterns = displayPatternSec.getStringList("lore");
            String attrLorePattern = displayPatternSec.getString("per_line_attribute_lore");
            if (attrLorePattern != null) {
                // 用 keys 判断 lore 项是否存在
                if (keys.contains("lore")) {
                    gem.setLore(parseVariables(lorePatterns, gem, attrLorePattern));
                } else {
                    gem.setLore(parseVariables(plugin.getConfigManager().getGemLorePattern(), gem, attrLorePattern));
                }
            } else {
                if (keys.contains("lore")) {
                    gem.setLore(parseVariables(
                            lorePatterns, gem, plugin.getConfigManager().getAttributeLorePattern()));
                }
            }
        }
        ConfigurationSection itemSec = overrideSec.getConfigurationSection("item");
        if (itemSec != null) {
            loadItemOverride(gem, itemSec);
        }
    }

    private void loadItemOverride(Gem gem, ConfigurationSection itemSec) {
        String durability = itemSec.getString("Durability");
        if (durability != null) {
            Gem.DurabilityEntry durabilityEntry = parseDurability(gem, durability);
            if (durabilityEntry != null) {
                gem.setDurability(durabilityEntry);
            }
        }

        for (String entry : itemSec.getStringList("EnchantList")) {
            int colon = entry.lastIndexOf(':');
            if (colon <= 0 || colon == entry.length() - 1) {
                plugin.getLogger().warning("宝石 " + gem.getId() + " 的 EnchantList 格式错误: " + entry + ", 应使用 附魔名:等级");
                continue;
            }
            String enchantName = entry.substring(0, colon);
            int level;
            try {
                level = Integer.parseInt(entry.substring(colon + 1));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("宝石 " + gem.getId() + " 的 EnchantList 等级无效: " + entry);
                continue;
            }
            Enchantment enchantment = parseEnchantment(enchantName);
            if (enchantment == null) {
                plugin.getLogger().warning("宝石 " + gem.getId() + " 的 EnchantList 含有无效附魔: " + enchantName);
                continue;
            }
            gem.addEnchantment(enchantment, level);
        }

        for (String flagName : itemSec.getStringList("ItemFlagList")) {
            try {
                gem.addItemFlag(ItemFlag.valueOf(flagName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("宝石 " + gem.getId() + " 的 ItemFlagList 含有无效选项: " + flagName);
            }
        }

        String colorHex = itemSec.getString("Color");
        if (colorHex != null) {
            Color color = parseHexColor(colorHex);
            if (color == null) {
                plugin.getLogger().warning("宝石 " + gem.getId() + " 的 Color 无效: " + colorHex + ", 应使用 RRGGBB 或 #RRGGBB");
            } else {
                gem.setLeatherColor(color);
            }
        }

        for (String entry : itemSec.getStringList("Attributes")) {
            String[] parts = entry.split(":");
            if (parts.length < 3 || parts.length > 4) {
                plugin.getLogger()
                        .warning("宝石 " + gem.getId() + " 的 Attributes 格式错误: " + entry + ", 应使用 属性名:数字:模式[:生效位置]");
                continue;
            }
            Attribute attribute = parseAttribute(parts[0]);
            if (attribute == null) {
                plugin.getLogger().warning("宝石 " + gem.getId() + " 的 Attributes 含有无效属性: " + parts[0]);
                continue;
            }
            double amount;
            try {
                amount = Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("宝石 " + gem.getId() + " 的 Attributes 数值无效: " + parts[1]);
                continue;
            }
            AttributeModifier.Operation operation = parseOperation(parts[2]);
            EquipmentSlot slot = parts.length == 4 ? parseEquipmentSlot(parts[3]) : null;
            gem.addAttribute(attribute, amount, operation, slot);
        }

        if (itemSec.contains("CustomModelData")) {
            int customModelData = itemSec.getInt("CustomModelData");
            gem.setCustomModelData(customModelData);
        }

        ConfigurationSection potionSec = itemSec.getConfigurationSection("Potion");
        if (potionSec != null) {
            for (String effectName : potionSec.getKeys(false)) {
                ConfigurationSection effectSec = potionSec.getConfigurationSection(effectName);
                if (effectSec == null) {
                    continue;
                }
                PotionEffectType effect = parsePotionEffect(effectName);
                if (effect == null) {
                    plugin.getLogger().warning("宝石 " + gem.getId() + " 的 Potion 含有无效效果: " + effectName);
                    continue;
                }
                int duration = parseInt(effectSec.get("duration"), 200);
                int amplifier = parseInt(effectSec.get("amplifier"), 0);
                boolean ambient = parseBoolean(effectSec.get("ambient"), true);
                boolean particles = parseBoolean(effectSec.get("particles"), true);
                boolean icon = parseBoolean(effectSec.get("icon"), true);
                gem.addPotionEffect(effect, duration, amplifier, ambient, particles, icon);
            }
        } else {
            String potionType = itemSec.getString("Potion");
            if (potionType != null) {
                PotionEffectType effect = parsePotionEffect(potionType);
                if (effect == null) {
                    plugin.getLogger().warning("宝石 " + gem.getId() + " 的 Potion 含有无效效果: " + potionType);
                } else {
                    gem.addPotionEffect(effect, 200, 0, true, true, true);
                }
            }
        }
    }

    private Gem.DurabilityEntry parseDurability(Gem gem, String raw) {
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
            plugin.getLogger().warning("宝石 " + gem.getId() + " 的 Durability 格式错误: " + raw);
            return null;
        }
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

    private int parseInt(Object value, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean parseBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private GemType parseType(String s) {
        GemType type = plugin.getConfigManager().getGemType(s);
        if (type == null) {
            type = plugin.getConfigManager().getGemType(s.toUpperCase());
        }
        if (type == null) {
            throw new RuntimeException("无效的宝石类型: " + s);
        }
        return type;
    }

    public String parseVariables(String text, Gem gem) {
        return GemTemplate.parse(text, gem);
    }

    public List<String> parseVariables(List<String> text, Gem gem, String attrLorePattern) {
        List<String> result = new ArrayList<>(text.size());
        for (String line : text) {
            if ("{attributeLores}".equals(line)) {
                for (String attrLore : gem.getAttributeLore()) {
                    String parsedAttrLore = parseVariables(attrLorePattern, gem).replace("{attributeLore}", attrLore);
                    result.add(TextUtils.translateAlternateColorCodes(parsedAttrLore));
                }

                continue;
            }
            result.add(parseVariables(line, gem));
        }
        return result;
    }
}
