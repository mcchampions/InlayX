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
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 宝石加载器
 */
class GemLoader {
    private final InlayX plugin;

    @Getter
    private final Map<String, Gem> gems;

    GemLoader(InlayX plugin, Map<String, Gem> gems) {
        this.plugin = plugin;
        this.gems = gems;
    }

    void loadAll() {
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
            if (gemSec != null) {
                parseAndRegister(id, gemSec);
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
            if (section != null) {
                parseAndRegister(id, section);
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

    void parseAndRegister(String gemId, ConfigurationSection section) {
        try {
            String name = section.getString("name", "未命名宝石");
            int level = section.getInt("level", 1);
            GemType type = parseType(section.getString("type", "ATTACK"));
            Material material = Material.getMaterial(section.getString("material", "EMERALD"));
            if (material == null) {
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
            gems.put(gemId, gem);
        } catch (Exception e) {
            plugin.getLogger().warning("解析宝石 " + gemId + " 时出错: " + e.getMessage());
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
