package me.qscbm.inlayx.interaction;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.socket.SocketResult;
import org.bukkit.ChatColor;
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
        player.sendMessage(ChatColor.GREEN + "宝石镶嵌成功!");
        playSocketSound(player, true);
    }

    /**
     * 把失败的镶嵌结果转成玩家提示并播放失败声音.
     *
     * @return true 表示宝石按配置已碎裂, 调用方需要从对应位置扣除一颗宝石
     */
    public boolean sendSocketFailure(Player player, SocketResult result, @Nullable Gem gem, boolean hadSocketLore) {
        switch (result.getStatus()) {
            case FAILED -> {
                if (gem != null && gem.isDestroyOnFailure()) {
                    player.sendMessage(ChatColor.RED + "镶嵌失败!宝石已碎裂.");
                    playSocketSound(player, false);
                    return true;
                }
                player.sendMessage(ChatColor.RED + "镶嵌失败!宝石完好无损, 可再次尝试.");
                playSocketSound(player, false);
            }
            case NOT_A_GEM -> player.sendMessage(ChatColor.RED + "这不是一个有效的宝石!");
            case UNKNOWN_GEM -> player.sendMessage(ChatColor.RED + "无法识别该宝石, 可能已被删除或配置已变更!");
            case NO_SOCKET -> {
                if (hadSocketLore) {
                    player.sendMessage(ChatColor.RED + "该装备的宝石槽位已满!");
                } else {
                    player.sendMessage(ChatColor.RED + "该装备没有宝石槽位!");
                }
            }
            case TYPE_MISMATCH -> {
                String typeName = gem == null ? "对应" : gem.getType().getName();
                player.sendMessage(ChatColor.RED + "该装备没有「" + typeName + "」类型的空槽位!");
            }
            case MATERIAL_MISMATCH -> player.sendMessage(ChatColor.RED + "该宝石不能镶嵌到这种装备上!");
            case OVER_CAP_LIMIT -> player.sendMessage(ChatColor.RED + "该装备的宝石槽位数量异常, 无法镶嵌!");
            case CANCELLED -> player.sendMessage(ChatColor.RED + "镶嵌已被取消!");
            default -> player.sendMessage(ChatColor.RED + "无法镶嵌, 请检查装备与宝石!");
        }
        return false;
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
