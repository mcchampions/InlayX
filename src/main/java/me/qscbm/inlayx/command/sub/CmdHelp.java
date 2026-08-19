package me.qscbm.inlayx.command.sub;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.command.GemCommand;
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
        return i18n("command.help.description");
    }

    @Override
    protected String usage() {
        return i18n("command.help.usage");
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        sender.sendMessage(i18n("command.help.title"));
        for (SubCommand cmd : GemCommand.COMMANDS.values()) {
            if (cmd.permission() != null && noneOf(sender, cmd.permission())) continue;
            sender.sendMessage(i18n("command.help.line", cmd.usage(), cmd.description()));
        }
    }
}
