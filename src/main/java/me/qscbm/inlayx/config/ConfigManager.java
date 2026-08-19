package me.qscbm.inlayx.config;

import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.SoundEventKeys;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.GemType;
import me.qscbm.inlayx.util.ReflectionUtils;
import me.qscbm.inlayx.util.TextUtils;
import org.bukkit.ChatColor;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * 配置管理
 */
@Getter
public class ConfigManager {
    private final InlayX plugin;

    private String language;
    private String guiTitle;
    private String socketHeader;
    private String socketFooter;
    private int maxSockets;
    private boolean rightClickSocketEnabled;
    private boolean dragSocketEnabled;
    private String socketEmptyPattern;
    private List<String> socketFilledPattern;
    private String socketAttributeLorePattern;

    @Getter(AccessLevel.NONE)
    private final Map<String, GemType> gemTypes = new LinkedHashMap<>();

    private SoundConfig socketSuccessSound;
    private SoundConfig socketFailureSound;
    private SoundConfig extractSuccessSound;
    private SoundConfig extractFailureSound;

    private String gemDisplayNamePattern;
    private List<String> gemLorePattern;
    private String attributeLorePattern;

    private List<String> noneFilterPattern;
    private List<String> whiteListFilterPattern;
    private List<String> blackListFilterPattern;
    private String perLineEquipmentDisplayLore;

    private boolean dropSystemEnabled;
    private DropSourceMode dropSourceMode;
    private double extractSuccessRate;
    private boolean dropGemOnFullInventory;

    public ConfigManager(InlayX plugin) {
        this.plugin = plugin;
        loadSettings();
    }

    public void loadSettings() {
        var cfg = plugin.getConfig();
        language = normalizeLanguage(cfg.getString("settings.language", "zh_cn"));
        guiTitle = TextUtils.translateAlternateColorCodes(cfg.getString("settings.gui_title", "&5宝石镶嵌"));
        socketHeader = TextUtils.translateAlternateColorCodes(
                cfg.getString("settings.socket.header", "&7------- 宝石槽位 -------"));
        socketFooter = TextUtils.translateAlternateColorCodes(
                cfg.getString("settings.socket.footer", "&7======================="));
        int configuredMaxSockets = cfg.getInt("settings.socket.max_sockets", 8);
        if (configuredMaxSockets < 0) {
            plugin.getLogger().warning("settings.socket.max_sockets 不能为负数, 已回退为默认值 8");
            maxSockets = 8;
        } else {
            maxSockets = configuredMaxSockets;
        }
        rightClickSocketEnabled = cfg.getBoolean("settings.socket.quick_socket.right_click", true);
        dragSocketEnabled = cfg.getBoolean("settings.socket.quick_socket.drag", true);
        socketEmptyPattern =
                cfg.getString("settings.socket.display_pattern.empty", "{gemTypeColor}◇ {gemTypeName}宝石槽位");
        socketFilledPattern = cfg.getStringList("settings.socket.display_pattern.filled");
        if (socketFilledPattern.isEmpty()) {
            socketFilledPattern = List.of("{gemTypeColor}◆ {gemDisplayName}", "{attributeLores}");
        }
        socketAttributeLorePattern = cfg.getString(
                "settings.socket.display_pattern.per_line_attribute_lore", "{gemTypeColor}  {attributeLore}");

        gemTypes.clear();
        ConfigurationSection typesSec = cfg.getConfigurationSection("settings.gem_types");
        if (typesSec != null) {
            for (String id : typesSec.getKeys(false)) {
                ConfigurationSection sec = typesSec.getConfigurationSection(id);
                if (sec == null) {
                    continue;
                }
                String name = sec.getString("name", id);
                ChatColor color = parseColor(sec.getString("color"), ChatColor.WHITE);
                gemTypes.put(id, new GemType(id, name, color));
            }
        }

        socketSuccessSound = SoundConfig.load(
                cfg.getConfigurationSection("settings.sounds.socket.success"),
                "ENTITY_PLAYER_LEVELUP",
                Sound.ENTITY_PLAYER_LEVELUP);
        socketFailureSound = SoundConfig.load(
                cfg.getConfigurationSection("settings.sounds.socket.failure"),
                "BLOCK_ANVIL_BREAK",
                Sound.BLOCK_ANVIL_BREAK);
        extractSuccessSound = SoundConfig.load(
                cfg.getConfigurationSection("settings.sounds.extract.success"),
                "ENTITY_PLAYER_LEVELUP",
                Sound.ENTITY_PLAYER_LEVELUP);
        extractFailureSound = SoundConfig.load(
                cfg.getConfigurationSection("settings.sounds.extract.failure"),
                "BLOCK_ANVIL_BREAK",
                Sound.BLOCK_ANVIL_BREAK);

        gemDisplayNamePattern =
                cfg.getString("settings.gem.display_pattern.display_name", "{gemTypeColor}{gemName} {gemLevelStars}");
        gemLorePattern = cfg.getStringList("settings.gem.display_pattern.lore");
        attributeLorePattern =
                cfg.getString("settings.gem.display_pattern.per_line_attribute_lore", "{gemTypeColor}{attributeLore}");
        noneFilterPattern = cfg.getStringList("settings.gem.display_pattern.equipment_filter_lore.pattern.none");
        whiteListFilterPattern =
                cfg.getStringList("settings.gem.display_pattern.equipment_filter_lore.pattern.white_list");
        blackListFilterPattern =
                cfg.getStringList("settings.gem.display_pattern.equipment_filter_lore.pattern.black_list");
        perLineEquipmentDisplayLore =
                cfg.getString("settings.gem.display_pattern.equipment_filter_lore.per_line_equipment_display_lore");

        dropSystemEnabled = cfg.getBoolean("settings.gem.drop.enable", false);
        dropSourceMode = parseDropSourceMode(cfg.getString("settings.gem.drop.mode", "FIRST"));
        extractSuccessRate = Math.clamp(cfg.getDouble("settings.gem.extract.success_rate", 1.0), 0.0, 1.0);
        dropGemOnFullInventory = "drop".equalsIgnoreCase(cfg.getString("settings.gem.give.full_inventory", "drop"));
    }

    //  ==================== Getter ====================

    public GemType getGemType(String id) {
        if (id == null) {
            return null;
        }
        return gemTypes.get(id);
    }

    public Map<String, GemType> getGemTypes() {
        return Collections.unmodifiableMap(gemTypes);
    }

    public GemType getDefaultGemType() {
        return gemTypes.isEmpty() ? null : gemTypes.values().iterator().next();
    }

    private DropSourceMode parseDropSourceMode(String name) {
        try {
            return DropSourceMode.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DropSourceMode.FIRST;
        }
    }

    private static String normalizeLanguage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "zh_cn";
        }
        String trimmed = raw.trim().toLowerCase();
        String normalized = trimmed.replace('-', '_');
        if (normalized.contains("_")) {
            String[] parts = normalized.split("_", 2);
            String lang = parts[0].replaceAll("[^a-z]", "");
            String region = parts[1].replaceAll("[^a-z0-9]", "");
            if (lang.isEmpty() || region.isEmpty()) {
                return "zh_cn";
            }
            return lang + "_" + region;
        }
        return switch (normalized) {
            case "zh" -> "zh_cn";
            case "en" -> "en_us";
            default -> normalized.matches("[a-z]{2,3}") ? normalized : "zh_cn";
        };
    }

    private static ChatColor parseColor(String name, ChatColor def) {
        if (name == null) return def;
        try {
            return ChatColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private static TypedKey<?> getSoundKey(String fieldName) {
        Object value = ReflectionUtils.getFieldValue(SoundEventKeys.class, fieldName.toUpperCase());
        if (value instanceof TypedKey<?> key) {
            return key;
        }
        return null;
    }

    private static Sound loadSound(String name, Sound fallback) {
        TypedKey<?> key = getSoundKey(name);
        if (key == null) {
            return fallback;
        }
        Sound sound = Registry.SOUNDS.get(key);
        return sound != null ? sound : fallback;
    }

    /**
     * 多个掉落来源之间的关系.
     */
    public enum DropSourceMode {
        /**
         * 第一次掉落后停止.
         */
        FIRST,
        /**
         * 掉落后继续遍历剩余来源.
         */
        ALL
    }

    /**
     * 单组声音配置(开关, 声音, 音量, 音调).
     */
    @Getter
    public static final class SoundConfig {
        private final boolean enable;
        private final Sound sound;
        private final float volume;
        private final float pitch;

        private SoundConfig(boolean enable, Sound sound, float volume, float pitch) {
            this.enable = enable;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public void play(Player player) {
            if (enable && sound != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }

        static SoundConfig load(ConfigurationSection section, String defaultSoundName, Sound fallback) {
            if (section == null) {
                return new SoundConfig(true, fallback, 1.0f, 1.0f);
            }
            return new SoundConfig(
                    section.getBoolean("enable", true),
                    loadSound(section.getString("sound", defaultSoundName), fallback),
                    (float) section.getDouble("volume", 1.0d),
                    (float) section.getDouble("pitch", 1.0d));
        }
    }
}
