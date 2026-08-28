package me.qscbm.inlayx.api.event;

import lombok.NonNull;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 在宝石即将镶嵌到装备上时触发.
 */
public final class GemSocketEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable Player actor;
    private final @NonNull ItemStack equipment;
    private final @NonNull Gem gem;
    private final @Nullable ItemStack gemItem;
    private boolean cancelled;

    public GemSocketEvent(
            @Nullable Player actor, @NonNull ItemStack equipment, @NonNull Gem gem, @Nullable ItemStack gemItem) {
        this.actor = actor;
        this.equipment = equipment;
        this.gem = gem;
        this.gemItem = gemItem;
    }

    /**
     * 发起镶嵌的玩家.
     */
    public @Nullable Player getActor() {
        return actor;
    }

    /**
     * 准备接受宝石的装备.
     */
    public @NonNull ItemStack getEquipment() {
        return equipment;
    }

    /**
     * 准备镶嵌的宝石定义.
     */
    public @NonNull Gem getGem() {
        return gem;
    }

    /**
     * 玩家手中的宝石物品. 管理员命令直接镶嵌时没有这个物品, 返回空.
     */
    public @Nullable ItemStack getGemItem() {
        return gemItem;
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
