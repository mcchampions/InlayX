package me.qscbm.inlayx.command.sub;

import me.qscbm.inlayx.InlayX;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 打开镶嵌页面
 */
public class CmdSocket extends SubCommand {
    public CmdSocket(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "socket";
    }

    @Override
    public String description() {
        return "打开宝石镶嵌界面";
    }

    @Override
    public String permission() {
        return "inlayx.socket";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        player.openInventory(plugin.getGemManager().createSocketGUI());
    }
}
