package me.qscbm.inlayx.api.event;

import lombok.NonNull;
import me.qscbm.inlayx.api.DropSource;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;

/**
 * 在插件为怪物选定掉落宝石之后, 把宝石加进掉落物之前触发.
 */
public final class GemDropEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NonNull LivingEntity entity;
    private final @NonNull Player killer;
    private final @NonNull DropSource source;
    private final @NonNull ConfigurationSection settings;
    private @Nullable Gem gem;
    private boolean cancelled;

    public GemDropEvent(
            @NonNull LivingEntity entity,
            @NonNull Player killer,
            @NonNull DropSource source,
            @Nullable Gem gem,
            @NonNull ConfigurationSection settings) {
        this.entity = entity;
        this.killer = killer;
        this.source = source;
        this.gem = gem;
        this.settings = settings;
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
     * 本次掉落的注册式来源.
     */
    public @NonNull DropSource getSource() {
        return source;
    }

    /**
     * 掉落来源 ID.
     */
    public @NonNull String getSourceId() {
        return source.id();
    }

    /**
     * 本次掉落实际使用的合并配置.
     */
    public @NonNull ConfigurationSection getSettings() {
        return settings;
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
