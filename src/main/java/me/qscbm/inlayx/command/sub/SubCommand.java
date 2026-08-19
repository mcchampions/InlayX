package me.qscbm.inlayx.command.sub;

import java.util.Collections;
import java.util.List;
import me.qscbm.inlayx.InlayX;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 子命令基类
 */
public abstract class SubCommand {
    protected final InlayX plugin;

    protected SubCommand(InlayX plugin) {
        this.plugin = plugin;
    }

    public abstract String name();

    public abstract String description();

    protected abstract void execute(CommandSender sender, String[] args);

    public String permission() {
        return null;
    }

    protected boolean playerOnly() {
        return false;
    }

    protected int minArgs() {
        return 0;
    }

    protected String usage() {
        return "/gem " + name();
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    public boolean canExecute(CommandSender sender, String[] args) {
        if (permission() != null && noneOf(sender, permission())) {
            sender.sendMessage(i18n("common.no_permission"));
            return false;
        }
        if (playerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(i18n("common.player_only"));
            return false;
        }
        if (args.length < minArgs()) {
            sender.sendMessage(i18n("common.usage", usage()));
            return false;
        }
        return true;
    }

    public void tryExecute(CommandSender sender, String[] args) {
        if (canExecute(sender, args)) {
            execute(sender, args);
        }
    }

    protected Player asPlayer(CommandSender sender) {
        return (Player) sender;
    }

    public static boolean noneOf(CommandSender sender, String perm) {
        if (perm == null) return false;
        return !sender.hasPermission("inlayx.admin") && !sender.hasPermission(perm);
    }

    public static boolean hasAny(CommandSender sender, String perm) {
        return sender.hasPermission("inlayx.admin") || perm == null || sender.hasPermission(perm);
    }

    /**
     * 获取当前语言的本地化消息(无占位符).
     */
    protected String i18n(String key) {
        return plugin.getLanguageService().get(key);
    }

    /**
     * 获取当前语言的本地化消息并替换占位符.
     */
    protected String i18n(String key, Object... args) {
        return plugin.getLanguageService().get(key, args);
    }

    protected static int argInt(String[] args, int idx, int min, int max) {
        if (idx >= args.length) return min;
        try {
            int v = Integer.parseInt(args[idx]);
            return (v >= min && v <= max) ? v : min;
        } catch (NumberFormatException e) {
            return min;
        }
    }
}
