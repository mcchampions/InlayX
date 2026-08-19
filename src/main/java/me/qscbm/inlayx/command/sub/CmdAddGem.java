package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.stream.Collectors;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.gem.Gem;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.socket.SocketResult;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 管理员命令: 镶嵌指定宝石
 */
public class CmdAddGem extends SubCommand {
    public CmdAddGem(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "addgem";
    }

    @Override
    public String description() {
        return i18n("command.addgem.description");
    }

    @Override
    protected String usage() {
        return i18n("command.addgem.usage");
    }

    @Override
    public String permission() {
        return "inlayx.addgem";
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

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(i18n("command.addgem.must_hold_equipment"));
            return;
        }
        Gem gem = gm.getGem(gemId);
        if (gem == null) {
            player.sendMessage(i18n("command.give.gem_not_found", gemId));
            return;
        }

        SocketResult result = gm.addGem(player, item, gemId);
        if (result.isSuccess()) {
            player.getInventory().setItemInMainHand(result.getItem());
            player.sendMessage(i18n("command.addgem.success", gem.getName()));
            plugin.getInteractionFeedback().playSocketSound(player, true);
            return;
        }
        plugin.getInteractionFeedback().sendSocketFailure(player, result, gem, gm.hasSocketLore(item));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getGemManager().getAllGems().stream().map(Gem::getId).collect(Collectors.toList());
        }
        return List.of();
    }
}
