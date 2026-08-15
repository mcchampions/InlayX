package me.qscbm.inlayx.command.sub;

import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import org.bukkit.ChatColor;
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
        return "列出所有宝石";
    }

    @Override
    public String permission() {
        return "inlayx.list";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "===== 可用宝石列表 =====");
        for (Gem gem : plugin.getGemManager().getAllGems()) {
            sender.sendMessage(ChatColor.YELLOW + gem.getId()
                    + ChatColor.WHITE + " - " + ChatColor.GREEN + gem.getName()
                    + ChatColor.WHITE + " (等级: " + gem.getLevel() + ")");
        }
    }
}
