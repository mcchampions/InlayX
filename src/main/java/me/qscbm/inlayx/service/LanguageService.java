package me.qscbm.inlayx.service;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.util.MCAssetsUtils;
import me.qscbm.inlayx.util.TextUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.json.JSONObject;

/**
 * 语言服务.
 * <p>
 * 管理 插件语言文本 与 原版语言文本
 */
public class LanguageService {
    public static final String DEFAULT_LANGUAGE = "zh_cn";

    private static final String LANGUAGE_DIR = "languages";
    private static final String RESOURCE_PREFIX = LANGUAGE_DIR + "/";

    private final InlayX plugin;

    @Getter
    private String language;

    private YamlConfiguration messages;

    private YamlConfiguration defaultMessages;

    private String effectiveLanguage;

    private Map<String, String> vanillaTranslations;

    public LanguageService(InlayX plugin) {
        this.plugin = plugin;
        this.defaultMessages = loadBundledMessages(DEFAULT_LANGUAGE);
        this.language = plugin.getConfigManager().getLanguage();

        this.messages = loadMessagesFromDisk(this.language);
        loadVanillaTranslations();
    }

    // ==================== 插件消息 ====================

    public String get(String key, Object... args) {
        String template = lookup(key);
        if (template == null) {
            return key;
        }
        String translated = TextUtils.translateAlternateColorCodes(template);
        if (args == null || args.length == 0) {
            return translated;
        }
        for (int i = 0; i < args.length; i++) {
            translated = translated.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return translated;
    }

    private String lookup(String key) {
        if (messages != null) {
            String value = messages.getString(key);
            if (value != null) {
                return value;
            }
        }
        if (defaultMessages != null) {
            return defaultMessages.getString(key);
        }
        return null;
    }

    // ==================== 原版物品名 ====================

    public String getMaterialI18nName(Material material) {
        return vanillaTranslations.getOrDefault(material.getItemTranslationKey(), material.name());
    }

    private void loadVanillaTranslations() {
        String version = plugin.getServer().getMinecraftVersion();
        String cacheDir =
                plugin.getDataPath().resolve("cache").resolve("assets").toString();

        JSONObject data = MCAssetsUtils.getLanguage(version, language, cacheDir);
        String loadedLang = language;
        if (data == null && !DEFAULT_LANGUAGE.equals(language)) {
            plugin.getLogger().warning("无法获取原版语言文件: " + language + ", 回退到 " + DEFAULT_LANGUAGE);
            data = MCAssetsUtils.getLanguage(version, DEFAULT_LANGUAGE, cacheDir);
            loadedLang = DEFAULT_LANGUAGE;
        }
        if (data == null) {
            plugin.getLogger().warning("无法获取原版语言文件(含回退语言), 原版物品名将使用材质名");
            effectiveLanguage = language;
            return;
        }

        vanillaTranslations = getTranslationMap(data);
        effectiveLanguage = loadedLang;
    }

    private Map<String, String> getTranslationMap(JSONObject jsonObject) {
        Set<String> keys = jsonObject.keySet();
        Map<String, String> translationMap = new HashMap<>(jsonObject.length());
        for (String key : keys) {
            String value = jsonObject.getString(key);
            translationMap.put(key, value);
        }
        return translationMap;
    }

    // ==================== 重载 ====================

    public void reload() {
        this.defaultMessages = loadBundledMessages(DEFAULT_LANGUAGE);
        this.language = plugin.getConfigManager().getLanguage();

        this.messages = loadMessagesFromDisk(this.language);
        loadVanillaTranslations();
    }

    // ==================== 语言文件加载 ====================

    private YamlConfiguration loadMessagesFromDisk(String lang) {
        File file = languageFile(lang);
        if (!file.exists()) {
            extractBundled(lang, file);
        }
        if (!file.exists()) {
            // 磁盘与 JAR 均无该语言文件, 直接回退到内置默认语言
            plugin.getLogger().warning("语言文件 " + lang + ".yml 不存在, 回退到 " + DEFAULT_LANGUAGE);
            return loadBundledMessages(DEFAULT_LANGUAGE);
        }
        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            plugin.getLogger().warning("加载语言文件 " + file.getName() + " 失败: " + e.getMessage());
            return loadBundledMessages(DEFAULT_LANGUAGE);
        }
    }

    private YamlConfiguration loadBundledMessages(String lang) {
        try (InputStream in = plugin.getResource(RESOURCE_PREFIX + lang + ".yml")) {
            if (in == null) {
                if (!DEFAULT_LANGUAGE.equals(lang)) {
                    return loadBundledMessages(DEFAULT_LANGUAGE);
                }
                return new YamlConfiguration();
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("读取内置语言文件 " + lang + ".yml 失败: " + e.getMessage());
            return new YamlConfiguration();
        }
    }

    private void extractBundled(String lang, File target) {
        try (InputStream in = plugin.getResource(RESOURCE_PREFIX + lang + ".yml")) {
            if (in == null) {
                return;
            }
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            java.nio.file.Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("已生成默认语言文件: " + target.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("生成默认语言文件 " + lang + ".yml 失败: " + e.getMessage());
        }
    }

    private File languageFile(String lang) {
        return new File(new File(plugin.getDataFolder(), LANGUAGE_DIR), lang + ".yml");
    }
}
