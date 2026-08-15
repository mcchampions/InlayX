package me.qscbm.inlayx.command.sub;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.command.GemCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * 显示帮助信息
 */
public class CmdHelp extends SubCommand {
    public CmdHelp(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "显示帮助信息";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "===== InlayX 帮助 =====");
        for (SubCommand cmd : GemCommand.COMMANDS.values()) {
            if (cmd.permission() != null && noneOf(sender, cmd.permission())) continue;
            sender.sendMessage(ChatColor.YELLOW + cmd.usage() + " " + ChatColor.WHITE + "- " + cmd.description());
        }
    }
}
