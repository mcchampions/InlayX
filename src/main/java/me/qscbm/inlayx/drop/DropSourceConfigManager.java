package me.qscbm.inlayx.drop;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.config.CommentConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NonNull;

/**
 * drop_source.yml 管理.
 * <p>
 * 负责生成文件, 加载默认配置, 注册新来源时自动补写缺失的 key.
 */
public final class DropSourceConfigManager {
    private final InlayX plugin;
    private final DropSourceRegistry registry;
    private CommentConfiguration config;

    public DropSourceConfigManager(@NonNull InlayX plugin, @NonNull DropSourceRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.config = new CommentConfiguration();
    }

    /**
     * 加载或生成 drop_source.yml, 并通知所有来源解析默认配置.
     */
    public synchronized void load() {
        File file = new File(plugin.getDataFolder(), "drop_source.yml");
        if (!file.exists() && !copyBundledDefault(file)) {
            generateFromRegisteredSources(file);
        }
        config = new CommentConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            plugin.getLogger().warning("加载 drop_source.yml 失败: " + e.getMessage());
            config = new CommentConfiguration();
        }
        ensureRegisteredKeys(file);
        notifySources();
    }

    private boolean copyBundledDefault(File file) {
        try (InputStream in = plugin.getResource("drop_source.yml")) {
            if (in == null) {
                return false;
            }
            file.getParentFile().mkdirs();
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("写入默认 drop_source.yml 失败: " + e.getMessage());
            return false;
        }
    }

    private void generateFromRegisteredSources(File file) {
        config = new CommentConfiguration();
        for (DropSourceRegistry.RegisteredSource entry : registry.getEntries()) {
            config.createSection(entry.source().id(), entry.defaultSettings());
        }
        save(file);
    }

    /**
     * 新来源注册后补写默认 key 并通知该来源.
     */
    public synchronized void onSourceRegistered(@NonNull DropSource source) {
        File file = new File(plugin.getDataFolder(), "drop_source.yml");
        ensureRegisteredKeys(file);
        notifySource(source);
    }

    /**
     * 获取一个来源的默认配置.
     * <p>
     * 返回的是文件配置与 defaultSettings 合并后的独立快照.
     */
    public synchronized @NonNull ConfigurationSection getSourceDefaults(@NonNull String sourceId) {
        YamlConfiguration settings = new YamlConfiguration();
        for (Map.Entry<String, Object> entry :
                registry.getDefaultSettings(sourceId).entrySet()) {
            settings.set(entry.getKey(), copyValue(entry.getValue()));
        }
        ConfigurationSection fileSection = config.getConfigurationSection(sourceId);
        if (fileSection != null) {
            for (Map.Entry<String, Object> entry : fileSection.getValues(true).entrySet()) {
                settings.set(entry.getKey(), copyValue(entry.getValue()));
            }
        }
        settings.set("enable", settings.getBoolean("enable", true));
        if (!settings.contains("priority")) {
            settings.set("priority", 0);
        }
        return settings;
    }

    /**
     * 合并来源默认配置和宝石覆盖配置.
     */
    public synchronized @NonNull ConfigurationSection mergeSettings(
            @NonNull String sourceId, @NonNull Map<String, Object> overrides) {
        ConfigurationSection defaults = getSourceDefaults(sourceId);
        YamlConfiguration merged = new YamlConfiguration();
        for (Map.Entry<String, Object> entry : defaults.getValues(true).entrySet()) {
            merged.set(entry.getKey(), copyValue(entry.getValue()));
        }
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            merged.set(entry.getKey(), copyValue(entry.getValue()));
        }
        merged.set("enable", merged.getBoolean("enable", true));
        if (!merged.contains("priority")) {
            merged.set("priority", 0);
        }
        return merged;
    }

    private void ensureRegisteredKeys(File file) {
        boolean changed = false;
        for (DropSourceRegistry.RegisteredSource entry : registry.getEntries()) {
            if (!config.isConfigurationSection(entry.source().id())) {
                config.createSection(entry.source().id(), entry.defaultSettings());
                changed = true;
            }
        }
        if (changed) {
            save(file);
        }
    }

    private void save(File file) {
        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("保存 drop_source.yml 失败: " + e.getMessage());
        }
    }

    private void notifySources() {
        for (DropSource source : registry.getSources()) {
            notifySource(source);
        }
    }

    private void notifySource(DropSource source) {
        try {
            source.onSettingsLoaded(getSourceDefaults(source.id()));
        } catch (Exception e) {
            plugin.getLogger().warning("掉落来源 " + source.id() + " 加载默认配置时出错: " + e.getMessage());
        }
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(entry.getKey(), copyValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object item : list) {
                copy.add(copyValue(item));
            }
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof Set<?> set) {
            return Set.copyOf(set);
        }
        return value;
    }
}
