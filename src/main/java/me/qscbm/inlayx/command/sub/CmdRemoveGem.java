package me.qscbm.inlayx.command.sub;

import java.util.List;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.GemManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 管理员命令: 移除指定宝石
 */
public class CmdRemoveGem extends SubCommand {
    public CmdRemoveGem(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "removegem";
    }

    @Override
    public String description() {
        return i18n("command.removegem.description");
    }

    @Override
    protected String usage() {
        return i18n("command.removegem.usage");
    }

    @Override
    public String permission() {
        return "inlayx.removegem";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected int minArgs() {
        return 1;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        String gemId = args[0];
        ItemStack item = player.getInventory().getItemInMainHand();
        GemManager gm = plugin.getGemManager();

        if (item == null || item.getType() == Material.AIR || !gm.hasSocketedGems(item)) {
            player.sendMessage(i18n("command.extract.not_socketed"));
            return;
        }
        if (!gm.getSocketedGems(item).contains(gemId)) {
            player.sendMessage(i18n("feedback.extract.not_found", gemId));
            return;
        }

        if (!gm.removeGem(item, gemId)) {
            player.sendMessage(i18n("command.removegem.failed"));
            return;
        }
        player.getInventory().setItemInMainHand(item);
        player.sendMessage(i18n("command.removegem.success", gemId));
        plugin.getInteractionFeedback().playSocketSound(player, true);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            return plugin.getGemManager().getSocketedGems(item);
        }
        return List.of();
    }
}
