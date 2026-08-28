package me.qscbm.inlayx.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

/**
 * 配置更新
 */
public final class ConfigUpdater {
    private ConfigUpdater() {}

    /**
     * 检查配置版本, 落后于 JAR 默认版本时合并缺失键并更新版本号.
     */
    public static void update(Plugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                plugin.getLogger().warning("更新文件时失败, 没有对应的文件: " + file.getCanonicalPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        CommentConfiguration existing = new CommentConfiguration();
        try {
            existing.load(file);
        } catch (Exception e) {
            plugin.getLogger().warning("加载现有配置文件 " + fileName + " 失败: " + e.getMessage());
            return;
        }
        int currentVersion = existing.getInt("config-version", 0);

        CommentConfiguration defaults = new CommentConfiguration();
        try (InputStream jarStream = plugin.getResource(fileName)) {
            if (jarStream == null) return;
            try (InputStreamReader reader = new InputStreamReader(jarStream, StandardCharsets.UTF_8)) {
                defaults.load(reader);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("读取默认配置文件 " + fileName + " 失败: " + e.getMessage());
            return;
        }

        int expectedVersion = defaults.getInt("config-version", 0);
        if (currentVersion >= expectedVersion) return;
        mergeSection(defaults, existing, "");

        existing.set("config-version", expectedVersion);
        try {
            existing.save(file);
            plugin.getLogger().info("更新配置文件 " + fileName + ": v" + currentVersion + " 至 v" + expectedVersion);
        } catch (Exception e) {
            plugin.getLogger().warning("保存更新后的 " + fileName + " 失败: " + e.getMessage());
        }
    }

    private static void mergeSection(CommentConfiguration defaults, CommentConfiguration target, String path) {
        ConfigurationSection defaultSection = path.isEmpty() ? defaults : defaults.getConfigurationSection(path);
        if (defaultSection == null) return;

        Set<String> keys = defaultSection.getKeys(false);
        for (String key : keys) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (CommentConfiguration.isCommentByKeys(key)) continue;
            if (defaultSection.isConfigurationSection(key)) {
                if (!target.isConfigurationSection(fullPath)) {
                    copyComment(defaults, target, fullPath);
                    target.createSection(fullPath);
                }
                mergeSection(defaults, target, fullPath);
            } else {
                if (!target.contains(fullPath)) {
                    copyComment(defaults, target, fullPath);
                    target.set(fullPath, defaults.get(fullPath));
                }
            }
        }
    }

    private static void copyComment(CommentConfiguration defaults, CommentConfiguration target, String fullPath) {
        String comment = defaults.getComment(fullPath);
        if (comment != null) {
            target.insertComment(fullPath, comment);
        }
    }
}
