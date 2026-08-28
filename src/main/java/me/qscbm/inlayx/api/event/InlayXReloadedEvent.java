package me.qscbm.inlayx.api.event;

import lombok.NonNull;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 插件重载后触发
 */
public class InlayXReloadedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public InlayXReloadedEvent() {}

    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NonNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}

