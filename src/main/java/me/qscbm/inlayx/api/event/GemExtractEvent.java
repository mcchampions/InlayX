package me.qscbm.inlayx.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 在装备上的宝石即将被提取时触发.
 *
 * <p>事件触发时宝石仍然镶嵌在装备上, 取消事件即可阻止提取, 装备和宝石都不会变化.
 */
public final class GemExtractEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable Player actor;
    private final @NonNull ItemStack equipment;
    private final @NonNull String gemId;
    private boolean cancelled;

    public GemExtractEvent(@Nullable Player actor, @NonNull ItemStack equipment, @NonNull String gemId) {
        this.actor = actor;
        this.equipment = equipment;
        this.gemId = gemId;
    }

    /**
     * 发起提取的玩家.
     */
    public @Nullable Player getActor() {
        return actor;
    }

    /**
     * 镶嵌着待提取宝石的装备, 此时还未被修改.
     */
    public @NonNull ItemStack getEquipment() {
        return equipment;
    }

    /**
     * 待提取宝石的 ID.
     */
    public @NonNull String getGemId() {
        return gemId;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NonNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
