package me.qscbm.inlayx.api;

import me.qscbm.inlayx.gem.GemManager;
import org.jspecify.annotations.NonNull;

/**
 * InlayX 对第三方插件开放的 API 入口.
 * <p>
 * 通过 Bukkit 的服务注册中心获取它.
 * 或者通过 InlayX.getApi()
 */
public interface InlayXApi {

    /**
     * 拿到负责宝石定义, 槽位和掉落逻辑的管理器.
     *
     * @return 插件当前的宝石管理器, 永远不会为空
     */
    @NonNull GemManager getGemManager();
}
