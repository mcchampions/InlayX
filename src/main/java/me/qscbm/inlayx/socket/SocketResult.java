package me.qscbm.inlayx.socket;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

/**
 * 镶嵌操作结果
 */
@Getter
public final class SocketResult {
    public enum Status {
        /** 镶嵌成功 */
        SUCCESS,
        /** 装备或宝石为空 */
        INVALID_INPUT,
        /** 物品不是宝石 */
        NOT_A_GEM,
        /** 宝石 ID 无法识别(配置被删除或变更) */
        UNKNOWN_GEM,
        /** 装备没有空槽位(无槽位或已满) */
        NO_SOCKET,
        /** 存在空槽位, 但没有与宝石类型匹配的槽位 */
        TYPE_MISMATCH,
        /** 宝石配置的允许材质列表不包含当前装备 */
        MATERIAL_MISMATCH,
        /** 装备槽位总数超过配置上限, 视为异常装备 */
        OVER_CAP_LIMIT,
        /** 镶嵌成功率判定失败 */
        FAILED,
        /** 被插件事件监听器取消 */
        CANCELLED
    }

    private final Status status;
    private final ItemStack item;

    /**
     * 镶嵌失败时是否被保护符的防碎裂效果保护(宝石未碎裂).
     */
    private final boolean talismanProtected;

    /**
     * 防碎裂效果触发后剩余的次数(0 表示效果已耗尽移除).
     */
    private final int talismanPreventUsesRemaining;

    private SocketResult(Status status, ItemStack item, boolean talismanProtected, int talismanPreventUsesRemaining) {
        this.status = status;
        this.item = item;
        this.talismanProtected = talismanProtected;
        this.talismanPreventUsesRemaining = talismanPreventUsesRemaining;
    }

    public static SocketResult success(ItemStack item) {
        return new SocketResult(Status.SUCCESS, item, false, 0);
    }

    public static SocketResult failure(Status status) {
        return new SocketResult(status, null, false, 0);
    }

    /**
     * 创建失败结果, 并标记防碎裂保护触发情况.
     *
     * @param preventUsesRemaining 触发后剩余的防碎裂次数
     */
    public static SocketResult failure(Status status, int preventUsesRemaining) {
        return new SocketResult(status, null, true, preventUsesRemaining);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
