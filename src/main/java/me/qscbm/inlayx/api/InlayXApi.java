package me.qscbm.inlayx.api;

import me.qscbm.inlayx.command.sub.SubCommand;
import me.qscbm.inlayx.gem.GemManager;
import org.jspecify.annotations.NonNull;

/**
 * API 入口.
 * <p>
 * 通过 Bukkit 的服务注册中心获取它.
 * 或者通过 InlayX.getApi()
 */
public interface InlayXApi {

    /**
     * 获取宝石管理器.
     */
    @NonNull GemManager getGemManager();

    /**
     * 注册子命令.
     */
    boolean registerSubCommand(@NonNull SubCommand subCommand);
}
