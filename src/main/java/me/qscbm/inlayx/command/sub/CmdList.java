package me.qscbm.inlayx.command.sub;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.command.CommandSender;

/**
 * 查询宝石列表
 */
public class CmdList extends SubCommand {
    public CmdList(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String description() {
        return i18n("command.list.description");
    }

    @Override
    protected String usage() {
        return i18n("command.list.usage");
    }

    @Override
    public String permission() {
        return "inlayx.list";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        sender.sendMessage(i18n("command.list.title"));
        for (Gem gem : plugin.getGemManager().getAllGems()) {
            sender.sendMessage(i18n("command.list.entry", gem.getId(), gem.getName(), gem.getLevel()));
        }
    }
}
