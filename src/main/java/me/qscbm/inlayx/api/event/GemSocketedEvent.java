package me.qscbm.inlayx.api.event;

import lombok.NonNull;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 在宝石成功镶嵌到装备之后触发.
 * <p>
 * 镶嵌失败不会触发这个事件.
 */
public final class GemSocketedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable Player actor;
    private final @NonNull ItemStack resultItem;
    private final @NonNull Gem gem;

    public GemSocketedEvent(@Nullable Player actor, @NonNull ItemStack resultItem, @NonNull Gem gem) {
        this.actor = actor;
        this.resultItem = resultItem;
        this.gem = gem;
    }

    /**
     * 发起镶嵌的玩家.
     */
    public @Nullable Player getActor() {
        return actor;
    }

    /**
     * 镶嵌完成后的装备物品.
     */
    public @NonNull ItemStack getResultItem() {
        return resultItem;
    }

    /**
     * 本次镶嵌进去的宝石定义.
     */
    public @NonNull Gem getGem() {
        return gem;
    }

    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NonNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
