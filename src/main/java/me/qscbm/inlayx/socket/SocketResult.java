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

    private SocketResult(Status status, ItemStack item) {
        this.status = status;
        this.item = item;
    }

    public static SocketResult success(ItemStack item) {
        return new SocketResult(Status.SUCCESS, item);
    }

    public static SocketResult failure(Status status) {
        return new SocketResult(status, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
