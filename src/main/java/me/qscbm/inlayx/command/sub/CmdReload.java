package me.qscbm.inlayx.command.sub;

import me.qscbm.inlayx.InlayX;
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
        return i18n("command.reload.description");
    }

    @Override
    public String permission() {
        return "inlayx.reload";
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        plugin.getConfigManager().loadSettings();
        plugin.getLanguageService().reload();
        plugin.getGemManager().getGuiFactory().rebuildItems();
        plugin.getGemManager().getExtractGuiFactory().rebuildItems();
        plugin.getItemGroupConfigManager().load();
        plugin.getAttachmentHandlerConfigManager().load();
        plugin.getDropSourceConfigManager().load();
        plugin.getGemManager().loadGems();
        sender.sendMessage(i18n("command.reload.success"));
    }
}
