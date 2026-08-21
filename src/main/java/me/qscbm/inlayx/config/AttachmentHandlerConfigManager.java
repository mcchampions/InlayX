package me.qscbm.inlayx.config;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.json.JSONObject;

public class AttachmentHandlerConfigManager {
    private final InlayX plugin;
    private YamlConfiguration config;

    @Getter
    private EnchantmentConfig enchantmentConfig;

    private final NamespacedKey enchantmentKey;

    public AttachmentHandlerConfigManager(@NonNull InlayX plugin) {
        this.plugin = plugin;
        this.config = new YamlConfiguration();
        enchantmentKey = new NamespacedKey(plugin, "enchantments");
    }

    /**
     * 加载或生成 attachment_handler.yml, 并通知所有来源解析默认配置.
     */
    public synchronized void load() {
        File file = new File(plugin.getDataFolder(), "attachment_handler.yml");
        if (!file.exists()) {
            copyBundledDefault(file);
        }
        config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            plugin.getLogger().warning("加载 attachment_handler.yml 失败: " + e.getMessage());
            config = new YamlConfiguration();
        }
        ConfigurationSection enchantmentConfigSec = config.getConfigurationSection("enchantments");
        if (enchantmentConfigSec != null) {
            enchantmentConfig = new EnchantmentConfig(enchantmentConfigSec);
        } else {
            plugin.getLogger().severe("加载 attachment_handler.yml 失败: 无法获取 enchantments 字段");
            enchantmentConfig = new EnchantmentConfig(new MemoryConfiguration());
        }
    }

    private void copyBundledDefault(File file) {
        try (InputStream in = plugin.getResource("attachment_handler.yml")) {
            if (in == null) {
                return;
            }
            file.getParentFile().mkdirs();
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            plugin.getLogger().warning("写入默认 attachment_handler.yml 失败: " + e.getMessage());
        }
    }

    public ItemMeta applyEnchantmentConfigWhenSocket(
            @NonNull ItemMeta meta, @NonNull Map<Enchantment, Integer> enchantments, @NonNull Gem gem) {
        if (enchantments.isEmpty()) {
            return meta;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String data = pdc.get(enchantmentKey, PersistentDataType.STRING);
        JSONObject json;
        if (data != null) {
            json = new JSONObject(data);
        } else {
            json = new JSONObject();
        }
        JSONObject subJson = new JSONObject();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            int old_level = meta.getEnchantLevel(enchantment);
            int final_level = parseExprWhenSocket(level, old_level);
            subJson.put(enchantment.getKey().getKey(), old_level);
            if (final_level == old_level) {
                continue;
            }
            if (!meta.addEnchant(enchantment, final_level, enchantmentConfig.ignoreLevelRestriction)) {
                InlayX.INSTANCE
                        .getLogger()
                        .warning("无法添加附魔:" + enchantment.getKey().getKey());
            }
        }
        JSONObject jsonObject = json.optJSONObject(gem.getId());
        if (jsonObject == null) {
            jsonObject = new JSONObject();
            jsonObject.put("1", subJson);
        } else {
            Set<String> keys = jsonObject.keySet();
            String idx = String.valueOf(keys.size() + 1);
            jsonObject.put(idx, subJson);
        }
        json.put(gem.getId(), jsonObject);
        pdc.set(enchantmentKey, PersistentDataType.STRING, json.toString());
        return meta;
    }

    public ItemMeta applyEnchantmentConfigWhenExtract(
            @NonNull ItemMeta meta, @NonNull Map<Enchantment, Integer> enchantments, @NonNull Gem gem) {
        if (enchantments.isEmpty()) {
            return meta;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String data = pdc.get(enchantmentKey, PersistentDataType.STRING);
        JSONObject json;
        if (data != null) {
            json = new JSONObject(data);
        } else {
            InlayX.INSTANCE.getLogger().warning("无法获取旧附魔数据, 可能导致逻辑问题");
            json = new JSONObject();
        }
        JSONObject jsonObject = json.optJSONObject(gem.getId());
        if (jsonObject == null) {
            InlayX.INSTANCE.getLogger().warning("无法获取旧附魔数据, 可能导致逻辑问题");
            jsonObject = new JSONObject();
        }
        Set<String> keys = jsonObject.keySet();
        String idx = String.valueOf(keys.size());
        JSONObject subJson = jsonObject.optJSONObject(idx);
        if (subJson == null) {
            subJson = new JSONObject();
        }
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            int old_level = subJson.optInt(enchantment.getKey().getKey());
            int now_level = meta.getEnchantLevel(enchantment);
            int final_level = parseExprWhenExtract(level, old_level, now_level);
            if (final_level == 0) {
                if (!meta.removeEnchant(enchantment)) {
                    InlayX.INSTANCE
                            .getLogger()
                            .warning("无法移除附魔:" + enchantment.getKey().getKey());
                }
                continue;
            }
            if (final_level == now_level) {
                continue;
            }
            if (!meta.addEnchant(enchantment, final_level, enchantmentConfig.ignoreLevelRestriction)) {
                InlayX.INSTANCE
                        .getLogger()
                        .warning("无法设置附魔:" + enchantment.getKey().getKey());
            }
        }

        jsonObject.remove(idx);
        if (jsonObject.isEmpty()) {
            json.remove(gem.getId());
        }
        pdc.set(enchantmentKey, PersistentDataType.STRING, json.toString());
        return meta;
    }

    private int parseExprWhenSocket(int level, int old_level) {
        Expression expr;
        if (old_level == 0) {
            expr = enchantmentConfig.getSocketLevelIfNonExist();
        } else if (level > old_level) {
            expr = enchantmentConfig.getSocketLevelIfHigher();
        } else if (level < old_level) {
            expr = enchantmentConfig.getSocketLevelIfLower();
        } else {
            expr = enchantmentConfig.getSocketLevelIfSame();
        }
        expr.setVariable("_var_level", level);
        expr.setVariable("_var_old_level", old_level);
        int result = Math.toIntExact(Math.round(expr.evaluate()));
        if (result < 0) {
            result = 0;
        }
        return result;
    }

    private int parseExprWhenExtract(int level, int old_level, int now_level) {
        Expression expr;
        if (level < old_level) {
            if (level < now_level) {
                expr = enchantmentConfig.getExtractLevelIfBeforeLowerAfterLower();
            } else if (level > now_level) {
                expr = enchantmentConfig.getExtractLevelIfBeforeLowerAfterHigher();
            } else {
                expr = enchantmentConfig.getExtractLevelIfBeforeLowerAfterSame();
            }
        } else if (level > old_level) {
            if (level < now_level) {
                expr = enchantmentConfig.getExtractLevelIfBeforeHigherAfterLower();
            } else if (level > now_level) {
                expr = enchantmentConfig.getExtractLevelIfBeforeHigherAfterHigher();
            } else {
                expr = enchantmentConfig.getExtractLevelIfBeforeHigherAfterSame();
            }
        } else {
            if (level < now_level) {
                expr = enchantmentConfig.getExtractLevelIfBeforeSameAfterLower();
            } else if (level > now_level) {
                expr = enchantmentConfig.getExtractLevelIfBeforeSameAfterHigher();
            } else {
                expr = enchantmentConfig.getExtractLevelIfBeforeSameAfterSame();
            }
        }
        expr.setVariable("_var_level", level);
        expr.setVariable("_var_old_level", old_level);
        expr.setVariable("_var_now_level", now_level);
        int result = Math.toIntExact(Math.round(expr.evaluate()));
        if (result < 0) {
            result = 0;
        }
        return result;
    }

    @Getter
    public static class EnchantmentConfig {
        private final boolean ignoreLevelRestriction;

        private final Expression socketLevelIfNonExist;

        private final Expression socketLevelIfLower;

        private final Expression socketLevelIfHigher;

        private final Expression socketLevelIfSame;

        private final Expression extractLevelIfBeforeLowerAfterLower;

        private final Expression extractLevelIfBeforeLowerAfterHigher;

        private final Expression extractLevelIfBeforeLowerAfterSame;

        private final Expression extractLevelIfBeforeHigherAfterLower;

        private final Expression extractLevelIfBeforeHigherAfterHigher;

        private final Expression extractLevelIfBeforeHigherAfterSame;

        private final Expression extractLevelIfBeforeSameAfterLower;

        private final Expression extractLevelIfBeforeSameAfterHigher;

        private final Expression extractLevelIfBeforeSameAfterSame;

        public EnchantmentConfig(@NonNull ConfigurationSection config) {
            this.ignoreLevelRestriction = config.getBoolean("ignore_level_restriction", false);

            this.socketLevelIfNonExist = parseExpr(config.getString("handler.socket.non_exist", "{level}"));

            this.socketLevelIfLower = parseExpr(config.getString("handler.socket.exist.lower", "{old_level}"));

            this.socketLevelIfHigher = parseExpr(config.getString("handler.socket.exist.higher", "{level}"));

            this.socketLevelIfSame = parseExpr(config.getString("handler.socket.exist.same", "{level}" + 1));

            this.extractLevelIfBeforeLowerAfterLower =
                    parseExpr(config.getString("handler.extract.before_lower.after_lower", "{now_level}"));

            this.extractLevelIfBeforeLowerAfterHigher =
                    parseExpr(config.getString("handler.extract.before_lower.after_higher", "{now_level}"));

            this.extractLevelIfBeforeLowerAfterSame =
                    parseExpr(config.getString("handler.extract.before_lower.after_same", "{now_level}"));
            this.extractLevelIfBeforeHigherAfterLower =
                    parseExpr(config.getString("handler.extract.before_higher.after_lower", "{now_level} - 1"));
            this.extractLevelIfBeforeHigherAfterHigher = parseExpr(config.getString(
                    "handler.extract.before_higher.after_higher", "{now_level} - {level} + {old_level}"));
            this.extractLevelIfBeforeHigherAfterSame =
                    parseExpr(config.getString("handler.extract.before_higher.after_same", "{old_level}"));
            this.extractLevelIfBeforeSameAfterLower =
                    parseExpr(config.getString("handler.extract.before_same.after_lower", "{now_level} - 1"));
            this.extractLevelIfBeforeSameAfterHigher =
                    parseExpr(config.getString("handler.extract.before_same.after_higher", "{now_level} - 1"));
            this.extractLevelIfBeforeSameAfterSame =
                    parseExpr(config.getString("handler.extract.before_same.after_same", "{now_level} - 1"));
        }
    }

    private static Expression parseExpr(String expr) {
        boolean hasLevel = expr.contains("{level}");
        boolean hasOldLevel = expr.contains("{old_level}");
        boolean hasNowLevel = expr.contains("{now_level}");
        if (hasLevel) {
            expr = expr.replace("{level}", "_var_level");
        }
        if (hasOldLevel) {
            expr = expr.replace("{old_level}", "_var_old_level");
        }
        if (hasNowLevel) {
            expr = expr.replace("{now_level}", "_var_now_level");
        }
        ExpressionBuilder builder = new ExpressionBuilder(expr);
        if (hasLevel) {
            builder.variable("_var_level");
        }
        if (hasOldLevel) {
            builder.variable("_var_old_level");
        }
        if (hasNowLevel) {
            builder.variable("_var_now_level");
        }
        return builder.build();
    }
}
