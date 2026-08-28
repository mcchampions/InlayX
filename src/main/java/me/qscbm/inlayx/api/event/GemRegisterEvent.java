package me.qscbm.inlayx.api.event;

import lombok.NonNull;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 在宝石即将通过 {@link me.qscbm.inlayx.gem.GemManager#registerGem(Gem)} 注册时触发.
 * <p>
 * 配置文件加载的宝石不触发此事件. 取消此事件将阻止本次注册.
 */
public final class GemRegisterEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final @NonNull Gem gem;
    private boolean cancelled;

    public GemRegisterEvent(@NonNull Gem gem) {
        this.gem = gem;
    }

    /**
     * 即将注册的宝石定义.
     */
    public @NonNull Gem getGem() {
        return gem;
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
