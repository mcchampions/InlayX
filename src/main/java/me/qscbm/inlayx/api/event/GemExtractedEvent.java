package me.qscbm.inlayx.api.event;

import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 在提取流程结束之后触发.
 */
public final class GemExtractedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable Player actor;
    private final @NonNull ItemStack equipment;
    private final @NonNull String gemId;
    /**
     *  宝石是否成功返还, false 表示宝石已碎裂.
     */
    @Getter
    private final boolean success;

    public GemExtractedEvent(
            @Nullable Player actor, @NonNull ItemStack equipment, @NonNull String gemId, boolean success) {
        this.actor = actor;
        this.equipment = equipment;
        this.gemId = gemId;
        this.success = success;
    }

    /**
     * 发起提取的玩家.
     */
    public @Nullable Player getActor() {
        return actor;
    }

    /**
     * 提取完成后的装备物品.
     */
    public @NonNull ItemStack getEquipment() {
        return equipment;
    }

    /**
     * 本次被提取的宝石 ID.
     */
    public @NonNull String getGemId() {
        return gemId;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NonNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
