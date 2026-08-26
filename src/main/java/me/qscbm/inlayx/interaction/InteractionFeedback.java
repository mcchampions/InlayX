package me.qscbm.inlayx.interaction;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.socket.SocketResult;
import me.qscbm.inlayx.talisman.TalismanManager;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * 集中处理镶嵌与提取交互中重复出现的提示和声音.
 */
public final class InteractionFeedback {

    private final InlayX plugin;

    public InteractionFeedback(InlayX plugin) {
        this.plugin = plugin;
    }

    /**
     * 提示玩家镶嵌成功并播放成功声音.
     */
    public void sendSocketSuccess(Player player) {
        player.sendMessage(plugin.getLanguageService().get("feedback.socket.success"));
        playSocketSound(player, true);
    }

    /**
     * 把失败的镶嵌结果转成玩家提示并播放失败声音.
     *
     * @return true 表示宝石按配置已碎裂
     */
    public boolean sendSocketFailure(Player player, SocketResult result, @Nullable Gem gem, boolean hadSocketLore) {
        switch (result.getStatus()) {
            case FAILED -> {
                if (gem != null && gem.isDestroyOnFailure()) {
                    if (result.isTalismanProtected()) {
                        player.sendMessage(plugin.getLanguageService().get("feedback.socket.failed_protected"));
                        if (result.getTalismanPreventUsesRemaining() > 0) {
                            player.sendMessage(plugin.getLanguageService()
                                    .get(
                                            "talisman.socket.prevent_remaining",
                                            result.getTalismanPreventUsesRemaining()));
                        }
                        playSocketSound(player, false);
                        return false;
                    }
                    player.sendMessage(plugin.getLanguageService().get("feedback.socket.failed_destroyed"));
                    playSocketSound(player, false);
                    return true;
                }
                player.sendMessage(plugin.getLanguageService().get("feedback.socket.failed_intact"));
                playSocketSound(player, false);
            }
            case NOT_A_GEM -> player.sendMessage(plugin.getLanguageService().get("feedback.socket.not_a_gem"));
            case UNKNOWN_GEM -> player.sendMessage(plugin.getLanguageService().get("feedback.socket.unknown_gem"));
            case NO_SOCKET -> {
                if (hadSocketLore) {
                    player.sendMessage(plugin.getLanguageService().get("feedback.socket.no_socket_full"));
                } else {
                    player.sendMessage(plugin.getLanguageService().get("feedback.socket.no_socket_empty"));
                }
            }
            case TYPE_MISMATCH -> {
                if (gem == null) {
                    player.sendMessage(plugin.getLanguageService().get("feedback.socket.type_mismatch_unknown"));
                } else {
                    player.sendMessage(plugin.getLanguageService()
                            .get("feedback.socket.type_mismatch", gem.getType().name()));
                }
            }
            case MATERIAL_MISMATCH ->
                player.sendMessage(plugin.getLanguageService().get("feedback.socket.material_mismatch"));
            case OVER_CAP_LIMIT ->
                player.sendMessage(plugin.getLanguageService().get("feedback.socket.over_cap"));
            case CANCELLED -> player.sendMessage(plugin.getLanguageService().get("feedback.socket.cancelled"));
            default -> player.sendMessage(plugin.getLanguageService().get("feedback.socket.default"));
        }
        return false;
    }

    /**
     * 反馈保护符应用到宝石的结果.
     *
     * @return true 表示保护符已消耗(成功/刷新/覆盖)
     */
    public boolean sendTalismanApplyFeedback(Player player, TalismanManager.ApplyStatus status) {
        boolean consumed = status == TalismanManager.ApplyStatus.SUCCESS
                || status == TalismanManager.ApplyStatus.REFRESHED
                || status == TalismanManager.ApplyStatus.REPLACED;
        String key =
                switch (status) {
                    case SUCCESS -> "talisman.apply.success";
                    case REFRESHED -> "talisman.apply.refreshed";
                    case REPLACED -> "talisman.apply.replaced";
                    case DUPLICATE -> "talisman.apply.duplicate";
                    case NO_EFFECT -> "talisman.apply.no_effect";
                    case NOT_A_GEM -> "talisman.apply.not_a_gem";
                    case UNKNOWN_TALISMAN -> "talisman.apply.unknown_talisman";
                };
        player.sendMessage(plugin.getLanguageService().get(key));
        playSocketSound(player, consumed);
        return consumed;
    }

    /**
     * 播放镶嵌成功或失败对应的声音.
     */
    public void playSocketSound(Player player, boolean success) {
        if (success) {
            plugin.getConfigManager().getSocketSuccessSound().play(player);
        } else {
            plugin.getConfigManager().getSocketFailureSound().play(player);
        }
    }

    /**
     * 播放提取成功或失败对应的声音.
     */
    public void playExtractSound(Player player, boolean success) {
        if (success) {
            plugin.getConfigManager().getExtractSuccessSound().play(player);
        } else {
            plugin.getConfigManager().getExtractFailureSound().play(player);
        }
    }
}
