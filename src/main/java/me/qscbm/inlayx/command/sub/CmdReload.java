package me.qscbm.inlayx.command.sub;

import me.qscbm.inlayx.InlayX;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * 重载插件
 */
public class CmdReload extends SubCommand {
    public CmdReload(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String description() {
        return "重载插件配置";
    }

    @Override
    public String permission() {
        return "inlayx.reload";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        plugin.getConfigManager().loadSettings();
        plugin.getDropSourceConfigManager().load();
        plugin.getGemManager().loadGems();
        sender.sendMessage(ChatColor.GREEN + "InlayX 插件配置已重载");
    }
}
