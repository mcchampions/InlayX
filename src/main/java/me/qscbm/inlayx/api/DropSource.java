package me.qscbm.inlayx.api;

import java.util.Map;
import lombok.NonNull;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 自定义掉落来源 SPI.
 * <p>
 * 实现此接口并通过 InlayXApi.registerDropSource 注册后, 在宝石配置的 drop 下以 id() 为 key 填写配置即可.
 * 实体死亡时插件会按优先级依次调用来源的 handleEntityDeath.
 */
public interface DropSource {
    /**
     * 原版怪物掉落来源 ID.
     */
    String NORMAL = "normal";

    /**
     * MythicMobs 掉落来源 ID.
     */
    String MYTHIC = "mythic";

    /**
     * 掉落来源 ID, 对应宝石配置 drop 下的 key.
     */
    @NonNull String id();

    /**
     * 来源的默认设置, 用于生成 drop_source.yml 中缺失的 key.
     * <p>
     * 通用项 enable 和 priority 未提供时, 插件会自动补为 true 和 0.
     */
    @NonNull Map<String, Object> defaultSettings();

    /**
     * drop_source.yml 加载或重载后回调, 来源在这里解析自己的默认配置.
     */
    default void onSettingsLoaded(@NonNull ConfigurationSection settings) {}

    /**
     * 处理一次实体死亡掉落.
     * <p>
     * 来源判断应该掉落时, 调用 context.select 选择要掉落的宝石.
     */
    void handleEntityDeath(@NonNull DropSourceContext context);
}
