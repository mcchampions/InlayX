package me.qscbm.inlayx.command.sub;

import java.util.List;
import me.qscbm.inlayx.InlayX;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * 显示帮助信息
 */
public class CmdHelp extends SubCommand {
    private final List<SubCommand> all;

    public CmdHelp(InlayX plugin, List<SubCommand> all) {
        super(plugin);
        this.all = all;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String permission() {
        return "inlayx.help";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "===== InlayX 帮助 =====");
        for (SubCommand cmd : all) {
            if (cmd.permission() != null && noneOf(sender, cmd.permission())) continue;
            sender.sendMessage(ChatColor.YELLOW + cmd.usage() + " " + ChatColor.WHITE + "- " + descriptionOf(cmd));
        }
    }

    private static String descriptionOf(SubCommand cmd) {
        return switch (cmd.name()) {
            case "help" -> "显示帮助信息";
            case "list" -> "列出所有宝石";
            case "give" -> "给予宝石";
            case "addgem" -> "直接向装备添加指定宝石";
            case "socket" -> "打开宝石镶嵌界面";
            case "extract" -> "从手持装备提取指定宝石";
            case "removegem" -> "直接移除装备上的指定宝石";
            case "info" -> "查看手持宝石或装备信息";
            case "addslot" -> "为手持装备添加空槽位";
            case "removeslot" -> "为手持装备移除空槽位";
            case "reload" -> "重载插件配置";
            default -> "";
        };
    }
}
