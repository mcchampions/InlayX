package me.qscbm.inlayx.command.sub;

import java.util.Collections;
import java.util.List;
import me.qscbm.inlayx.InlayX;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
            return false;
        }
        if (playerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行");
            return false;
        }
        if (args.length < minArgs()) {
            sender.sendMessage(ChatColor.RED + "用法: " + usage());
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
        return !sender.hasPermission("inlayx.admin") && !sender.hasPermission(perm);
    }

    public static boolean hasAny(CommandSender sender, String perm) {
        return sender.hasPermission("inlayx.admin") || sender.hasPermission(perm);
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
