package me.qscbm.inlayx.config;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import me.qscbm.inlayx.InlayX;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ItemGroupConfigManager {

    private final InlayX plugin;
    private YamlConfiguration config;

    private Map<String, ItemGroupConfig> itemGroupConfigs;

    private Map<String, ItemGroupOrItem> itemGroups;

    public ItemGroupConfigManager(@NonNull InlayX plugin) {
        this.plugin = plugin;
        this.config = new YamlConfiguration();
    }

    /**
     * 加载或生成 item_group.yml, 并通知所有来源解析默认配置.
     */
    public synchronized void load() {
        File file = new File(plugin.getDataFolder(), "item_group.yml");
        if (!file.exists()) {
            copyBundledDefault(file);
        }
        config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            plugin.getLogger().warning("加载 item_group.yml 失败: " + e.getMessage());
            config = new YamlConfiguration();
        }
        itemGroupConfigs = new HashMap<>();
        ConfigurationSection itemGroupSection = config.getConfigurationSection("item_group");
        Set<String> keys = itemGroupSection.getKeys(false);
        for (String id : keys) {
            ConfigurationSection itemSection = itemGroupSection.getConfigurationSection(id);
            String name = itemSection.getString("name");
            List<String> items = itemSection.getStringList("items");
            itemGroupConfigs.put(id, new ItemGroupConfig(id, name, items));
        }
        loadItemGroups();
    }

    public void loadItemGroups() {
        itemGroups = new ConcurrentHashMap<>();
        for (ItemGroupConfig itemGroupConfig : itemGroupConfigs.values()) {
            try {
                loadItemGroup(itemGroupConfig);
            } catch (IllegalArgumentException e) {
                // 忽略无效的物品组
                plugin.getLogger().warning("加载物品组 " + itemGroupConfig.id() + " 失败: " + e.getMessage());
            }
        }
    }

    public void loadItemGroup(ItemGroupConfig itemGroupConfig) {
        String id = itemGroupConfig.id();
        if (itemGroups.containsKey(id)) {
            return;
        }
        String name = itemGroupConfig.name();
        List<String> items = itemGroupConfig.items();

        List<Material> materials = new ArrayList<>();
        List<ItemGroupOrItem> subGroup = new ArrayList<>();
        for (String item : items) {
            Material material = Material.getMaterial(item);
            if (material != null) {
                materials.add(material);
                continue;
            }
            if (itemGroupConfigs.containsKey(item)) {
                loadItemGroup(itemGroupConfigs.get(item));
                subGroup.add(itemGroups.get(item));
                continue;
            }
            throw new IllegalArgumentException("无效的物品 / 物品组: " + item);
        }
        itemGroups.put(id, ItemGroupOrItem.createItemGroup(id, name, materials, subGroup));
    }

    public ItemGroupOrItem getItemGroup(String id) {
        return itemGroups.get(id);
    }

    public ItemGroupOrItem getItemGroupOrItem(String id) {
        ItemGroupOrItem group = itemGroups.get(id);
        if (group == null) {
            Material material = Material.getMaterial(id);
            if (material == null) {
                throw new IllegalArgumentException("无效的物品 / 物品组: " + id);
            }
            String name = plugin.getLanguageService().getMaterialI18nName(material);
            return ItemGroupOrItem.createItem(id, name, material);
        }
        return group;
    }

    private void copyBundledDefault(File file) {
        try (InputStream in = plugin.getResource("item_group.yml")) {
            if (in == null) {
                return;
            }
            file.getParentFile().mkdirs();
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            plugin.getLogger().warning("写入默认 item_group.yml 失败: " + e.getMessage());
        }
    }

    public Map<String, ItemGroupConfig> getItemGroupConfigs() {
        return Collections.unmodifiableMap(itemGroupConfigs);
    }

    public record ItemGroupConfig(String id, String name, List<String> items) {}
}
