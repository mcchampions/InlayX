package me.qscbm.inlayx.command.sub;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.qscbm.inlayx.InlayX;
import me.qscbm.inlayx.command.GemTabCompleter;
import me.qscbm.inlayx.gem.GemManager;
import me.qscbm.inlayx.gem.GemType;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 管理员命令: 添加指定槽位
 */
public class CmdAddSlot extends SubCommand {
    public CmdAddSlot(InlayX plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "addslot";
    }

    @Override
    public String description() {
        return i18n("command.addslot.description");
    }

    @Override
    protected String usage() {
        return i18n("command.addslot.usage");
    }

    @Override
    public String permission() {
        return "inlayx.addslot";
    }

    @Override
    protected boolean playerOnly() {
        return true;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        ItemStack item = player.getInventory().getItemInMainHand();
        GemManager gm = plugin.getGemManager();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(i18n("command.addslot.must_hold_equipment"));
            return;
        }
        GemType type = args.length > 0
                ? plugin.getConfigManager().getGemType(args[0])
                : plugin.getConfigManager().getDefaultGemType();
        if (type == null) {
            if (args.length > 0) {
                player.sendMessage(i18n("command.addslot.invalid_type", args[0]));
            } else {
                player.sendMessage(i18n("command.addslot.no_type_configured"));
            }
            return;
        }
        int requested = argInt(args, 1, 1, plugin.getConfigManager().getMaxSockets());
        int maxSockets = plugin.getConfigManager().getMaxSockets();
        int current = gm.getSocketCount(item);
        int added = Math.max(0, Math.min(requested, maxSockets - current));
        if (added == 0) {
            player.sendMessage(i18n("command.addslot.reached_max", maxSockets));
            return;
        }
        player.getInventory().setItemInMainHand(gm.addSlotToItem(item, added, type));
        if (added < requested) {
            player.sendMessage(i18n("command.addslot.added_partial", added, type.name(), maxSockets));
        } else {
            player.sendMessage(i18n("command.addslot.added", added, type.name()));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return GemTabCompleter.filter(
                    plugin.getConfigManager().getGemTypes().keySet().stream().toList(), args[0]);
        }
        if (args.length == 2) {
            return IntStream.rangeClosed(1, plugin.getConfigManager().getMaxSockets())
                    .mapToObj(String::valueOf)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
