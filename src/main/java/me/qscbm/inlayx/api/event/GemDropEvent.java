package me.qscbm.inlayx.api.event;

import me.qscbm.inlayx.gem.Gem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 在插件为怪物选定掉落宝石之后, 把宝石加进掉落物之前触发.
 */
public final class GemDropEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NonNull LivingEntity entity;
    private final @NonNull Player killer;
    private final @NonNull String source;
    private final int mobLevel;
    private @Nullable Gem gem;
    private boolean cancelled;

    public GemDropEvent(
            @NonNull LivingEntity entity,
            @NonNull Player killer,
            @NonNull String source,
            int mobLevel,
            @Nullable Gem gem) {
        this.entity = entity;
        this.killer = killer;
        this.source = source;
        this.mobLevel = mobLevel;
        this.gem = gem;
    }

    /**
     * 触发掉落的怪物.
     */
    public @NonNull LivingEntity getEntity() {
        return entity;
    }

    /**
     * 杀死怪物的玩家.
     */
    public @NonNull Player getKiller() {
        return killer;
    }

    /**
     * 掉落来源, 目前是 normal 或 mythic.
     */
    public @NonNull String getSource() {
        return source;
    }

    /**
     * 参与本次掉落判定的怪物等级.
     */
    public int getMobLevel() {
        return mobLevel;
    }

    /**
     * 插件选中的宝石, 监听器可以替换或清空它.
     */
    public @Nullable Gem getGem() {
        return gem;
    }

    public void setGem(@Nullable Gem gem) {
        this.gem = gem;
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
