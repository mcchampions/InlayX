package me.qscbm.inlayx.gem;

import lombok.Getter;
import org.bukkit.ChatColor;

/**
 * 宝石类型
 */
@Getter
public final class GemType {
    private final String id;
    private final String name;
    private final ChatColor color;

    public GemType(String id, String name, ChatColor color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }
}
